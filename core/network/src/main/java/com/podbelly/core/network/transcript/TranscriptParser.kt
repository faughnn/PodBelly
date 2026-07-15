package com.podbelly.core.network.transcript

import com.podbelly.core.network.model.TranscriptCue
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and parses Podcasting 2.0 episode transcripts
 * (`<podcast:transcript>`) into a list of [TranscriptCue]s.
 *
 * Supported formats — the same set Pocket Casts handles, minus HTML:
 * - **podcastindex JSON**: `{"segments":[{"startTime":1.2,"body":"…"}]}` with
 *   startTime in (possibly fractional) seconds; a `"start"` key is tolerated too.
 * - **WebVTT** (`text/vtt`): `00:00:01.000 --> 00:00:04.000` cue blocks.
 * - **SRT / SubRip** (`application/x-subrip`, `application/srt`):
 *   `00:00:01,000 --> 00:00:04,000` blocks preceded by an index line.
 *
 * One cue is produced per timestamp block (multi-line blocks are joined with a
 * space). Malformed input never throws from the parsing layer — it yields an
 * empty list; only network failures propagate as [IOException] from [fetch].
 */
@Singleton
class TranscriptParser @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    /**
     * Downloads the transcript at [url] (bounded to [MAX_TRANSCRIPT_BYTES]) and
     * parses it. [type] is the MIME type the feed declared; when it's missing or
     * unrecognized the content is sniffed.
     *
     * @throws IOException on network errors, HTTP errors, or an oversized body.
     */
    suspend fun fetch(url: String, type: String?): List<TranscriptCue> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json, text/vtt, application/x-subrip, text/plain, */*")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch transcript: HTTP ${response.code} for $url")
            }
            val body = response.body ?: throw IOException("Empty transcript body for $url")
            val content = readCapped(body.byteStream(), MAX_TRANSCRIPT_BYTES, url)
            // Prefer the feed-declared type; fall back to the response Content-Type.
            val effectiveType = type?.takeIf { it.isNotBlank() }
                ?: response.header("Content-Type").orEmpty()
            parse(content, effectiveType)
        }
    }

    /**
     * Parses transcript [content] according to [type], sniffing the format when the
     * type is missing or unknown. Never throws; malformed input yields an empty list.
     */
    fun parse(content: String, type: String?): List<TranscriptCue> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            when (normalizeType(type)) {
                "application/json" -> parseJson(trimmed)
                "text/vtt" -> parseVtt(trimmed)
                "application/x-subrip", "application/srt" -> parseSrt(trimmed)
                else -> sniffAndParse(trimmed)
            }
        } catch (_: Exception) {
            // A malformed transcript must never take playback down with it.
            emptyList()
        }
    }

    private fun normalizeType(type: String?): String {
        return type.orEmpty().substringBefore(';').trim().lowercase(Locale.US)
    }

    private fun sniffAndParse(content: String): List<TranscriptCue> {
        return when {
            content.startsWith("{") || content.startsWith("[") -> parseJson(content)
            content.startsWith("WEBVTT") -> parseVtt(content)
            content.contains("-->") -> parseSrt(content)
            else -> emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // podcastindex JSON
    // -------------------------------------------------------------------------

    /**
     * podcastindex format: `{"segments":[{"startTime":1.2,"body":"…"}, …]}`.
     * `startTime` is in seconds and may be fractional; some producers write
     * `"start"` instead, which is tolerated.
     */
    private fun parseJson(content: String): List<TranscriptCue> {
        val adapter = moshi.adapter(Any::class.java)
        val root = adapter.fromJson(content) as? Map<*, *> ?: return emptyList()
        val segments = root["segments"] as? List<*> ?: return emptyList()

        val cues = mutableListOf<TranscriptCue>()
        for (segment in segments) {
            val map = segment as? Map<*, *> ?: continue
            val startSeconds = (map["startTime"] as? Number)?.toDouble()
                ?: (map["start"] as? Number)?.toDouble()
                ?: continue
            val body = (map["body"] as? String)?.trim().orEmpty()
            if (body.isEmpty() || startSeconds < 0) continue
            cues.add(TranscriptCue(startMs = (startSeconds * 1000).toLong(), text = body))
        }
        return mergeConsecutive(cues)
    }

    // -------------------------------------------------------------------------
    // WebVTT / SRT
    // -------------------------------------------------------------------------

    /** WebVTT: cue blocks of `HH:MM:SS.mmm --> HH:MM:SS.mmm` followed by text lines. */
    private fun parseVtt(content: String): List<TranscriptCue> = parseTimestampBlocks(content)

    /** SRT: like VTT but with `,` as the millisecond separator and index lines. */
    private fun parseSrt(content: String): List<TranscriptCue> = parseTimestampBlocks(content)

    /**
     * Shared block parser for VTT and SRT: whenever a line contains `-->`, its start
     * timestamp opens a cue whose text is every following non-empty line until a
     * blank line. Index lines (SRT), the `WEBVTT` header, and `NOTE`/`STYLE` blocks
     * simply never match a timestamp line, so they fall through harmlessly.
     */
    private fun parseTimestampBlocks(content: String): List<TranscriptCue> {
        val lines = content.lines()
        val cues = mutableListOf<TranscriptCue>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val arrowIndex = line.indexOf("-->")
            if (arrowIndex <= 0) {
                i++
                continue
            }
            val startMs = parseTimestampMs(line.substring(0, arrowIndex).trim())
            if (startMs == null) {
                i++
                continue
            }
            val text = StringBuilder()
            i++
            while (i < lines.size && lines[i].isNotBlank()) {
                if (text.isNotEmpty()) text.append(' ')
                text.append(stripTags(lines[i].trim()))
                i++
            }
            val cueText = text.toString().trim()
            if (cueText.isNotEmpty()) {
                cues.add(TranscriptCue(startMs = startMs, text = cueText))
            }
        }
        return mergeConsecutive(cues)
    }

    /**
     * Parses `HH:MM:SS.mmm`, `MM:SS.mmm`, or the same with `,` before the millis
     * (SRT). Millis are optional. Returns null when the string isn't a timestamp.
     */
    internal fun parseTimestampMs(raw: String): Long? {
        val match = TIMESTAMP_REGEX.matchEntire(raw.trim()) ?: return null
        val (hoursStr, minutesStr, secondsStr, millisStr) = match.destructured
        val hours = if (hoursStr.isEmpty()) 0L else hoursStr.toLongOrNull() ?: return null
        val minutes = minutesStr.toLongOrNull() ?: return null
        val seconds = secondsStr.toLongOrNull() ?: return null
        // Millis may be 1-3 digits; pad so ".5" means 500ms, not 5ms.
        val millis = if (millisStr.isEmpty()) 0L else millisStr.padEnd(3, '0').toLongOrNull() ?: return null
        return ((hours * 3600 + minutes * 60 + seconds) * 1000) + millis
    }

    /** Drops inline markup like VTT voice tags (`<v Speaker>`) and styling tags. */
    private fun stripTags(text: String): String {
        return text.replace(TAG_REGEX, "").trim()
    }

    /**
     * Collapses consecutive cues that carry the same text (some generators repeat a
     * caption across adjacent timestamp blocks); keeps the earliest start.
     */
    private fun mergeConsecutive(cues: List<TranscriptCue>): List<TranscriptCue> {
        if (cues.size < 2) return cues
        val merged = mutableListOf<TranscriptCue>()
        for (cue in cues) {
            if (merged.isNotEmpty() && merged.last().text == cue.text) continue
            merged.add(cue)
        }
        return merged
    }

    /** Reads at most [limit] bytes; throws when the body exceeds the cap. */
    private fun readCapped(stream: InputStream, limit: Long, url: String): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) {
                throw IOException("Transcript exceeds ${limit / (1024 * 1024)}MB limit: $url")
            }
            out.write(buffer, 0, read)
        }
        return out.toString("UTF-8")
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder().build()

        /** Maximum bytes we will read from a transcript (same 10MB cap as feeds). */
        private const val MAX_TRANSCRIPT_BYTES = 10L * 1024 * 1024

        // Optional hours, then MM:SS with optional .mmm or ,mmm — covers VTT
        // (00:01.000 / 00:00:01.000) and SRT (00:00:01,000).
        private val TIMESTAMP_REGEX = Regex("""(?:(\d{1,3}):)?(\d{1,2}):(\d{2})[.,]?(\d{0,3})""")

        private val TAG_REGEX = Regex("""<[^>]*>""")
    }
}
