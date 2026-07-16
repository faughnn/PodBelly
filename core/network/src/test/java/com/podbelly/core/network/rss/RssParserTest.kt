package com.podbelly.core.network.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RssParserTest {

    private lateinit var parser: RssParser
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        parser = RssParser()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Channel / Feed metadata
    // -------------------------------------------------------------------------

    @Test
    fun `parse valid RSS feed extracts channel metadata`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>My Podcast</title>
                <description>A great podcast about things.</description>
                <itunes:author>Jane Doe</itunes:author>
                <itunes:image href="https://example.com/artwork.jpg"/>
                <link>https://example.com/podcast</link>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("My Podcast", feed.title)
        assertEquals("A great podcast about things.", feed.description)
        assertEquals("Jane Doe", feed.author)
        assertEquals("https://example.com/artwork.jpg", feed.artworkUrl)
        assertEquals("https://example.com/podcast", feed.link)
    }

    @Test
    fun `parse feed uses feedUrl as link fallback when link tag is missing`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>No Link Podcast</title>
                <description>Desc</description>
                <itunes:author>Author</itunes:author>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/fallback-feed.xml", xml)

        assertEquals("https://example.com/fallback-feed.xml", feed.link)
    }

    @Test
    fun `parse feed uses itunes summary as description fallback`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Summary Podcast</title>
                <itunes:summary>Itunes summary text here</itunes:summary>
                <itunes:author>Author</itunes:author>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("Itunes summary text here", feed.description)
    }

    // -------------------------------------------------------------------------
    // Episode parsing
    // -------------------------------------------------------------------------

    @Test
    fun `parse episodes extracts all episode fields`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test Podcast</title>
                <description>Description</description>
                <item>
                  <title>Episode 1</title>
                  <guid>episode-1-guid</guid>
                  <enclosure url="https://example.com/ep1.mp3" length="12345678" type="audio/mpeg"/>
                  <itunes:duration>1:02:30</itunes:duration>
                  <pubDate>Mon, 01 Jan 2024 12:00:00 +0000</pubDate>
                  <description>Episode one description</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(1, feed.episodes.size)

        val episode = feed.episodes[0]
        assertEquals("Episode 1", episode.title)
        assertEquals("episode-1-guid", episode.guid)
        assertEquals("https://example.com/ep1.mp3", episode.audioUrl)
        assertEquals("Episode one description", episode.description)
        assertEquals(12345678L, episode.fileSize)

        // Duration: 1h 2m 30s = 3750 seconds = 3750000 ms
        assertEquals(3750000L, episode.duration)

        // publishedAt should be non-zero (parsed from the date string)
        assertTrue("publishedAt should be > 0", episode.publishedAt > 0L)
    }

    @Test
    fun `parse multiple episodes preserves order`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Multi Episode Podcast</title>
                <description>Desc</description>
                <item>
                  <title>Episode 1</title>
                  <guid>guid-1</guid>
                  <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg" length="100"/>
                </item>
                <item>
                  <title>Episode 2</title>
                  <guid>guid-2</guid>
                  <enclosure url="https://example.com/ep2.mp3" type="audio/mpeg" length="200"/>
                </item>
                <item>
                  <title>Episode 3</title>
                  <guid>guid-3</guid>
                  <enclosure url="https://example.com/ep3.mp3" type="audio/mpeg" length="300"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(3, feed.episodes.size)
        assertEquals("Episode 1", feed.episodes[0].title)
        assertEquals("Episode 2", feed.episodes[1].title)
        assertEquals("Episode 3", feed.episodes[2].title)
    }

    // -------------------------------------------------------------------------
    // Empty and missing fields
    // -------------------------------------------------------------------------

    @Test
    fun `parse feed with no episodes returns empty list`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Empty Podcast</title>
                <description>No episodes here</description>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertTrue(feed.episodes.isEmpty())
    }

    @Test
    fun `episodes without enclosure are excluded`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test</title>
                <description>Desc</description>
                <item>
                  <title>No Audio Episode</title>
                  <guid>guid-no-audio</guid>
                  <description>This item has no enclosure</description>
                </item>
                <item>
                  <title>Has Audio</title>
                  <guid>guid-has-audio</guid>
                  <enclosure url="https://example.com/audio.mp3" type="audio/mpeg" length="500"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(1, feed.episodes.size)
        assertEquals("Has Audio", feed.episodes[0].title)
    }

    @Test
    fun `episode artworkUrl is null when itunes image is missing`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test</title>
                <description>Desc</description>
                <item>
                  <title>No Artwork Episode</title>
                  <guid>guid-no-art</guid>
                  <enclosure url="https://example.com/ep.mp3" type="audio/mpeg" length="100"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(1, feed.episodes.size)
        assertNull(feed.episodes[0].artworkUrl)
    }

    @Test
    fun `episode artworkUrl is set when itunes image is present`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test</title>
                <description>Desc</description>
                <item>
                  <title>Artwork Episode</title>
                  <guid>guid-art</guid>
                  <enclosure url="https://example.com/ep.mp3" type="audio/mpeg" length="100"/>
                  <itunes:image href="https://example.com/ep-art.jpg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(1, feed.episodes.size)
        assertEquals("https://example.com/ep-art.jpg", feed.episodes[0].artworkUrl)
    }

    // -------------------------------------------------------------------------
    // GUID generation fallback
    // -------------------------------------------------------------------------

    @Test
    fun `missing guid tag generates deterministic guid from title and audioUrl`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test</title>
                <description>Desc</description>
                <item>
                  <title>No GUID Episode</title>
                  <enclosure url="https://example.com/ep.mp3" type="audio/mpeg" length="100"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(1, feed.episodes.size)
        val episode = feed.episodes[0]

        // The guid should not be blank
        assertTrue("Generated GUID should not be blank", episode.guid.isNotBlank())
        // It should be a SHA-256 hex string (64 characters)
        assertEquals(64, episode.guid.length)
        assertTrue("GUID should be hex", episode.guid.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `generated guid is deterministic for same title and audioUrl`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Test</title>
                <description>Desc</description>
                <item>
                  <title>Deterministic GUID</title>
                  <enclosure url="https://example.com/ep-det.mp3" type="audio/mpeg" length="100"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed1 = parser.parse("https://example.com/feed.xml", xml)
        val feed2 = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(feed1.episodes[0].guid, feed2.episodes[0].guid)
    }

    // -------------------------------------------------------------------------
    // Duration parsing
    // -------------------------------------------------------------------------

    @Test
    fun `parseDuration with HH MM SS format`() {
        // 1:30:45 = 1*3600 + 30*60 + 45 = 5445 seconds = 5445000 ms
        assertEquals(5445000L, parser.parseDuration("1:30:45"))
    }

    @Test
    fun `parseDuration with MM SS format`() {
        // 45:30 = 45*60 + 30 = 2730 seconds = 2730000 ms
        assertEquals(2730000L, parser.parseDuration("45:30"))
    }

    @Test
    fun `parseDuration with raw seconds`() {
        // 3600 seconds = 3600000 ms
        assertEquals(3600000L, parser.parseDuration("3600"))
    }

    @Test
    fun `parseDuration with fractional seconds in HH MM SS`() {
        // 0:01:30.5 = 90.5 seconds = 90500 ms
        assertEquals(90500L, parser.parseDuration("0:01:30.5"))
    }

    @Test
    fun `parseDuration with zero`() {
        assertEquals(0L, parser.parseDuration("0"))
    }

    @Test
    fun `parseDuration with blank string`() {
        assertEquals(0L, parser.parseDuration(""))
        assertEquals(0L, parser.parseDuration("   "))
    }

    @Test
    fun `parseDuration with single digit hours`() {
        // 2:05:10 = 2*3600 + 5*60 + 10 = 7510 seconds = 7510000 ms
        assertEquals(7510000L, parser.parseDuration("2:05:10"))
    }

    @Test
    fun `parseDuration with leading and trailing whitespace`() {
        assertEquals(3600000L, parser.parseDuration("  1:00:00  "))
    }

    // -------------------------------------------------------------------------
    // Date parsing
    // -------------------------------------------------------------------------

    @Test
    fun `parseRfc822Date with standard RFC822 format`() {
        val result = parser.parseRfc822Date("Mon, 01 Jan 2024 12:00:00 +0000")
        assertTrue("Should parse valid RFC822 date", result > 0L)
    }

    @Test
    fun `parseRfc822Date with blank string returns zero`() {
        assertEquals(0L, parser.parseRfc822Date(""))
        assertEquals(0L, parser.parseRfc822Date("   "))
    }

    @Test
    fun `parseRfc822Date with ISO 8601 format`() {
        val result = parser.parseRfc822Date("2024-01-15T10:30:00Z")
        assertTrue("Should parse ISO 8601 date", result > 0L)
    }

    @Test
    fun `parseRfc822Date with timezone abbreviation`() {
        val result = parser.parseRfc822Date("Mon, 01 Jan 2024 12:00:00 EST")
        assertTrue("Should parse date with timezone abbreviation", result > 0L)
    }

    // -------------------------------------------------------------------------
    // Channel-level image fallback (non-itunes <image><url>...)
    // -------------------------------------------------------------------------

    @Test
    fun `channel image falls back to standard RSS image url element`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Standard Image Podcast</title>
                <description>Desc</description>
                <image>
                  <url>https://example.com/standard-art.png</url>
                  <title>Standard Image Podcast</title>
                  <link>https://example.com</link>
                </image>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("https://example.com/standard-art.png", feed.artworkUrl)
    }

    @Test
    fun `itunes image takes precedence over standard RSS image`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Dual Image Podcast</title>
                <description>Desc</description>
                <itunes:image href="https://example.com/itunes-art.jpg"/>
                <image>
                  <url>https://example.com/standard-art.png</url>
                  <title>Dual Image Podcast</title>
                  <link>https://example.com</link>
                </image>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("https://example.com/itunes-art.jpg", feed.artworkUrl)
    }

    // -------------------------------------------------------------------------
    // Streaming input (InputStream overload)
    // -------------------------------------------------------------------------

    private fun streamFeedXml(title: String, encodingDecl: String?) = buildString {
        append("<?xml version=\"1.0\"")
        if (encodingDecl != null) append(" encoding=\"$encodingDecl\"")
        append("?>\n")
        append(
            """
            <rss version="2.0">
              <channel>
                <title>$title</title>
                <description>Desc</description>
              </channel>
            </rss>
            """.trimIndent()
        )
    }

    @Test
    fun `streaming parse produces the same result as string parse`() = runTest {
        val xml = streamFeedXml("Streamed Podcast", "UTF-8")

        val fromString = parser.parse("https://example.com/feed.xml", xml)
        val fromStream = parser.parse(
            "https://example.com/feed.xml",
            xml.byteInputStream(Charsets.UTF_8),
            null,
        )

        assertEquals(fromString, fromStream)
    }

    @Test
    fun `streaming parse sniffs ISO-8859-1 from the XML prolog when no header charset`() = runTest {
        // "Séance Café" — bytes are ISO-8859-1, declared only in the prolog.
        val xml = streamFeedXml("Séance Café", "ISO-8859-1")

        val feed = parser.parse(
            "https://example.com/feed.xml",
            xml.byteInputStream(Charsets.ISO_8859_1),
            null,
        )

        assertEquals("Séance Café", feed.title)
    }

    @Test
    fun `streaming parse prefers the header charset over the prolog`() = runTest {
        // Server says ISO-8859-1 in Content-Type while the prolog lies (UTF-8);
        // header must win, matching the old buffered implementation.
        val xml = streamFeedXml("Séance Café", "UTF-8")

        val feed = parser.parse(
            "https://example.com/feed.xml",
            xml.byteInputStream(Charsets.ISO_8859_1),
            "ISO-8859-1",
        )

        assertEquals("Séance Café", feed.title)
    }

    @Test
    fun `streaming parse defaults to UTF-8 without header or prolog encoding`() = runTest {
        val xml = streamFeedXml("Séance Café", null)

        val feed = parser.parse(
            "https://example.com/feed.xml",
            xml.byteInputStream(Charsets.UTF_8),
            null,
        )

        assertEquals("Séance Café", feed.title)
    }

    // -------------------------------------------------------------------------
    // Podcasting 2.0 transcript (<podcast:transcript>)
    // -------------------------------------------------------------------------

    // The injected tags are collapsed onto one line BEFORE interpolation: a
    // multi-line argument would contribute column-0 lines to the raw string,
    // turning the outer trimIndent() into a no-op and leaving whitespace before
    // the XML declaration — which strict parsers (MXParser on the unit-test
    // classpath) reject even though lenient ones (kxml2) accept it.
    private fun transcriptFeedXml(transcriptTags: String): String = transcriptFeedXmlTemplate(
        transcriptTags.lines().joinToString(" ") { it.trim() }
    )

    private fun transcriptFeedXmlTemplate(transcriptTagsOneLine: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"
             xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
             xmlns:podcast="https://podcastindex.org/namespace/1.0">
          <channel>
            <title>Transcript Podcast</title>
            <description>Desc</description>
            <item>
              <title>Episode 1</title>
              <guid>guid-1</guid>
              <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg" length="100"/>
              $transcriptTagsOneLine
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun `parse extracts podcast transcript url and type`() = runTest {
        val xml = transcriptFeedXml(
            """<podcast:transcript url="https://example.com/ep1.vtt" type="text/vtt"/>"""
        )

        val feed = parser.parse("https://example.com/feed.xml", xml)

        val episode = feed.episodes[0]
        assertEquals("https://example.com/ep1.vtt", episode.transcriptUrl)
        assertEquals("text/vtt", episode.transcriptType)
    }

    @Test
    fun `episode without transcript has null transcript fields`() = runTest {
        val xml = transcriptFeedXml("")

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertNull(feed.episodes[0].transcriptUrl)
        assertNull(feed.episodes[0].transcriptType)
    }

    @Test
    fun `multiple transcripts prefer JSON over VTT and SRT`() = runTest {
        val xml = transcriptFeedXml(
            """
            <podcast:transcript url="https://example.com/ep1.srt" type="application/x-subrip"/>
            <podcast:transcript url="https://example.com/ep1.vtt" type="text/vtt"/>
            <podcast:transcript url="https://example.com/ep1.json" type="application/json"/>
            """.trimIndent()
        )

        val feed = parser.parse("https://example.com/feed.xml", xml)

        val episode = feed.episodes[0]
        assertEquals("https://example.com/ep1.json", episode.transcriptUrl)
        assertEquals("application/json", episode.transcriptType)
    }

    @Test
    fun `multiple transcripts prefer VTT over SRT and HTML`() = runTest {
        val xml = transcriptFeedXml(
            """
            <podcast:transcript url="https://example.com/ep1.html" type="text/html"/>
            <podcast:transcript url="https://example.com/ep1.vtt" type="text/vtt"/>
            <podcast:transcript url="https://example.com/ep1.srt" type="application/srt"/>
            """.trimIndent()
        )

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("https://example.com/ep1.vtt", feed.episodes[0].transcriptUrl)
        assertEquals("text/vtt", feed.episodes[0].transcriptType)
    }

    @Test
    fun `unpreferred transcript type is still kept when it is the only one`() = runTest {
        val xml = transcriptFeedXml(
            """<podcast:transcript url="https://example.com/ep1.html" type="text/html"/>"""
        )

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals("https://example.com/ep1.html", feed.episodes[0].transcriptUrl)
        assertEquals("text/html", feed.episodes[0].transcriptType)
    }

    @Test
    fun `transcript without url is ignored`() = runTest {
        val xml = transcriptFeedXml("""<podcast:transcript type="text/vtt"/>""")

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertNull(feed.episodes[0].transcriptUrl)
    }

    @Test
    fun `transcript does not leak into the next item`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:podcast="https://podcastindex.org/namespace/1.0">
              <channel>
                <title>Transcript Podcast</title>
                <item>
                  <title>Episode 1</title>
                  <guid>guid-1</guid>
                  <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg"/>
                  <podcast:transcript url="https://example.com/ep1.vtt" type="text/vtt"/>
                </item>
                <item>
                  <title>Episode 2</title>
                  <guid>guid-2</guid>
                  <enclosure url="https://example.com/ep2.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse("https://example.com/feed.xml", xml)

        assertEquals(2, feed.episodes.size)
        assertEquals("https://example.com/ep1.vtt", feed.episodes[0].transcriptUrl)
        assertNull(feed.episodes[1].transcriptUrl)
    }

    @Test
    fun `streaming parse strips a UTF-8 byte-order mark`() = runTest {
        // A BOM decoded as ordinary text yields U+FEFF before the prolog, which
        // XML parsers reject ("PI must not start with xml") — resolveCharset must
        // consume it. The old buffered implementation failed on such feeds.
        val xml = streamFeedXml("Séance Café", "UTF-8")
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            xml.toByteArray(Charsets.UTF_8)

        val feed = parser.parse(
            "https://example.com/feed.xml",
            bytes.inputStream(),
            null,
        )

        assertEquals("Séance Café", feed.title)
    }
}
