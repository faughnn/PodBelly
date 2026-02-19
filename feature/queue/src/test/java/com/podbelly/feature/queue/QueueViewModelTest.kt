package com.podbelly.feature.queue

import app.cash.turbine.test
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.dao.QueueEpisode
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val queueDao = mockk<QueueDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val playbackController = mockk<PlaybackController>(relaxed = true)

    private val queueFlow = MutableStateFlow<List<QueueEpisode>>(emptyList())
    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())
    private val playbackStateFlow = MutableStateFlow(PlaybackState())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { queueDao.getQueueWithEpisodes() } returns queueFlow
        every { podcastDao.getAll() } returns podcastsFlow
        every { playbackController.playbackState } returns playbackStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): QueueViewModel {
        return QueueViewModel(
            queueDao = queueDao,
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            playbackController = playbackController,
        )
    }

    // -- Helpers --

    private fun makePodcast(
        id: Long = 1L,
        title: String = "Test Podcast",
        artworkUrl: String = "https://art.com/podcast.jpg",
    ) = PodcastEntity(
        id = id,
        feedUrl = "https://example.com/feed.xml",
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
        title: String = "Episode Title",
        artworkUrl: String = "https://art.com/ep.jpg",
        downloadPath: String = "",
        playbackPosition: Long = 0L,
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = "guid-$id",
        title = title,
        description = "Desc",
        audioUrl = "https://audio.com/ep$id.mp3",
        publicationDate = 1000L * id,
        durationSeconds = 1800,
        artworkUrl = artworkUrl,
        downloadPath = downloadPath,
        playbackPosition = playbackPosition,
    )

    private fun makeQueueItem(
        id: Long = 1L,
        episodeId: Long = 1L,
        position: Int = 0,
    ) = QueueItemEntity(
        id = id,
        episodeId = episodeId,
        position = position,
        addedAt = System.currentTimeMillis(),
    )

    // -- Tests --

    @Test
    fun `initial state has empty queue`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.queueItems.isEmpty())
            assertTrue(initial.isEmpty)
            assertNull(initial.nowPlayingEpisodeId)
        }
    }

    @Test
    fun `queue items are mapped correctly from QueueEpisode to QueueItemUi`() = runTest {
        val podcast = makePodcast(id = 1L, title = "My Podcast", artworkUrl = "https://art.com/p.jpg")
        val episode = makeEpisode(id = 10L, podcastId = 1L, title = "Test Episode", artworkUrl = "https://art.com/e.jpg")
        val queueItem = makeQueueItem(id = 1L, episodeId = 10L, position = 0)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial empty state
            awaitItem()

            // Emit queue and podcast data
            podcastsFlow.value = listOf(podcast)
            queueFlow.value = listOf(
                QueueEpisode(queueItem = queueItem, episode = episode)
            )

            val state = awaitItem()
            assertEquals(1, state.queueItems.size)
            assertFalse(state.isEmpty)

            val item = state.queueItems[0]
            assertEquals(1L, item.queueId)
            assertEquals(10L, item.episodeId)
            assertEquals("Test Episode", item.episodeTitle)
            assertEquals("My Podcast", item.podcastTitle)
            // Episode has its own artwork, so it should use that
            assertEquals("https://art.com/e.jpg", item.artworkUrl)
            assertEquals(1800, item.durationSeconds)
            assertEquals(0, item.position)
        }
    }

    @Test
    fun `queue item falls back to podcast artwork when episode artwork is blank`() = runTest {
        val podcast = makePodcast(id = 1L, title = "Podcast", artworkUrl = "https://art.com/fallback.jpg")
        val episode = makeEpisode(id = 20L, podcastId = 1L, artworkUrl = "") // blank artwork
        val queueItem = makeQueueItem(id = 2L, episodeId = 20L, position = 0)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastsFlow.value = listOf(podcast)
            queueFlow.value = listOf(
                QueueEpisode(queueItem = queueItem, episode = episode)
            )

            val state = awaitItem()
            assertEquals("https://art.com/fallback.jpg", state.queueItems[0].artworkUrl)
        }
    }

    @Test
    fun `playItem calls playbackController play with correct params`() = runTest {
        val podcast = makePodcast(id = 1L, title = "Show", artworkUrl = "https://art.com/p.jpg")
        val episode = makeEpisode(
            id = 15L,
            podcastId = 1L,
            title = "Episode to Play",
            artworkUrl = "https://art.com/ep.jpg",
            downloadPath = "",
        )

        coEvery { podcastDao.getByIdOnce(1L) } returns podcast
        coEvery { episodeDao.getByIdOnce(15L) } returns episode

        val viewModel = createViewModel()
        viewModel.playItem(15L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 15L,
                audioUrl = episode.audioUrl,
                title = "Episode to Play",
                podcastTitle = "Show",
                artworkUrl = "https://art.com/ep.jpg",
                startPosition = 0L,
            )
        }
    }

    @Test
    fun `playItem resumes from saved position via startPosition`() = runTest {
        val podcast = makePodcast(id = 1L)
        val episode = makeEpisode(id = 25L, podcastId = 1L, playbackPosition = 45000L)

        coEvery { podcastDao.getByIdOnce(1L) } returns podcast
        coEvery { episodeDao.getByIdOnce(25L) } returns episode

        val viewModel = createViewModel()
        viewModel.playItem(25L)
        advanceUntilIdle()

        verify {
            playbackController.play(
                episodeId = 25L,
                audioUrl = episode.audioUrl,
                title = episode.title,
                podcastTitle = podcast.title,
                artworkUrl = episode.artworkUrl,
                startPosition = 45000L,
            )
        }
        // seekTo should NOT be called separately -- startPosition is passed directly
        verify(exactly = 0) { playbackController.seekTo(any()) }
    }

    @Test
    fun `removeItem calls queueDao removeFromQueue`() = runTest {
        val viewModel = createViewModel()
        viewModel.removeItem(99L)
        advanceUntilIdle()

        coVerify { queueDao.removeFromQueue(99L) }
    }

    @Test
    fun `clearQueue calls queueDao clearQueue`() = runTest {
        val viewModel = createViewModel()
        viewModel.clearQueue()
        advanceUntilIdle()

        coVerify { queueDao.clearQueue() }
    }

    @Test
    fun `nowPlayingEpisodeId is set when playback state has non-zero episodeId`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            playbackStateFlow.value = PlaybackState(episodeId = 42L, isPlaying = true)

            val state = awaitItem()
            assertEquals(42L, state.nowPlayingEpisodeId)
        }
    }

    @Test
    fun `nowPlayingEpisodeId is null when playback state episodeId is zero`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.nowPlayingEpisodeId)
        }
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}
