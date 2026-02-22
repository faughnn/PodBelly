package com.podbelly.ui

import app.cash.turbine.test
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.DownloadsSortOrder
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.DownloadErrorWithEpisode
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.playback.PlaybackController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val playbackController = mockk<PlaybackController>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val downloadErrorDao = mockk<DownloadErrorDao>(relaxed = true)

    private val downloadedEpisodesFlow = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())
    private val sortOrderFlow = MutableStateFlow(DownloadsSortOrder.DATE_NEWEST)
    private val errorsFlow = MutableStateFlow<List<DownloadErrorWithEpisode>>(emptyList())

    private val testPodcast = PodcastEntity(
        id = 1L,
        feedUrl = "https://example.com/feed.xml",
        title = "Test Podcast",
        author = "Author",
        description = "Desc",
        artworkUrl = "https://example.com/podcast_art.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
    )

    private val testEpisode = EpisodeEntity(
        id = 10L,
        podcastId = 1L,
        guid = "guid-10",
        title = "Downloaded Episode",
        description = "Desc",
        audioUrl = "https://example.com/ep10.mp3",
        publicationDate = 1700000000000L,
        durationSeconds = 1800,
        artworkUrl = "https://example.com/ep_art.jpg",
        played = false,
        playbackPosition = 30000L,
        downloadPath = "/data/ep10.mp3",
        downloadedAt = 1700000100000L,
        fileSize = 50000000L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { episodeDao.getDownloadedEpisodes() } returns downloadedEpisodesFlow
        every { podcastDao.getAll() } returns podcastsFlow
        every { preferencesManager.downloadsSortOrder } returns sortOrderFlow
        every { downloadErrorDao.getAll() } returns errorsFlow
        every { downloadManager.downloadProgress } returns MutableStateFlow(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DownloadsViewModel {
        return DownloadsViewModel(
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            playbackController = playbackController,
            downloadManager = downloadManager,
            preferencesManager = preferencesManager,
            downloadErrorDao = downloadErrorDao,
        )
    }

    // -- UI State tests --

    @Test
    fun `initial state has empty episodes and default sort order`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.episodes.isEmpty())
            assertEquals(DownloadsSortOrder.DATE_NEWEST, state.sortOrder)
            assertTrue(state.downloadErrors.isEmpty())
        }
    }

    @Test
    fun `emitting episodes and podcasts populates UI state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastsFlow.value = listOf(testPodcast)
            downloadedEpisodesFlow.value = listOf(testEpisode)

            val state = awaitItem()
            assertEquals(1, state.episodes.size)
            val item = state.episodes[0]
            assertEquals(10L, item.episodeId)
            assertEquals("Downloaded Episode", item.title)
            assertEquals("Test Podcast", item.podcastTitle)
            assertEquals("https://example.com/ep_art.jpg", item.artworkUrl)
            assertEquals(1800, item.durationSeconds)
            assertEquals("/data/ep10.mp3", item.downloadPath)
            assertEquals(50000000L, item.fileSize)
            assertEquals(30000L, item.playbackPosition)
        }
    }

    @Test
    fun `artwork falls back to podcast when episode artwork is blank`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(testPodcast)
            downloadedEpisodesFlow.value = listOf(testEpisode.copy(artworkUrl = ""))

            val state = awaitItem()
            assertEquals("https://example.com/podcast_art.jpg", state.episodes[0].artworkUrl)
        }
    }

    @Test
    fun `episodes without matching podcast are excluded`() = runTest {
        val orphanEpisode = testEpisode.copy(id = 99L, guid = "guid-99", podcastId = 999L)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(testPodcast) // only podcast id=1
            downloadedEpisodesFlow.value = listOf(testEpisode, orphanEpisode)

            val state = awaitItem()
            assertEquals(1, state.episodes.size)
            assertEquals(10L, state.episodes[0].episodeId)
        }
    }

    // -- Sort order tests --

    @Test
    fun `DATE_NEWEST sorts by downloadedAt descending`() = runTest {
        val ep1 = testEpisode.copy(id = 1L, guid = "g1", title = "Old", downloadedAt = 1000L)
        val ep2 = testEpisode.copy(id = 2L, guid = "g2", title = "New", downloadedAt = 2000L)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(testPodcast)
            downloadedEpisodesFlow.value = listOf(ep1, ep2)

            val state = awaitItem()
            assertEquals("New", state.episodes[0].title)
            assertEquals("Old", state.episodes[1].title)
        }
    }

    @Test
    fun `DATE_OLDEST sorts by downloadedAt ascending`() = runTest {
        val ep1 = testEpisode.copy(id = 1L, guid = "g1", title = "Old", downloadedAt = 1000L)
        val ep2 = testEpisode.copy(id = 2L, guid = "g2", title = "New", downloadedAt = 2000L)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = DownloadsSortOrder.DATE_OLDEST
            podcastsFlow.value = listOf(testPodcast)
            downloadedEpisodesFlow.value = listOf(ep1, ep2)

            val state = awaitItem()
            assertEquals("Old", state.episodes[0].title)
            assertEquals("New", state.episodes[1].title)
        }
    }

    @Test
    fun `NAME_A_TO_Z sorts alphabetically by title`() = runTest {
        val epB = testEpisode.copy(id = 1L, guid = "g1", title = "Bravo")
        val epA = testEpisode.copy(id = 2L, guid = "g2", title = "Alpha")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = DownloadsSortOrder.NAME_A_TO_Z
            podcastsFlow.value = listOf(testPodcast)
            downloadedEpisodesFlow.value = listOf(epB, epA)

            val state = awaitItem()
            assertEquals("Alpha", state.episodes[0].title)
            assertEquals("Bravo", state.episodes[1].title)
        }
    }

    @Test
    fun `PODCAST_NAME sorts alphabetically by podcast title`() = runTest {
        val podcast2 = testPodcast.copy(id = 2L, feedUrl = "https://other.com/feed", title = "Alpha Show")
        val epFromP1 = testEpisode.copy(id = 1L, guid = "g1", podcastId = 1L) // "Test Podcast"
        val epFromP2 = testEpisode.copy(id = 2L, guid = "g2", podcastId = 2L) // "Alpha Show"

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = DownloadsSortOrder.PODCAST_NAME
            podcastsFlow.value = listOf(testPodcast, podcast2)
            downloadedEpisodesFlow.value = listOf(epFromP1, epFromP2)

            val state = awaitItem()
            assertEquals("Alpha Show", state.episodes[0].podcastTitle)
            assertEquals("Test Podcast", state.episodes[1].podcastTitle)
        }
    }

    // -- Download errors tests --

    @Test
    fun `download errors are included in UI state`() = runTest {
        val error = DownloadErrorWithEpisode(
            id = 1L,
            episodeId = 10L,
            episodeTitle = "Failed Episode",
            errorMessage = "HTTP 500",
            errorCode = 500,
            timestamp = 1700000000000L,
            retryCount = 0,
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            errorsFlow.value = listOf(error)

            val state = awaitItem()
            assertEquals(1, state.downloadErrors.size)
            assertEquals("Failed Episode", state.downloadErrors[0].episodeTitle)
            assertEquals("HTTP 500", state.downloadErrors[0].errorMessage)
        }
    }

    // -- Action tests --

    @Test
    fun `setSortOrder delegates to preferencesManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSortOrder(DownloadsSortOrder.NAME_A_TO_Z)
        advanceUntilIdle()

        coVerify { preferencesManager.setDownloadsSortOrder(DownloadsSortOrder.NAME_A_TO_Z) }
    }

    @Test
    fun `playEpisode calls playbackController with correct params`() = runTest {
        coEvery { episodeDao.getByIdOnce(10L) } returns testEpisode
        coEvery { podcastDao.getByIdOnce(1L) } returns testPodcast

        val viewModel = createViewModel()
        viewModel.playEpisode(10L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 10L,
                audioUrl = "/data/ep10.mp3", // uses downloadPath
                title = "Downloaded Episode",
                podcastTitle = "Test Podcast",
                artworkUrl = "https://example.com/ep_art.jpg",
                startPosition = 30000L,
                podcastId = 1L,
            )
        }
    }

    @Test
    fun `playEpisode uses audioUrl when downloadPath is blank`() = runTest {
        val noDownloadEpisode = testEpisode.copy(downloadPath = "")
        coEvery { episodeDao.getByIdOnce(10L) } returns noDownloadEpisode
        coEvery { podcastDao.getByIdOnce(1L) } returns testPodcast

        val viewModel = createViewModel()
        viewModel.playEpisode(10L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 10L,
                audioUrl = "https://example.com/ep10.mp3",
                title = "Downloaded Episode",
                podcastTitle = "Test Podcast",
                artworkUrl = "https://example.com/ep_art.jpg",
                startPosition = 30000L,
                podcastId = 1L,
            )
        }
    }

    @Test
    fun `playEpisode does nothing when episode not found`() = runTest {
        coEvery { episodeDao.getByIdOnce(999L) } returns null

        val viewModel = createViewModel()
        viewModel.playEpisode(999L)
        advanceUntilIdle()

        verify(exactly = 0) { playbackController.play(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `deleteDownload delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.deleteDownload(10L)
        advanceUntilIdle()

        coVerify { downloadManager.deleteDownload(10L) }
    }

    @Test
    fun `retryDownload delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.retryDownload(10L)
        advanceUntilIdle()

        coVerify { downloadManager.retryDownload(10L) }
    }

    @Test
    fun `clearAllErrors delegates to downloadErrorDao deleteAll`() = runTest {
        val viewModel = createViewModel()
        viewModel.clearAllErrors()
        advanceUntilIdle()

        coVerify { downloadErrorDao.deleteAll() }
    }
}
