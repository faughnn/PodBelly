package com.podbelly.core.network.opml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpmlHandlerTest {

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    @Test
    fun `parseOpml extracts feeds from valid OPML document`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head>
                <title>My Podcasts</title>
              </head>
              <body>
                <outline type="rss" text="Podcast One" title="Podcast One" xmlUrl="https://example.com/feed1.xml"/>
                <outline type="rss" text="Podcast Two" title="Podcast Two" xmlUrl="https://example.com/feed2.xml"/>
                <outline type="rss" text="Podcast Three" title="Podcast Three" xmlUrl="https://example.com/feed3.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(3, feeds.size)
        assertEquals("Podcast One", feeds[0].title)
        assertEquals("https://example.com/feed1.xml", feeds[0].feedUrl)
        assertEquals("Podcast Two", feeds[1].title)
        assertEquals("https://example.com/feed2.xml", feeds[1].feedUrl)
        assertEquals("Podcast Three", feeds[2].title)
        assertEquals("https://example.com/feed3.xml", feeds[2].feedUrl)
    }

    @Test
    fun `parseOpml handles nested outline elements`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Nested</title></head>
              <body>
                <outline text="Tech">
                  <outline type="rss" text="Tech Podcast" title="Tech Podcast" xmlUrl="https://example.com/tech.xml"/>
                </outline>
                <outline text="Comedy">
                  <outline type="rss" text="Comedy Show" title="Comedy Show" xmlUrl="https://example.com/comedy.xml"/>
                </outline>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(2, feeds.size)
        assertEquals("Tech Podcast", feeds[0].title)
        assertEquals("Comedy Show", feeds[1].title)
    }

    @Test
    fun `parseOpml uses text attribute as title fallback when title is missing`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Test</title></head>
              <body>
                <outline type="rss" text="Text Only Feed" xmlUrl="https://example.com/feed.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(1, feeds.size)
        assertEquals("Text Only Feed", feeds[0].title)
    }

    @Test
    fun `parseOpml returns empty list for OPML with no feeds`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Empty</title></head>
              <body>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertTrue(feeds.isEmpty())
    }

    @Test
    fun `parseOpml handles outline with xmlUrl but no type attribute`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Test</title></head>
              <body>
                <outline text="No Type Feed" title="No Type Feed" xmlUrl="https://example.com/notype.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(1, feeds.size)
        assertEquals("No Type Feed", feeds[0].title)
        assertEquals("https://example.com/notype.xml", feeds[0].feedUrl)
    }

    @Test
    fun `parseOpml skips outlines without xmlUrl and without rss type`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Test</title></head>
              <body>
                <outline text="Category Only"/>
                <outline type="rss" text="Real Feed" xmlUrl="https://example.com/real.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(1, feeds.size)
        assertEquals("Real Feed", feeds[0].title)
    }

    @Test
    fun `parseOpml trims whitespace from title and feedUrl`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Test</title></head>
              <body>
                <outline type="rss" text="  Spaced Title  " title="  Spaced Title  " xmlUrl="  https://example.com/feed.xml  "/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHandler.parseOpml(opml)

        assertEquals(1, feeds.size)
        assertEquals("Spaced Title", feeds[0].title)
        assertEquals("https://example.com/feed.xml", feeds[0].feedUrl)
    }

    // -------------------------------------------------------------------------
    // Generation
    // -------------------------------------------------------------------------

    @Test
    fun `generateOpml produces valid OPML with feed entries`() {
        val feeds = listOf(
            OpmlFeed(title = "Podcast A", feedUrl = "https://example.com/a.xml"),
            OpmlFeed(title = "Podcast B", feedUrl = "https://example.com/b.xml"),
        )

        val opmlXml = OpmlHandler.generateOpml(feeds)

        // The generated OPML should contain the feed URLs and titles
        assertTrue("Should contain Podcast A title", opmlXml.contains("Podcast A"))
        assertTrue("Should contain Podcast B title", opmlXml.contains("Podcast B"))
        assertTrue("Should contain feed A URL", opmlXml.contains("https://example.com/a.xml"))
        assertTrue("Should contain feed B URL", opmlXml.contains("https://example.com/b.xml"))
        assertTrue("Should contain opml tag", opmlXml.contains("<opml"))
        assertTrue("Should contain outline tags", opmlXml.contains("<outline"))
    }

    @Test
    fun `generateOpml with empty list produces valid OPML with no outlines`() {
        val opmlXml = OpmlHandler.generateOpml(emptyList())

        assertTrue("Should contain opml tag", opmlXml.contains("<opml"))
        assertTrue("Should contain body tag", opmlXml.contains("<body"))
        // Should not contain any outline elements
        assertTrue("Should not contain outline tags", !opmlXml.contains("<outline"))
    }

    // -------------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `round trip generate then parse produces same feeds`() {
        val originalFeeds = listOf(
            OpmlFeed(title = "Alpha Podcast", feedUrl = "https://example.com/alpha.xml"),
            OpmlFeed(title = "Beta Podcast", feedUrl = "https://example.com/beta.xml"),
            OpmlFeed(title = "Gamma Podcast", feedUrl = "https://example.com/gamma.xml"),
        )

        val opmlXml = OpmlHandler.generateOpml(originalFeeds)
        val parsedFeeds = OpmlHandler.parseOpml(opmlXml)

        assertEquals(originalFeeds.size, parsedFeeds.size)
        for (i in originalFeeds.indices) {
            assertEquals(originalFeeds[i].title, parsedFeeds[i].title)
            assertEquals(originalFeeds[i].feedUrl, parsedFeeds[i].feedUrl)
        }
    }

    @Test
    fun `round trip with empty list`() {
        val opmlXml = OpmlHandler.generateOpml(emptyList())
        val parsedFeeds = OpmlHandler.parseOpml(opmlXml)

        assertTrue(parsedFeeds.isEmpty())
    }

    @Test
    fun `round trip preserves special characters in titles`() {
        val originalFeeds = listOf(
            OpmlFeed(title = "Science & Technology", feedUrl = "https://example.com/sci.xml"),
        )

        val opmlXml = OpmlHandler.generateOpml(originalFeeds)
        val parsedFeeds = OpmlHandler.parseOpml(opmlXml)

        assertEquals(1, parsedFeeds.size)
        assertEquals("Science & Technology", parsedFeeds[0].title)
        assertEquals("https://example.com/sci.xml", parsedFeeds[0].feedUrl)
    }
}
