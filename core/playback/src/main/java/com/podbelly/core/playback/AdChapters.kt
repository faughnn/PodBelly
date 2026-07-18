package com.podbelly.core.playback

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame

/**
 * Chapter titles that unambiguously mark the whole chapter as an advertisement.
 * Matched against the trimmed, lowercased title. Kept to exact phrases so that
 * ordinary titles that merely *start* with "Ad" ("Ad Astra review") never match.
 */
private val AD_TITLES = setOf(
    "ad",
    "ads",
    "advert",
    "adverts",
    "advertisement",
    "advertisements",
    "advertising",
    "ad break",
    "adbreak",
    "commercial",
    "commercials",
    "commercial break",
    "promo",
    "promos",
    "promotion",
    "paid promotion",
    "sponsor",
    "sponsors",
    "sponsorship",
    "sponsor break",
    "sponsored",
    "werbung",
)

/**
 * Prefixes that mark a chapter as an ad even with extra text after them
 * ("Sponsor: Squarespace", "Ad break 2"). Each prefix is either punctuated or
 * a full ad word on its own, so "Ad Astra" and the like can't slip through.
 */
private val AD_TITLE_PREFIXES = listOf(
    "ad:",
    "ads:",
    "ad break",
    "advert",
    "commercial break",
    "promo:",
    "paid promotion",
    "sponsor:",
    "sponsors:",
    "sponsored ",
    "sponsor break",
    "sponsorship",
    "werbung",
)

/**
 * Whether a chapter title marks the chapter as an advertisement, so the
 * "Skip ad chapters" setting can jump over it (the pattern podcast apps use for
 * chapter-based ad skipping — publishers like Relay and NPR mark ad chapters).
 */
fun isAdChapterTitle(title: String): Boolean {
    val normalized = title.trim().lowercase()
    if (normalized.isEmpty()) return false
    if (normalized in AD_TITLES) return true
    return AD_TITLE_PREFIXES.any { normalized.startsWith(it) }
}

/**
 * Builds the chapter list from the ID3 metadata of the playing file (CHAP
 * frames, the standard way MP3 podcasts embed chapters). Chapters are sorted
 * by start time; an end time the encoder left invalid (missing CHAP end times
 * are common) is repaired from the next chapter's start, falling back to
 * [durationMs] for the last chapter (0 = unknown, which leaves the chapter
 * zero-length and therefore never auto-skipped).
 */
@OptIn(UnstableApi::class)
fun chaptersFromMetadata(metadata: List<Metadata>, durationMs: Long = 0L): List<Chapter> {
    val raw = mutableListOf<Chapter>()
    for (meta in metadata) {
        for (i in 0 until meta.length()) {
            val frame = meta.get(i) as? ChapterFrame ?: continue
            val title = (0 until frame.subFrameCount)
                .asSequence()
                .map { frame.getSubFrame(it) }
                .filterIsInstance<TextInformationFrame>()
                .firstOrNull { it.id == "TIT2" }
                ?.values?.firstOrNull()
                ?: ""
            raw.add(
                Chapter(
                    title = title,
                    // CHAP times are unsigned 32-bit; a negative int here means
                    // the encoder wrote 0xFFFFFFFF ("not set").
                    startTimeMs = frame.startTimeMs.toLong().coerceAtLeast(0L),
                    endTimeMs = frame.endTimeMs.toLong(),
                )
            )
        }
    }
    if (raw.isEmpty()) return emptyList()

    val sorted = raw.sortedBy { it.startTimeMs }
    return sorted.mapIndexed { index, chapter ->
        val nextStart = sorted.getOrNull(index + 1)?.startTimeMs
        val end = when {
            chapter.endTimeMs > chapter.startTimeMs -> chapter.endTimeMs
            nextStart != null && nextStart > chapter.startTimeMs -> nextStart
            durationMs > chapter.startTimeMs -> durationMs
            else -> chapter.startTimeMs
        }
        chapter.copy(endTimeMs = end)
    }
}
