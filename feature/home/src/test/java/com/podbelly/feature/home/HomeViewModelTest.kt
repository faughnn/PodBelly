package com.podbelly.feature.home

import app.cash.turbine.test
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.common.DownloadManager
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val playbackController = mockk<PlaybackController>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val episodesFlow = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { episodeDao.getRecentEpisodes(50) } returns episodesFlow
        every { podcastDao.getAll() } returns podcastsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            playbackController = playbackController,
            downloadManager = downloadManager,
        )
    }

    // -- Helpers --

    private fun makePodcast(
        id: Long = 1L,
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
        artworkUrl: String = "https://example.com/art.jpg",
    ) = PodcastEntity(
        id = id,
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        description = "Description",
        artworkUrl = artworkUrl,
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
    )

    private fun makeEpisode(
        id: Long = 1L,
        podcastId: Long = 1L,
        title: String = "Episode 1",
        artworkUrl: String = "",
        downloadPath: String = "",
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = "guid-$id",
        title = title,
        description = "Description",
        audioUrl = "https://example.com/episode$id.mp3",
        publicationDate = 1000L * id,
        durationSeconds = 3600,
        artworkUrl = artworkUrl,
        downloadPath = downloadPath,
    )

    // -- Tests --

    @Test
    fun `initial UI state has empty recent episodes and isEmpty is false`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(emptyList<HomeEpisodeItem>(), initial.recentEpisodes)
            assertFalse(initial.isEmpty)
        }
    }

    @Test
    fun `when episodes and podcasts are emitted, UI state contains correct HomeEpisodeItems`() = runTest {
        val podcast = makePodcast(id = 1L, title = "My Show", artworkUrl = "https://art.com/show.jpg")
        val episode = makeEpisode(
            id = 10L,
            podcastId = 1L,
            title = "Great Episode",
            artworkUrl = "", // blank so it falls back to podcast artwork
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Consume the initial empty state
            awaitItem()

            // Emit data
            podcastsFlow.value = listOf(podcast)
            episodesFlow.value = listOf(episode)

            val state = awaitItem()
            assertEquals(1, state.recentEpisodes.size)

            val item = state.recentEpisodes[0]
            assertEquals(10L, item.episodeId)
            assertEquals("Great Episode", item.title)
            assertEquals("My Show", item.podcastTitle)
            // Artwork falls back to podcast artwork because episode artworkUrl is blank
            assertEquals("https://art.com/show.jpg", item.artworkUrl)
            assertEquals(episode.publicationDate, item.publicationDate)
            assertEquals(episode.durationSeconds, item.durationSeconds)
            assertFalse(item.played)
        }
    }

    @Test
    fun `when no podcasts exist, isEmpty is true`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            podcastsFlow.value = emptyList()
            episodesFlow.value = emptyList()

            val state = awaitItem()
            assertTrue(state.isEmpty)
            assertTrue(state.recentEpisodes.isEmpty())
        }
    }

    @Test
    fun `playEpisode calls playbackController play with correct params`() = runTest {
        val podcast = makePodcast(id = 1L, title = "Show Title", artworkUrl = "https://art.com/podcast.jpg")
        val episode = makeEpisode(
            id = 5L,
            podcastId = 1L,
            title = "Ep Title",
            artworkUrl = "https://art.com/episode.jpg",
            downloadPath = "",
        )

        coEvery { podcastDao.getByIdOnce(1L) } returns podcast
        coEvery { episodeDao.getByIdOnce(5L) } returns episode

        val viewModel = createViewModel()
        viewModel.playEpisode(5L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 5L,
                audioUrl = episode.audioUrl, // downloadPath is blank, falls back to audioUrl
                title = "Ep Title",
                podcastTitle = "Show Title",
                artworkUrl = "https://art.com/episode.jpg",
                startPosition = 0L,
                podcastId = 1L,
            )
        }
    }

    @Test
    fun `downloadEpisode delegates to downloadManager and registers job`() = runTest {
        val viewModel = createViewModel()
        viewModel.downloadEpisode(5L)
        advanceUntilIdle()

        coVerify { downloadManager.downloadEpisode(5L) }
        verify { downloadManager.registerDownloadJob(5L, any()) }
    }

    @Test
    fun `deleteDownload delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.deleteDownload(5L)
        advanceUntilIdle()

        coVerify { downloadManager.deleteDownload(5L) }
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
    fun `playEpisode uses downloadPath when available`() = runTest {
        val podcast = makePodcast(id = 1L, title = "Show", artworkUrl = "https://art.com/p.jpg")
        val episode = makeEpisode(
            id = 7L,
            podcastId = 1L,
            title = "Downloaded Ep",
            artworkUrl = "",
            downloadPath = "/data/local/episode7.mp3",
        )

        coEvery { podcastDao.getByIdOnce(1L) } returns podcast
        coEvery { episodeDao.getByIdOnce(7L) } returns episode

        val viewModel = createViewModel()
        viewModel.playEpisode(7L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 7L,
                audioUrl = "/data/local/episode7.mp3",
                title = "Downloaded Ep",
                podcastTitle = "Show",
                artworkUrl = "https://art.com/p.jpg", // falls back to podcast artwork
                startPosition = 0L,
                podcastId = 1L,
            )
        }
    }
}
