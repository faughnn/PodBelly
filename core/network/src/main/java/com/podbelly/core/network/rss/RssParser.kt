package com.podbelly.core.network.rss

import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * A robust RSS 2.0 feed parser that uses [XmlPullParser] to extract channel
 * metadata and episode information from podcast RSS feeds.
 *
 * Handles:
 * - Standard RSS 2.0 elements (<channel>, <item>, <enclosure>, etc.)
 * - iTunes podcast namespace (itunes:author, itunes:image, itunes:duration, itunes:summary, etc.)
 * - content:encoded for rich descriptions
 * - CDATA sections
 * - Multiple date formats (RFC 822 variants, ISO 8601)
 * - Duration in HH:MM:SS, MM:SS, and raw seconds formats
 * - Missing or malformed fields with sensible defaults
 */
class RssParser @Inject constructor() {

    companion object {
        private const val NS_ITUNES = "http://www.itunes.com/dtds/podcast-1.0.dtd"
        private const val NS_CONTENT = "http://purl.org/rss/1.0/modules/content/"

        private val RFC822_FORMATS = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm Z",
            "EEE, dd MMM yyyy HH:mm z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd"
        )
    }

    /**
     * Parses the given [xmlContent] as an RSS 2.0 feed and returns an [RssFeed].
     *
     * @param feedUrl The original URL of the feed, used as a fallback for the link field.
     * @param xmlContent The raw XML string of the RSS feed.
     * @return A parsed [RssFeed] containing channel metadata and a list of [RssEpisode]s.
     */
    suspend fun parse(feedUrl: String, xmlContent: String): RssFeed = withContext(Dispatchers.IO) {
        parseInternal(feedUrl, xmlContent)
    }

    private fun parseInternal(feedUrl: String, xmlContent: String): RssFeed {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var channelTitle = ""
        var channelAuthor = ""
        var channelDescription = ""
        var channelArtworkUrl = ""
        var channelLink = ""
        val episodes = mutableListOf<RssEpisode>()

        var insideChannel = false
        var insideItem = false
        var insideChannelImage = false

        // Item-level fields
        var itemTitle = ""
        var itemDescription = ""
        var itemSummary = ""
        var itemGuid = ""
        var itemAudioUrl = ""
        var itemDuration = 0L
        var itemPublishedAt = 0L
        var itemFileSize = 0L
        var itemArtworkUrl: String? = null
        var itemLink = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name ?: ""
                    val ns = parser.namespace ?: ""

                    when {
                        tag == "channel" && !insideChannel -> {
                            insideChannel = true
                        }

                        tag == "item" && insideChannel && !insideItem -> {
                            insideItem = true
                            itemTitle = ""
                            itemDescription = ""
                            itemSummary = ""
                            itemGuid = ""
                            itemAudioUrl = ""
                            itemDuration = 0L
                            itemPublishedAt = 0L
                            itemFileSize = 0L
                            itemArtworkUrl = null
                            itemLink = ""
                        }

                        // --- Inside <item> ---
                        insideItem -> {
                            when {
                                tag == "title" && ns.isEmpty() -> {
                                    itemTitle = readText(parser)
                                }
                                tag == "description" && ns.isEmpty() -> {
                                    itemDescription = readText(parser)
                                }
                                tag == "summary" && ns == NS_ITUNES -> {
                                    itemSummary = readText(parser)
                                }
                                tag == "encoded" && ns == NS_CONTENT -> {
                                    val encoded = readText(parser)
                                    if (itemDescription.isBlank()) {
                                        itemDescription = encoded
                                    }
                                }
                                tag == "guid" -> {
                                    itemGuid = readText(parser)
                                }
                                tag == "enclosure" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    val length = parser.getAttributeValue(null, "length")
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    if (!url.isNullOrBlank() &&
                                        (type.startsWith("audio/") || type.isBlank() || type.startsWith("video/"))
                                    ) {
                                        itemAudioUrl = url.trim()
                                        itemFileSize = length?.toLongOrNull() ?: 0L
                                    }
                                }
                                tag == "pubDate" && ns.isEmpty() -> {
                                    val dateStr = readText(parser)
                                    itemPublishedAt = parseRfc822Date(dateStr)
                                }
                                tag == "duration" && ns == NS_ITUNES -> {
                                    val durationStr = readText(parser)
                                    itemDuration = parseDuration(durationStr)
                                }
                                tag == "image" && ns == NS_ITUNES -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        itemArtworkUrl = href.trim()
                                    }
                                }
                                tag == "episode" && ns == NS_ITUNES -> {
                                    // Read and discard; episode number not stored in our model
                                    readText(parser)
                                }
                                tag == "link" && ns.isEmpty() -> {
                                    itemLink = readText(parser)
                                }
                            }
                        }

                        // --- Inside <channel> but not inside <item> ---
                        insideChannel && !insideItem -> {
                            when {
                                tag == "title" && ns.isEmpty() -> {
                                    channelTitle = readText(parser)
                                }
                                tag == "description" && ns.isEmpty() -> {
                                    channelDescription = readText(parser)
                                }
                                tag == "summary" && ns == NS_ITUNES -> {
                                    if (channelDescription.isBlank()) {
                                        channelDescription = readText(parser)
                                    }
                                }
                                tag == "author" && ns == NS_ITUNES -> {
                                    channelAuthor = readText(parser)
                                }
                                tag == "link" && ns.isEmpty() -> {
                                    channelLink = readText(parser)
                                }
                                tag == "image" && ns == NS_ITUNES -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        channelArtworkUrl = href.trim()
                                    }
                                }
                                tag == "image" && ns.isEmpty() -> {
                                    insideChannelImage = true
                                }
                                tag == "url" && insideChannelImage -> {
                                    val url = readText(parser)
                                    if (channelArtworkUrl.isBlank() && url.isNotBlank()) {
                                        channelArtworkUrl = url.trim()
                                    }
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val tag = parser.name ?: ""

                    when {
                        tag == "channel" && insideChannel && !insideItem -> {
                            insideChannel = false
                        }

                        tag == "item" && insideItem -> {
                            insideItem = false

                            // Prefer description; fall back to itunes:summary
                            val finalDescription = itemDescription.ifBlank { itemSummary }

                            val resolvedGuid = itemGuid.ifBlank {
                                generateGuid(itemTitle, itemAudioUrl)
                            }

                            // Only add episodes that have an audio URL
                            if (itemAudioUrl.isNotBlank()) {
                                episodes.add(
                                    RssEpisode(
                                        guid = resolvedGuid,
                                        title = itemTitle,
                                        description = finalDescription,
                                        audioUrl = itemAudioUrl,
                                        duration = itemDuration,
                                        publishedAt = itemPublishedAt,
                                        fileSize = itemFileSize,
                                        artworkUrl = itemArtworkUrl
                                    )
                                )
                            }
                        }

                        tag == "image" && insideChannelImage && !insideItem -> {
                            insideChannelImage = false
                        }
                    }
                }
            }

            eventType = parser.next()
        }

        return RssFeed(
            title = channelTitle,
            author = channelAuthor,
            description = channelDescription,
            artworkUrl = channelArtworkUrl,
            link = channelLink.ifBlank { feedUrl },
            episodes = episodes
        )
    }

    /**
     * Reads the text content of the current element, handling CDATA and mixed content.
     * After this call, the parser will be positioned on the END_TAG of the element.
     */
    private fun readText(parser: XmlPullParser): String {
        val result = StringBuilder()
        var depth = 1

        while (depth > 0) {
            val next = parser.next()
            when (next) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                    result.append(parser.text ?: "")
                }
                XmlPullParser.START_TAG -> {
                    depth++
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
                XmlPullParser.END_DOCUMENT -> {
                    break
                }
            }
        }

        return result.toString().trim()
    }

    /**
     * Parses duration strings in various formats:
     * - "HH:MM:SS" -> milliseconds
     * - "MM:SS" -> milliseconds
     * - "SS" or raw seconds -> milliseconds
     * - "H:MM:SS" -> milliseconds
     */
    internal fun parseDuration(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return 0L

        // Try raw seconds first (plain number, possibly with decimals)
        trimmed.toDoubleOrNull()?.let { seconds ->
            return (seconds * 1000).toLong()
        }

        // Split by colon
        val parts = trimmed.split(":")
        return try {
            when (parts.size) {
                3 -> {
                    val hours = parts[0].trim().toLongOrNull() ?: 0L
                    val minutes = parts[1].trim().toLongOrNull() ?: 0L
                    val seconds = parts[2].trim().toDoubleOrNull() ?: 0.0
                    ((hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).toLong())
                }
                2 -> {
                    val minutes = parts[0].trim().toLongOrNull() ?: 0L
                    val seconds = parts[1].trim().toDoubleOrNull() ?: 0.0
                    (minutes * 60 * 1000 + (seconds * 1000).toLong())
                }
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Parses RFC 822 date strings with multiple fallback formats.
     * Returns epoch milliseconds, or 0 if unparseable.
     */
    internal fun parseRfc822Date(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return 0L

        // Some feeds include extra timezone names like "PST", "EDT" in addition to offset.
        // Clean those up by stripping trailing alpha timezone abbreviations when an offset is present.
        val cleaned = cleanDateString(trimmed)

        for (format in RFC822_FORMATS) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = true
                }
                val date = sdf.parse(cleaned)
                if (date != null) {
                    return date.time
                }
            } catch (_: ParseException) {
                // Try next format
            }
        }

        // Last resort: try the original untouched string
        if (cleaned != trimmed) {
            for (format in RFC822_FORMATS) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = true
                    }
                    val date = sdf.parse(trimmed)
                    if (date != null) {
                        return date.time
                    }
                } catch (_: ParseException) {
                    // Try next format
                }
            }
        }

        return 0L
    }

    /**
     * Cleans common date string anomalies:
     * - Extra whitespace
     * - Duplicate timezone info (e.g. "+0000 (UTC)")
     * - Missing timezone (assume UTC)
     */
    private fun cleanDateString(raw: String): String {
        var s = raw.replace(Regex("\\s+"), " ").trim()

        // Remove parenthesized timezone names: "(UTC)", "(PST)", etc.
        s = s.replace(Regex("\\s*\\([A-Za-z]+\\)\\s*$"), "").trim()

        // Some feeds use timezone abbreviations without offset.
        // Replace common ones with numeric offsets.
        val tzReplacements = mapOf(
            "EST" to "-0500", "EDT" to "-0400",
            "CST" to "-0600", "CDT" to "-0500",
            "MST" to "-0700", "MDT" to "-0600",
            "PST" to "-0800", "PDT" to "-0700",
            "UTC" to "+0000", "GMT" to "+0000",
            "UT" to "+0000"
        )

        // Only replace if the timezone abbreviation is the last token and there's no numeric offset
        val endsWithAlphaTz = Regex("\\s([A-Z]{2,4})$").find(s)
        if (endsWithAlphaTz != null) {
            val tz = endsWithAlphaTz.groupValues[1]
            val replacement = tzReplacements[tz]
            if (replacement != null) {
                // Check there's no numeric offset already present
                val beforeTz = s.substring(0, endsWithAlphaTz.range.first).trim()
                if (!Regex("[+-]\\d{4}$").containsMatchIn(beforeTz)) {
                    s = "$beforeTz $replacement"
                }
            }
        }

        return s
    }

    /**
     * Generates a deterministic GUID from title and audio URL using SHA-256.
     */
    private fun generateGuid(title: String, audioUrl: String): String {
        val input = "$title|$audioUrl"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
