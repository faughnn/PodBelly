package com.podbelly.core.network.api

import com.podbelly.core.network.rss.RssParser
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastSearchRepositoryIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: PodcastSearchRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder().build()
        val moshi = Moshi.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(ItunesSearchApi::class.java)
        val rssParser = RssParser()
        repository = PodcastSearchRepository(api, okHttpClient, rssParser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mockWebServer.shutdown()
    }

    // -------------------------------------------------------------------------
    // search() tests
    // -------------------------------------------------------------------------

    @Test
    fun `search returns mapped SearchResult list on successful response`() = runTest {
        val json = """
            {
              "resultCount": 2,
              "results": [
                {
                  "trackName": "The Daily",
                  "artistName": "The New York Times",
                  "feedUrl": "https://feeds.example.com/the-daily",
                  "artworkUrl600": "https://img.example.com/the-daily.jpg",
                  "collectionName": "The Daily Collection"
                },
                {
                  "trackName": "Serial",
                  "artistName": "Serial Productions",
                  "feedUrl": "https://feeds.example.com/serial",
                  "artworkUrl600": "https://img.example.com/serial.jpg",
                  "collectionName": "Serial Collection"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        )

        val results = repository.search("podcast")

        assertEquals(2, results.size)

        assertEquals("The Daily", results[0].title)
        assertEquals("The New York Times", results[0].author)
        assertEquals("https://feeds.example.com/the-daily", results[0].feedUrl)
        assertEquals("https://img.example.com/the-daily.jpg", results[0].artworkUrl)

        assertEquals("Serial", results[1].title)
        assertEquals("Serial Productions", results[1].author)
        assertEquals("https://feeds.example.com/serial", results[1].feedUrl)
        assertEquals("https://img.example.com/serial.jpg", results[1].artworkUrl)

        // Verify the request was made to the correct endpoint
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.startsWith("/search?"))
        assertTrue(request.path!!.contains("term=podcast"))
        assertTrue(request.path!!.contains("media=podcast"))
        assertTrue(request.path!!.contains("entity=podcast"))
    }

    @Test
    fun `search filters out results with missing feedUrl`() = runTest {
        val json = """
            {
              "resultCount": 3,
              "results": [
                {
                  "trackName": "Has Feed",
                  "artistName": "Author A",
                  "feedUrl": "https://feeds.example.com/valid",
                  "artworkUrl600": "https://img.example.com/valid.jpg",
                  "collectionName": "Has Feed Collection"
                },
                {
                  "trackName": "No Feed Null",
                  "artistName": "Author B",
                  "feedUrl": null,
                  "artworkUrl600": "https://img.example.com/null.jpg",
                  "collectionName": "Null Feed Collection"
                },
                {
                  "trackName": "No Feed Missing",
                  "artistName": "Author C",
                  "artworkUrl600": "https://img.example.com/missing.jpg",
                  "collectionName": "Missing Feed Collection"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        )

        val results = repository.search("test")

        assertEquals(1, results.size)
        assertEquals("Has Feed", results[0].title)
        assertEquals("https://feeds.example.com/valid", results[0].feedUrl)
    }

    @Test
    fun `search filters out results with blank feedUrl`() = runTest {
        val json = """
            {
              "resultCount": 2,
              "results": [
                {
                  "trackName": "Blank Feed",
                  "artistName": "Author",
                  "feedUrl": "   ",
                  "artworkUrl600": "https://img.example.com/blank.jpg",
                  "collectionName": "Blank Collection"
                },
                {
                  "trackName": "Empty Feed",
                  "artistName": "Author",
                  "feedUrl": "",
                  "artworkUrl600": "https://img.example.com/empty.jpg",
                  "collectionName": "Empty Collection"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        )

        val results = repository.search("blank")

        assertTrue("Results with blank/empty feedUrl should be filtered out", results.isEmpty())
    }

    @Test
    fun `search returns empty list when response has zero results`() = runTest {
        val json = """
            {
              "resultCount": 0,
              "results": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        )

        val results = repository.search("xyznonexistent")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `search uses collectionName as title fallback when trackName is null`() = runTest {
        val json = """
            {
              "resultCount": 1,
              "results": [
                {
                  "trackName": null,
                  "artistName": "Author",
                  "feedUrl": "https://feeds.example.com/podcast",
                  "artworkUrl600": "https://img.example.com/art.jpg",
                  "collectionName": "Fallback Collection Name"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        )

        val results = repository.search("fallback")

        assertEquals(1, results.size)
        assertEquals("Fallback Collection Name", results[0].title)
    }

    @Test(expected = Exception::class)
    fun `search throws exception on network error`() = runTest {
        // Enqueue a response but immediately shutdown the server to simulate network error
        mockWebServer.shutdown()

        repository.search("error")
    }

    @Test(expected = Exception::class)
    fun `search throws exception on malformed JSON response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("{ this is not valid json !!!")
                .setHeader("Content-Type", "application/json")
        )

        repository.search("malformed")
    }

    @Test(expected = Exception::class)
    fun `search throws exception on HTTP 500 server error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                .setBody("Internal Server Error")
        )

        repository.search("servererror")
    }

    // -------------------------------------------------------------------------
    // fetchFeed() tests
    // -------------------------------------------------------------------------

    @Test
    fun `fetchFeed returns parsed RssFeed on successful response`() = runTest {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                 xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>Tech Talk Daily</title>
                <description>A daily podcast about technology, innovation, and the future.</description>
                <itunes:author>Jane Smith</itunes:author>
                <itunes:image href="https://img.example.com/tech-talk.jpg"/>
                <link>https://techtalkdaily.example.com</link>
                <item>
                  <title>Episode 42: The Future of AI</title>
                  <guid>tech-talk-ep-42</guid>
                  <description>We discuss the latest developments in artificial intelligence.</description>
                  <enclosure url="https://cdn.example.com/ep42.mp3" length="48000000" type="audio/mpeg"/>
                  <itunes:duration>0:45:30</itunes:duration>
                  <pubDate>Mon, 15 Jan 2024 08:00:00 +0000</pubDate>
                  <itunes:image href="https://img.example.com/ep42.jpg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(rssXml)
                .setHeader("Content-Type", "application/rss+xml")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        val feed = repository.fetchFeed(feedUrl)

        assertEquals("Tech Talk Daily", feed.title)
        assertEquals("Jane Smith", feed.author)
        assertEquals(
            "A daily podcast about technology, innovation, and the future.",
            feed.description
        )
        assertEquals("https://img.example.com/tech-talk.jpg", feed.artworkUrl)
        assertEquals("https://techtalkdaily.example.com", feed.link)

        assertEquals(1, feed.episodes.size)
        val episode = feed.episodes[0]
        assertEquals("Episode 42: The Future of AI", episode.title)
        assertEquals("tech-talk-ep-42", episode.guid)
        assertEquals(
            "We discuss the latest developments in artificial intelligence.",
            episode.description
        )
        assertEquals("https://cdn.example.com/ep42.mp3", episode.audioUrl)
        assertEquals(48000000L, episode.fileSize)
        // 45m30s = 2730s = 2730000ms
        assertEquals(2730000L, episode.duration)
        assertTrue("publishedAt should be > 0", episode.publishedAt > 0L)
        assertEquals("https://img.example.com/ep42.jpg", episode.artworkUrl)
    }

    @Test
    fun `fetchFeed parses multiple episodes from feed`() = runTest {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                 xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>History Unplugged</title>
                <description>Exploring history one episode at a time.</description>
                <itunes:author>Professor Howard</itunes:author>
                <itunes:image href="https://img.example.com/history.jpg"/>
                <link>https://historyunplugged.example.com</link>
                <item>
                  <title>Episode 3: The Renaissance</title>
                  <guid>hist-ep-3</guid>
                  <description>Art, science, and the rebirth of Europe.</description>
                  <enclosure url="https://cdn.example.com/hist-ep3.mp3" length="52000000" type="audio/mpeg"/>
                  <itunes:duration>1:05:00</itunes:duration>
                  <pubDate>Wed, 10 Jan 2024 10:00:00 +0000</pubDate>
                </item>
                <item>
                  <title>Episode 2: Ancient Rome</title>
                  <guid>hist-ep-2</guid>
                  <description>The rise and fall of the Roman Empire.</description>
                  <enclosure url="https://cdn.example.com/hist-ep2.mp3" length="45000000" type="audio/mpeg"/>
                  <itunes:duration>58:30</itunes:duration>
                  <pubDate>Wed, 03 Jan 2024 10:00:00 +0000</pubDate>
                </item>
                <item>
                  <title>Episode 1: Ancient Greece</title>
                  <guid>hist-ep-1</guid>
                  <description><![CDATA[Democracy, philosophy, and the <em>birthplace</em> of Western civilization.]]></description>
                  <enclosure url="https://cdn.example.com/hist-ep1.mp3" length="38000000" type="audio/mpeg"/>
                  <itunes:duration>48:15</itunes:duration>
                  <pubDate>Wed, 27 Dec 2023 10:00:00 +0000</pubDate>
                  <itunes:image href="https://img.example.com/hist-ep1.jpg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(rssXml)
                .setHeader("Content-Type", "application/rss+xml")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        val feed = repository.fetchFeed(feedUrl)

        assertEquals("History Unplugged", feed.title)
        assertEquals("Professor Howard", feed.author)
        assertEquals(3, feed.episodes.size)

        // Verify episodes are in feed order
        assertEquals("Episode 3: The Renaissance", feed.episodes[0].title)
        assertEquals("hist-ep-3", feed.episodes[0].guid)
        assertEquals("https://cdn.example.com/hist-ep3.mp3", feed.episodes[0].audioUrl)
        assertEquals(52000000L, feed.episodes[0].fileSize)
        // 1:05:00 = 3900s = 3900000ms
        assertEquals(3900000L, feed.episodes[0].duration)

        assertEquals("Episode 2: Ancient Rome", feed.episodes[1].title)
        assertEquals("hist-ep-2", feed.episodes[1].guid)
        assertEquals("https://cdn.example.com/hist-ep2.mp3", feed.episodes[1].audioUrl)
        // 58:30 = 3510s = 3510000ms
        assertEquals(3510000L, feed.episodes[1].duration)

        assertEquals("Episode 1: Ancient Greece", feed.episodes[2].title)
        assertEquals("hist-ep-1", feed.episodes[2].guid)
        assertEquals("https://cdn.example.com/hist-ep1.mp3", feed.episodes[2].audioUrl)
        // 48:15 = 2895s = 2895000ms
        assertEquals(2895000L, feed.episodes[2].duration)
        assertEquals("https://img.example.com/hist-ep1.jpg", feed.episodes[2].artworkUrl)

        // CDATA description should be extracted (including the HTML tags as text content)
        assertTrue(
            "CDATA description should contain birthplace",
            feed.episodes[2].description.contains("birthplace")
        )
    }

    @Test
    fun `fetchFeed sends correct User-Agent and Accept headers`() = runTest {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Header Test</title>
                <description>Testing headers</description>
                <itunes:author>Author</itunes:author>
              </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(rssXml)
                .setHeader("Content-Type", "application/rss+xml")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        repository.fetchFeed(feedUrl)

        val request = mockWebServer.takeRequest()
        assertEquals("Podbelly/1.0 (Android; Podcast App) OkHttp", request.getHeader("User-Agent"))
        assertTrue(
            "Accept header should include RSS XML types",
            request.getHeader("Accept")!!.contains("application/rss+xml")
        )
    }

    @Test(expected = IOException::class)
    fun `fetchFeed throws IOException on network error`() = runTest {
        // Shut down the server to simulate a network connectivity failure
        val feedUrl = mockWebServer.url("/feed.xml").toString()
        mockWebServer.shutdown()

        repository.fetchFeed(feedUrl)
    }

    @Test(expected = IOException::class)
    fun `fetchFeed throws IOException on non-200 response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                .setBody("Not Found")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        repository.fetchFeed(feedUrl)
    }

    @Test(expected = IOException::class)
    fun `fetchFeed throws IOException on HTTP 500 response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                .setBody("Internal Server Error")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        repository.fetchFeed(feedUrl)
    }

    @Test(expected = IOException::class)
    fun `fetchFeed throws IOException on empty response body`() = runTest {
        // MockResponse with no body at all -- OkHttp will return a null body
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Length", "0")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        repository.fetchFeed(feedUrl)
    }

    @Test
    fun `fetchFeed handles feed with content encoded descriptions`() = runTest {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                 xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>Content Encoded Podcast</title>
                <description>Testing content:encoded</description>
                <itunes:author>Author</itunes:author>
                <item>
                  <title>Rich Description Episode</title>
                  <guid>content-encoded-ep</guid>
                  <content:encoded><![CDATA[<p>This is a <strong>rich</strong> description with HTML.</p>]]></content:encoded>
                  <enclosure url="https://cdn.example.com/rich.mp3" length="10000000" type="audio/mpeg"/>
                  <itunes:duration>30:00</itunes:duration>
                  <pubDate>Fri, 19 Jan 2024 12:00:00 +0000</pubDate>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(rssXml)
                .setHeader("Content-Type", "application/rss+xml")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        val feed = repository.fetchFeed(feedUrl)

        assertEquals(1, feed.episodes.size)
        // content:encoded is used as description fallback when <description> is blank
        assertTrue(
            "Description should contain the content:encoded text",
            feed.episodes[0].description.contains("rich")
        )
    }

    @Test
    fun `fetchFeed uses feedUrl as link when channel link is absent`() = runTest {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>No Link Podcast</title>
                <description>A podcast without a link element</description>
                <itunes:author>Author</itunes:author>
              </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(rssXml)
                .setHeader("Content-Type", "application/rss+xml")
        )

        val feedUrl = mockWebServer.url("/feed.xml").toString()
        val feed = repository.fetchFeed(feedUrl)

        assertEquals("No Link Podcast", feed.title)
        // When <link> is missing, the parser falls back to using feedUrl
        assertEquals(feedUrl, feed.link)
    }
}
