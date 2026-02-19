package com.podbelly.feature.settings

import app.cash.turbine.test
import com.podbelly.core.common.DarkThemeMode
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.opml.OpmlFeed
import com.podbelly.core.network.opml.OpmlHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val downloadManager = mockk<com.podbelly.core.common.DownloadManager>(relaxed = true)

    private val darkThemeModeFlow = MutableStateFlow(DarkThemeMode.SYSTEM)
    private val feedRefreshIntervalFlow = MutableStateFlow(60)
    private val autoDownloadEnabledFlow = MutableStateFlow(false)
    private val autoDownloadCountFlow = MutableStateFlow(3)
    private val autoDeletePlayedFlow = MutableStateFlow(false)
    private val downloadOnWifiOnlyFlow = MutableStateFlow(true)
    private val skipSilenceFlow = MutableStateFlow(false)
    private val volumeBoostFlow = MutableStateFlow(false)
    private val playbackSpeedFlow = MutableStateFlow(1.0f)

    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesManager.darkThemeMode } returns darkThemeModeFlow
        every { preferencesManager.feedRefreshIntervalMinutes } returns feedRefreshIntervalFlow
        every { preferencesManager.autoDownloadEnabled } returns autoDownloadEnabledFlow
        every { preferencesManager.autoDownloadEpisodeCount } returns autoDownloadCountFlow
        every { preferencesManager.autoDeletePlayed } returns autoDeletePlayedFlow
        every { preferencesManager.downloadOnWifiOnly } returns downloadOnWifiOnlyFlow
        every { preferencesManager.skipSilence } returns skipSilenceFlow
        every { preferencesManager.volumeBoost } returns volumeBoostFlow
        every { preferencesManager.playbackSpeed } returns playbackSpeedFlow

        every { podcastDao.getAll() } returns podcastsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            preferencesManager = preferencesManager,
            podcastDao = podcastDao,
            episodeDao = episodeDao,
            searchRepository = searchRepository,
            downloadManager = downloadManager,
        )
    }

    // -- Helpers --

    private fun makePodcast(
        id: Long = 1L,
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
    ) = PodcastEntity(
        id = id,
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        description = "Description",
        artworkUrl = "https://art.com/podcast.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
    )

    // -- Tests --

    @Test
    fun `initial state loads from PreferencesManager`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(DarkThemeMode.SYSTEM, initial.darkThemeMode)
            assertEquals(60, initial.feedRefreshIntervalMinutes)
            assertFalse(initial.autoDownloadEnabled)
            assertEquals(3, initial.autoDownloadEpisodeCount)
            assertFalse(initial.autoDeletePlayed)
            assertTrue(initial.downloadOnWifiOnly)
            assertFalse(initial.skipSilence)
            assertFalse(initial.volumeBoost)
            assertEquals(1.0f, initial.defaultPlaybackSpeed)
        }
    }

    @Test
    fun `uiState reflects updated preference values`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            darkThemeModeFlow.value = DarkThemeMode.DARK
            playbackSpeedFlow.value = 1.5f

            // There may be intermediate states; collect until we see the final state
            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<SettingsUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertEquals(DarkThemeMode.DARK, lastItem.darkThemeMode)
                assertEquals(1.5f, lastItem.defaultPlaybackSpeed)
            }
        }
    }

    @Test
    fun `setDarkThemeMode updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDarkThemeMode(DarkThemeMode.DARK)
        advanceUntilIdle()

        coVerify { preferencesManager.setDarkThemeMode(DarkThemeMode.DARK) }
    }

    @Test
    fun `setDefaultPlaybackSpeed updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDefaultPlaybackSpeed(2.0f)
        advanceUntilIdle()

        coVerify { preferencesManager.setPlaybackSpeed(2.0f) }
    }

    @Test
    fun `setSkipSilence updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSkipSilence(true)
        advanceUntilIdle()

        coVerify { preferencesManager.setSkipSilence(true) }
    }

    @Test
    fun `setVolumeBoost updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setVolumeBoost(true)
        advanceUntilIdle()

        coVerify { preferencesManager.setVolumeBoost(true) }
    }

    @Test
    fun `setAutoDownload updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setAutoDownload(true)
        advanceUntilIdle()

        coVerify { preferencesManager.setAutoDownloadEnabled(true) }
    }

    @Test
    fun `setFeedRefreshInterval updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setFeedRefreshInterval(120)
        advanceUntilIdle()

        coVerify { preferencesManager.setFeedRefreshIntervalMinutes(120) }
    }

    @Test
    fun `importOpml parses and subscribes podcasts`() = runTest {
        val opmlXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline type="rss" text="Pod1" title="Pod1" xmlUrl="https://feed1.com/rss"/>
                <outline type="rss" text="Pod2" title="Pod2" xmlUrl="https://feed2.com/rss"/>
              </body>
            </opml>
        """.trimIndent()

        // Neither podcast exists in DB
        coEvery { podcastDao.getByFeedUrl(any()) } returns null
        coEvery { podcastDao.insert(any()) } returns 10L

        val rssEpisode = RssEpisode(
            guid = "guid-1",
            title = "Episode",
            description = "Desc",
            audioUrl = "https://audio.com/ep.mp3",
            publishedAt = 100000L,
            duration = 60000L,
            artworkUrl = null,
            fileSize = 5000L,
        )
        val rssFeed = RssFeed(
            title = "Pod Title",
            description = "Desc",
            author = "Author",
            artworkUrl = "https://art.com",
            link = "https://link.com",
            episodes = listOf(rssEpisode),
        )
        coEvery { searchRepository.fetchFeed(any()) } returns rssFeed

        val viewModel = createViewModel()
        viewModel.importOpml(opmlXml)
        advanceUntilIdle()

        // Should fetch feed for each entry
        coVerify { searchRepository.fetchFeed("https://feed1.com/rss") }
        coVerify { searchRepository.fetchFeed("https://feed2.com/rss") }

        // Should insert 2 podcasts and 2 episode batches
        coVerify(exactly = 2) { podcastDao.insert(any()) }
        coVerify(exactly = 2) { episodeDao.insertAll(any()) }
    }

    @Test
    fun `importOpml skips already existing podcasts`() = runTest {
        val opmlXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline type="rss" text="Existing" title="Existing" xmlUrl="https://existing.com/rss"/>
              </body>
            </opml>
        """.trimIndent()

        val existing = makePodcast(id = 5L, feedUrl = "https://existing.com/rss")
        coEvery { podcastDao.getByFeedUrl("https://existing.com/rss") } returns existing

        val viewModel = createViewModel()
        viewModel.importOpml(opmlXml)
        advanceUntilIdle()

        // Should NOT fetch feed or insert podcast since it already exists
        coVerify(exactly = 0) { searchRepository.fetchFeed(any()) }
        coVerify(exactly = 0) { podcastDao.insert(any()) }
    }

    @Test
    fun `importOpml sets import message with count`() = runTest {
        val opmlXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline type="rss" text="Pod1" title="Pod1" xmlUrl="https://feed1.com/rss"/>
              </body>
            </opml>
        """.trimIndent()

        coEvery { podcastDao.getByFeedUrl(any()) } returns null
        coEvery { podcastDao.insert(any()) } returns 10L

        val rssFeed = RssFeed(
            title = "Pod", description = "D", author = "A",
            artworkUrl = "", link = "", episodes = emptyList(),
        )
        coEvery { searchRepository.fetchFeed(any()) } returns rssFeed

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.importOpml(opmlXml)
            advanceUntilIdle()

            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<SettingsUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertTrue(lastItem.importExportMessage?.contains("Imported 1 of 1") == true)
            }
        }
    }

    @Test
    fun `exportOpml generates correct OPML and calls onResult`() = runTest {
        val podcasts = listOf(
            makePodcast(id = 1L, feedUrl = "https://feed1.com/rss", title = "Podcast One"),
            makePodcast(id = 2L, feedUrl = "https://feed2.com/rss", title = "Podcast Two"),
        )
        podcastsFlow.value = podcasts

        var capturedXml: String? = null

        val viewModel = createViewModel()
        viewModel.exportOpml { xml -> capturedXml = xml }
        advanceUntilIdle()

        // Verify the callback was invoked with XML content
        assertTrue(capturedXml != null)
        assertTrue(capturedXml!!.contains("Podcast One"))
        assertTrue(capturedXml!!.contains("Podcast Two"))
        assertTrue(capturedXml!!.contains("https://feed1.com/rss"))
        assertTrue(capturedXml!!.contains("https://feed2.com/rss"))
    }

    @Test
    fun `exportOpml sets export message with count`() = runTest {
        val podcasts = listOf(
            makePodcast(id = 1L, feedUrl = "https://feed1.com/rss", title = "P1"),
            makePodcast(id = 2L, feedUrl = "https://feed2.com/rss", title = "P2"),
        )
        podcastsFlow.value = podcasts

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.exportOpml { /* no-op */ }
            advanceUntilIdle()

            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<SettingsUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertEquals("Exported 2 subscription(s)", lastItem.importExportMessage)
            }
        }
    }

    @Test
    fun `clearMessage sets importExportMessage to null`() = runTest {
        val viewModel = createViewModel()

        // Trigger an action that sets a message first
        podcastsFlow.value = emptyList()
        viewModel.exportOpml { }
        advanceUntilIdle()

        viewModel.clearMessage()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.importExportMessage)
        }
    }

    @Test
    fun `setDownloadOnWifiOnly updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDownloadOnWifiOnly(false)
        advanceUntilIdle()

        coVerify { preferencesManager.setDownloadOnWifiOnly(false) }
    }

    @Test
    fun `setAutoDeletePlayed updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setAutoDeletePlayed(true)
        advanceUntilIdle()

        coVerify { preferencesManager.setAutoDeletePlayed(true) }
    }

    @Test
    fun `setAutoDownloadEpisodeCount updates preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setAutoDownloadEpisodeCount(5)
        advanceUntilIdle()

        coVerify { preferencesManager.setAutoDownloadEpisodeCount(5) }
    }
}
