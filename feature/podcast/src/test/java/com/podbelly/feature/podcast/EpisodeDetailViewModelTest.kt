package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity
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
class EpisodeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val queueDao = mockk<QueueDao>(relaxed = true)
    private val playbackController = mockk<PlaybackController>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val episodeFlow = MutableStateFlow<EpisodeEntity?>(null)
    private val queueFlow = MutableStateFlow<List<QueueItemEntity>>(emptyList())

    private val testEpisode = EpisodeEntity(
        id = 5L,
        podcastId = 1L,
        guid = "guid-5",
        title = "Test Episode Title",
        description = "This is a detailed episode description with show notes.",
        audioUrl = "https://example.com/episode5.mp3",
        publicationDate = 1700000000000L,
        durationSeconds = 3600,
        artworkUrl = "https://example.com/episode_art.jpg",
        played = false,
        playbackPosition = 30000L,
        downloadPath = "",
    )

    private val testPodcast = PodcastEntity(
        id = 1L,
        feedUrl = "https://example.com/feed.xml",
        title = "Test Podcast",
        author = "Author",
        description = "Podcast desc",
        artworkUrl = "https://example.com/podcast_art.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { episodeDao.getById(5L) } returns episodeFlow
        every { queueDao.getAll() } returns queueFlow
        coEvery { podcastDao.getByIdOnce(1L) } returns testPodcast
        every { downloadManager.downloadProgress } returns MutableStateFlow(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EpisodeDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("episodeId" to 5L))
        return EpisodeDetailViewModel(
            savedStateHandle = savedStateHandle,
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            queueDao = queueDao,
            playbackController = playbackController,
            downloadManager = downloadManager,
        )
    }

    @Test
    fun `initial state is empty when no episode emitted`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0L, state.episodeId)
            assertEquals("", state.title)
        }
    }

    @Test
    fun `emitting episode populates UI state correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial empty

            episodeFlow.value = testEpisode

            val state = awaitItem()
            assertEquals(5L, state.episodeId)
            assertEquals("Test Episode Title", state.title)
            assertEquals("This is a detailed episode description with show notes.", state.description)
            assertEquals("Test Podcast", state.podcastTitle)
            assertEquals(1L, state.podcastId)
            assertEquals("https://example.com/episode_art.jpg", state.artworkUrl)
            assertEquals(3600, state.durationSeconds)
            assertEquals(30000L, state.playbackPosition)
            assertFalse(state.played)
            assertFalse(state.isDownloaded)
            assertFalse(state.isInQueue)
        }
    }

    @Test
    fun `artwork falls back to podcast when episode has none`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            episodeFlow.value = testEpisode.copy(artworkUrl = "")

            val state = awaitItem()
            assertEquals("https://example.com/podcast_art.jpg", state.artworkUrl)
        }
    }

    @Test
    fun `isInQueue is true when episode is in queue`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial empty

            episodeFlow.value = testEpisode
            queueFlow.value = listOf(
                QueueItemEntity(id = 1L, episodeId = 5L, position = 0, addedAt = 0L)
            )

            // Consume intermediate emissions until we get one with isInQueue true
            var state = awaitItem()
            if (!state.isInQueue && state.episodeId != 0L) {
                state = awaitItem()
            }
            assertTrue(state.isInQueue)
        }
    }

    @Test
    fun `isDownloaded is true when downloadPath is set`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            episodeFlow.value = testEpisode.copy(downloadPath = "/data/episode5.mp3")

            val state = awaitItem()
            assertTrue(state.isDownloaded)
        }
    }

    @Test
    fun `playEpisode calls playbackController with correct params`() = runTest {
        coEvery { episodeDao.getByIdOnce(5L) } returns testEpisode

        val viewModel = createViewModel()
        viewModel.playEpisode()
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 5L,
                audioUrl = "https://example.com/episode5.mp3",
                title = "Test Episode Title",
                podcastTitle = "Test Podcast",
                artworkUrl = "https://example.com/episode_art.jpg",
                startPosition = 30000L,
            )
        }
    }

    @Test
    fun `playEpisode uses downloadPath when available`() = runTest {
        coEvery { episodeDao.getByIdOnce(5L) } returns testEpisode.copy(
            downloadPath = "/local/episode5.mp3"
        )

        val viewModel = createViewModel()
        viewModel.playEpisode()
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 5L,
                audioUrl = "/local/episode5.mp3",
                title = "Test Episode Title",
                podcastTitle = "Test Podcast",
                artworkUrl = "https://example.com/episode_art.jpg",
                startPosition = 30000L,
            )
        }
    }

    @Test
    fun `togglePlayed marks unplayed episode as played`() = runTest {
        coEvery { episodeDao.getByIdOnce(5L) } returns testEpisode.copy(played = false)

        val viewModel = createViewModel()
        viewModel.togglePlayed()
        advanceUntilIdle()

        coVerify { episodeDao.markAsPlayed(5L) }
    }

    @Test
    fun `togglePlayed marks played episode as unplayed`() = runTest {
        coEvery { episodeDao.getByIdOnce(5L) } returns testEpisode.copy(played = true)

        val viewModel = createViewModel()
        viewModel.togglePlayed()
        advanceUntilIdle()

        coVerify { episodeDao.markAsUnplayed(5L) }
    }

    @Test
    fun `addToQueue adds episode at end of queue`() = runTest {
        coEvery { queueDao.getMaxPosition() } returns 2

        val viewModel = createViewModel()
        viewModel.addToQueue()
        advanceUntilIdle()

        coVerify {
            queueDao.addToQueue(match {
                it.episodeId == 5L && it.position == 3
            })
        }
    }

    @Test
    fun `removeFromQueue calls queueDao`() = runTest {
        val viewModel = createViewModel()
        viewModel.removeFromQueue()
        advanceUntilIdle()

        coVerify { queueDao.removeFromQueue(5L) }
    }

    @Test
    fun `downloadEpisode delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.downloadEpisode()
        advanceUntilIdle()

        coVerify { downloadManager.downloadEpisode(5L) }
    }

    @Test
    fun `deleteDownload delegates to downloadManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.deleteDownload()
        advanceUntilIdle()

        coVerify { downloadManager.deleteDownload(5L) }
    }
}
