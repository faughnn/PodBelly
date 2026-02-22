package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.playback.PlaybackController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val playbackController = mockk<PlaybackController>(relaxed = true)
    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val podcastFlow = MutableStateFlow<PodcastEntity?>(null)
    private val episodesFlow = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    private val testPodcast = PodcastEntity(
        id = 1L,
        feedUrl = "https://example.com/feed.xml",
        title = "Test Podcast",
        author = "Author",
        description = "A description",
        artworkUrl = "https://example.com/art.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 1000L,
        episodeCount = 5,
        notifyNewEpisodes = true,
    )

    private val testEpisode = EpisodeEntity(
        id = 10L,
        podcastId = 1L,
        guid = "guid-10",
        title = "Episode One",
        description = "Episode description",
        audioUrl = "https://example.com/ep10.mp3",
        publicationDate = 1700000000000L,
        durationSeconds = 1800,
        artworkUrl = "https://example.com/ep_art.jpg",
        played = false,
        playbackPosition = 5000L,
        downloadPath = "",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { podcastDao.getById(1L) } returns podcastFlow
        every { episodeDao.getByPodcastId(1L) } returns episodesFlow
        every { downloadManager.downloadProgress } returns MutableStateFlow(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PodcastDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("podcastId" to 1L))
        return PodcastDetailViewModel(
            savedStateHandle = savedStateHandle,
            podcastDao = podcastDao,
            episodeDao = episodeDao,
            playbackController = playbackController,
            searchRepository = searchRepository,
            downloadManager = downloadManager,
        )
    }

    // -- UI State tests --

    @Test
    fun `initial state has null podcast and empty episodes`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.podcast)
            assertTrue(state.episodes.isEmpty())
            assertEquals(EpisodeFilter.ALL, state.filter)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `emitting podcast and episodes populates UI state correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(testEpisode)

            val state = awaitItem()
            val podcast = state.podcast!!
            assertEquals(1L, podcast.id)
            assertEquals("Test Podcast", podcast.title)
            assertEquals("Author", podcast.author)
            assertEquals("A description", podcast.description)
            assertEquals("https://example.com/art.jpg", podcast.artworkUrl)
            assertEquals(5, podcast.episodeCount)
            assertTrue(podcast.notifyNewEpisodes)

            assertEquals(1, state.episodes.size)
            val episode = state.episodes[0]
            assertEquals(10L, episode.id)
            assertEquals("Episode One", episode.title)
            assertEquals(1800, episode.durationSeconds)
            assertFalse(episode.played)
            assertFalse(episode.isDownloaded)
            assertEquals(5000L, episode.playbackPosition)
        }
    }

    @Test
    fun `episode isDownloaded is true when downloadPath is non-empty`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(testEpisode.copy(downloadPath = "/data/ep10.mp3"))

            val state = awaitItem()
            assertTrue(state.episodes[0].isDownloaded)
        }
    }

    // -- Filter tests --

    @Test
    fun `setFilter to UNPLAYED filters out played episodes`() = runTest {
        val playedEpisode = testEpisode.copy(id = 11L, guid = "guid-11", title = "Played", played = true)
        val unplayedEpisode = testEpisode.copy(id = 12L, guid = "guid-12", title = "Unplayed", played = false)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(playedEpisode, unplayedEpisode)
            awaitItem() // all episodes shown

            viewModel.setFilter(EpisodeFilter.UNPLAYED)

            val state = awaitItem()
            assertEquals(EpisodeFilter.UNPLAYED, state.filter)
            assertEquals(1, state.episodes.size)
            assertEquals("Unplayed", state.episodes[0].title)
        }
    }

    @Test
    fun `setFilter to DOWNLOADED filters to downloaded episodes only`() = runTest {
        val downloadedEp = testEpisode.copy(id = 13L, guid = "guid-13", title = "Downloaded", downloadPath = "/path")
        val notDownloadedEp = testEpisode.copy(id = 14L, guid = "guid-14", title = "Not Downloaded", downloadPath = "")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(downloadedEp, notDownloadedEp)
            awaitItem()

            viewModel.setFilter(EpisodeFilter.DOWNLOADED)

            val state = awaitItem()
            assertEquals(EpisodeFilter.DOWNLOADED, state.filter)
            assertEquals(1, state.episodes.size)
            assertEquals("Downloaded", state.episodes[0].title)
        }
    }

    @Test
    fun `setFilter to ALL shows all episodes`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(
                testEpisode.copy(id = 15L, guid = "guid-15", played = true),
                testEpisode.copy(id = 16L, guid = "guid-16", played = false),
            )
            awaitItem()

            viewModel.setFilter(EpisodeFilter.UNPLAYED)
            awaitItem()

            viewModel.setFilter(EpisodeFilter.ALL)

            val state = awaitItem()
            assertEquals(EpisodeFilter.ALL, state.filter)
            assertEquals(2, state.episodes.size)
        }
    }

    // -- playEpisode tests --

    @Test
    fun `playEpisode calls playbackController with correct params`() = runTest {
        coEvery { episodeDao.getByIdOnce(10L) } returns testEpisode

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(testEpisode)
            awaitItem() // populated

            viewModel.playEpisode(10L)
            advanceUntilIdle()

            verify {
                playbackController.play(
                    episodeId = 10L,
                    audioUrl = "https://example.com/ep10.mp3",
                    title = "Episode One",
                    podcastTitle = "Test Podcast",
                    artworkUrl = "https://example.com/ep_art.jpg",
                    startPosition = 5000L,
                    podcastId = 1L,
                )
            }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `playEpisode uses downloadPath when available`() = runTest {
        val downloadedEpisode = testEpisode.copy(downloadPath = "/local/ep10.mp3")
        coEvery { episodeDao.getByIdOnce(10L) } returns downloadedEpisode

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(downloadedEpisode)
            awaitItem()

            viewModel.playEpisode(10L)
            advanceUntilIdle()

            verify {
                playbackController.play(
                    episodeId = 10L,
                    audioUrl = "/local/ep10.mp3",
                    title = "Episode One",
                    podcastTitle = "Test Podcast",
                    artworkUrl = "https://example.com/ep_art.jpg",
                    startPosition = 5000L,
                    podcastId = 1L,
                )
            }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `playEpisode falls back to podcast artwork when episode has none`() = runTest {
        val noArtEpisode = testEpisode.copy(artworkUrl = "")
        coEvery { episodeDao.getByIdOnce(10L) } returns noArtEpisode

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            episodesFlow.value = listOf(noArtEpisode)
            awaitItem()

            viewModel.playEpisode(10L)
            advanceUntilIdle()

            verify {
                playbackController.play(
                    episodeId = 10L,
                    audioUrl = "https://example.com/ep10.mp3",
                    title = "Episode One",
                    podcastTitle = "Test Podcast",
                    artworkUrl = "https://example.com/art.jpg",
                    startPosition = 5000L,
                    podcastId = 1L,
                )
            }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `playEpisode does nothing when episode not found`() = runTest {
        coEvery { episodeDao.getByIdOnce(999L) } returns null

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast
            awaitItem()

            viewModel.playEpisode(999L)
            advanceUntilIdle()

            verify(exactly = 0) { playbackController.play(any(), any(), any(), any(), any(), any(), any()) }

            cancelAndConsumeRemainingEvents()
        }
    }

    // -- downloadEpisode tests --

    @Test
    fun `downloadEpisode delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.downloadEpisode(10L)
        advanceUntilIdle()

        coVerify { downloadManager.downloadEpisode(10L) }
    }

    // -- deleteDownload tests --

    @Test
    fun `deleteDownload delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.deleteDownload(10L)
        advanceUntilIdle()

        coVerify { downloadManager.deleteDownload(10L) }
    }

    // -- toggleNotifications tests --

    @Test
    fun `toggleNotifications disables when currently enabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast.copy(notifyNewEpisodes = true)
            awaitItem()

            viewModel.toggleNotifications()
            advanceUntilIdle()

            coVerify { podcastDao.setNotifyNewEpisodes(1L, false) }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleNotifications enables when currently disabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastFlow.value = testPodcast.copy(notifyNewEpisodes = false)
            awaitItem()

            viewModel.toggleNotifications()
            advanceUntilIdle()

            coVerify { podcastDao.setNotifyNewEpisodes(1L, true) }

            cancelAndConsumeRemainingEvents()
        }
    }

    // -- unsubscribe tests --

    @Test
    fun `unsubscribe calls podcastDao unsubscribe`() = runTest {
        val viewModel = createViewModel()
        viewModel.unsubscribe()
        advanceUntilIdle()

        coVerify { podcastDao.unsubscribe(1L) }
    }

    // -- refreshFeed tests --

    @Test
    fun `refreshFeed fetches feed and calls searchRepository`() = runTest {
        val rssFeed = RssFeed(
            title = "Test Podcast",
            description = "Desc",
            author = "Author",
            artworkUrl = "https://example.com/art.jpg",
            link = "https://example.com",
            episodes = emptyList(),
        )
        coEvery { searchRepository.fetchFeed("https://example.com/feed.xml") } returns rssFeed

        podcastFlow.value = testPodcast

        val viewModel = createViewModel()

        // Subscribe to uiState so the combine starts collecting
        val job = backgroundScope.launch { viewModel.uiState.collect {} }

        advanceUntilIdle() // let uiState populate

        viewModel.refreshFeed()
        advanceUntilIdle()

        coVerify { searchRepository.fetchFeed("https://example.com/feed.xml") }
        assertFalse(viewModel.uiState.value.isRefreshing)

        job.cancel()
    }

    @Test
    fun `refreshFeed inserts new episodes and updates podcast`() = runTest {
        val rssEpisode = RssEpisode(
            guid = "new-guid",
            title = "New Episode",
            description = "New desc",
            audioUrl = "https://example.com/new.mp3",
            publishedAt = 1700000000000L,
            duration = 90000L, // 90 seconds in millis
            artworkUrl = null,
            fileSize = 0L,
        )
        val rssFeed = RssFeed(
            title = "Test Podcast",
            description = "Desc",
            author = "Author",
            artworkUrl = "https://example.com/art.jpg",
            link = "https://example.com",
            episodes = listOf(rssEpisode),
        )
        coEvery { searchRepository.fetchFeed("https://example.com/feed.xml") } returns rssFeed
        coEvery { episodeDao.getByGuid("new-guid") } returns null
        coEvery { episodeDao.insertAll(any()) } returns listOf(100L)

        podcastFlow.value = testPodcast

        val viewModel = createViewModel()
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.refreshFeed()
        advanceUntilIdle()

        coVerify {
            episodeDao.insertAll(match { episodes ->
                episodes.size == 1 &&
                    episodes[0].guid == "new-guid" &&
                    episodes[0].title == "New Episode" &&
                    episodes[0].podcastId == 1L &&
                    episodes[0].durationSeconds == 90
            })
        }

        coVerify {
            podcastDao.update(match { podcast ->
                podcast.episodeCount == 6 && podcast.lastRefreshedAt > 0
            })
        }

        job.cancel()
    }

    @Test
    fun `refreshFeed skips existing episodes`() = runTest {
        val rssEpisode = RssEpisode(
            guid = "existing-guid",
            title = "Existing Episode",
            description = "Desc",
            audioUrl = "https://example.com/existing.mp3",
            publishedAt = 1700000000000L,
            duration = 60000L,
            artworkUrl = null,
            fileSize = 0L,
        )
        val rssFeed = RssFeed(
            title = "Test",
            description = "Desc",
            author = "Author",
            artworkUrl = "",
            link = "",
            episodes = listOf(rssEpisode),
        )
        coEvery { searchRepository.fetchFeed(any()) } returns rssFeed
        coEvery { episodeDao.getByGuid("existing-guid") } returns testEpisode

        podcastFlow.value = testPodcast

        val viewModel = createViewModel()
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.refreshFeed()
        advanceUntilIdle()

        // insertAll should not be called since all episodes are existing
        coVerify(exactly = 0) { episodeDao.insertAll(any()) }

        job.cancel()
    }

    @Test
    fun `refreshFeed handles network error gracefully`() = runTest {
        coEvery { searchRepository.fetchFeed(any()) } throws RuntimeException("Network error")

        podcastFlow.value = testPodcast

        val viewModel = createViewModel()

        val job = backgroundScope.launch { viewModel.uiState.collect {} }

        advanceUntilIdle()

        viewModel.refreshFeed()
        advanceUntilIdle()

        // Should not crash; isRefreshing should be back to false
        assertFalse(viewModel.uiState.value.isRefreshing)

        job.cancel()
    }

    // -- downloadProgress tests --

    @Test
    fun `downloadProgress exposes downloadManager progress`() {
        val progressFlow = MutableStateFlow(mapOf(10L to 0.5f))
        every { downloadManager.downloadProgress } returns progressFlow

        val viewModel = createViewModel()
        assertEquals(mapOf(10L to 0.5f), viewModel.downloadProgress.value)
    }
}
