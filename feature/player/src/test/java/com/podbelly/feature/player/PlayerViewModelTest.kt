package com.podbelly.feature.player

import app.cash.turbine.test
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
import com.podbelly.core.playback.SleepTimer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val playbackController = mockk<PlaybackController>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val queueDao = mockk<QueueDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val sleepTimer = mockk<SleepTimer>(relaxed = true)

    private val playbackStateFlow = MutableStateFlow(PlaybackState())
    private val sleepTimerRemainingFlow = MutableStateFlow(0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playbackController.playbackState } returns playbackStateFlow
        every { sleepTimer.remainingMillis } returns sleepTimerRemainingFlow

        // PreferencesManager flows for init block
        every { preferencesManager.playbackSpeed } returns flowOf(1.0f)
        every { preferencesManager.skipSilence } returns flowOf(false)
        every { preferencesManager.volumeBoost } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlayerViewModel {
        return PlayerViewModel(
            playbackController = playbackController,
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            queueDao = queueDao,
            preferencesManager = preferencesManager,
            sleepTimer = sleepTimer,
        )
    }

    // -- Tests --

    @Test
    fun `initial UI state has default PlaybackState`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(PlaybackState(), initial.playbackState)
            assertEquals(0L, initial.sleepTimerRemaining)
            assertFalse(initial.isSleepTimerActive)
            assertEquals(1.0f, initial.playbackSpeed)
            assertFalse(initial.skipSilence)
            assertFalse(initial.volumeBoost)
            assertFalse(initial.showSleepTimerPicker)
            assertFalse(initial.showSpeedPicker)
        }
    }

    @Test
    fun `togglePlayPause pauses when currently playing`() = runTest {
        playbackStateFlow.value = PlaybackState(isPlaying = true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.togglePlayPause()

        verify { playbackController.pause() }
        verify(exactly = 0) { playbackController.resume() }
    }

    @Test
    fun `togglePlayPause resumes when not playing`() = runTest {
        playbackStateFlow.value = PlaybackState(isPlaying = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.togglePlayPause()

        verify { playbackController.resume() }
        verify(exactly = 0) { playbackController.pause() }
    }

    @Test
    fun `seekTo delegates to playbackController`() = runTest {
        val viewModel = createViewModel()
        viewModel.seekTo(30000L)

        verify { playbackController.seekTo(30000L) }
    }

    @Test
    fun `skipForward delegates to playbackController with 30 seconds`() = runTest {
        val viewModel = createViewModel()
        viewModel.skipForward()

        verify { playbackController.skipForward(seconds = 30) }
    }

    @Test
    fun `skipBack delegates to playbackController with 10 seconds`() = runTest {
        val viewModel = createViewModel()
        viewModel.skipBack()

        verify { playbackController.skipBack(seconds = 10) }
    }

    @Test
    fun `setPlaybackSpeed updates controller and preferences`() = runTest {
        val viewModel = createViewModel()
        viewModel.setPlaybackSpeed(1.5f)
        advanceUntilIdle()

        verify { playbackController.setPlaybackSpeed(1.5f) }
        coVerify { preferencesManager.setPlaybackSpeed(1.5f) }
    }

    @Test
    fun `setPlaybackSpeed hides speed picker`() = runTest {
        val viewModel = createViewModel()

        viewModel.showSpeedPicker()
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(2.0f)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showSpeedPicker)
        }
    }

    @Test
    fun `startSleepTimer delegates to sleepTimer`() = runTest {
        val viewModel = createViewModel()
        viewModel.startSleepTimer(30)
        advanceUntilIdle()

        verify { sleepTimer.start(30) }
        coVerify { preferencesManager.setSleepTimerMinutes(30) }
    }

    @Test
    fun `startSleepTimer hides sleep timer picker`() = runTest {
        val viewModel = createViewModel()

        viewModel.showSleepTimerPicker()
        advanceUntilIdle()

        viewModel.startSleepTimer(15)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showSleepTimerPicker)
        }
    }

    @Test
    fun `startSleepTimerEndOfEpisode delegates to sleepTimer`() = runTest {
        val viewModel = createViewModel()

        viewModel.showSleepTimerPicker()
        advanceUntilIdle()

        viewModel.startSleepTimerEndOfEpisode()
        advanceUntilIdle()

        verify { sleepTimer.startEndOfEpisode() }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showSleepTimerPicker)
        }
    }

    @Test
    fun `cancelSleepTimer delegates to sleepTimer`() = runTest {
        val viewModel = createViewModel()
        viewModel.cancelSleepTimer()
        advanceUntilIdle()

        verify { sleepTimer.cancel() }
        coVerify { preferencesManager.setSleepTimerMinutes(0) }
    }

    @Test
    fun `uiState reflects sleep timer active when remainingMillis is positive`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            sleepTimerRemainingFlow.value = 60000L

            val state = awaitItem()
            assertEquals(60000L, state.sleepTimerRemaining)
            assertTrue(state.isSleepTimerActive)
        }
    }

    @Test
    fun `uiState reflects playback state changes`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            playbackStateFlow.value = PlaybackState(
                episodeId = 5L,
                isPlaying = true,
                currentPosition = 10000L,
                duration = 60000L,
                playbackSpeed = 1.5f,
            )

            val state = awaitItem()
            assertEquals(5L, state.playbackState.episodeId)
            assertTrue(state.playbackState.isPlaying)
            assertEquals(10000L, state.playbackState.currentPosition)
            assertEquals(60000L, state.playbackState.duration)
            assertEquals(1.5f, state.playbackSpeed)
        }
    }

    @Test
    fun `toggleSkipSilence toggles and persists the value`() = runTest {
        // Start with skipSilence = false
        playbackStateFlow.value = PlaybackState(skipSilence = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSkipSilence()
        advanceUntilIdle()

        verify { playbackController.setSkipSilence(true) }
        coVerify { preferencesManager.setSkipSilence(true) }
    }

    @Test
    fun `toggleVolumeBoost toggles and persists the value`() = runTest {
        playbackStateFlow.value = PlaybackState(volumeBoost = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleVolumeBoost()
        advanceUntilIdle()

        verify { playbackController.setVolumeBoost(true) }
        coVerify { preferencesManager.setVolumeBoost(true) }
    }

    @Test
    fun `savePosition saves current position for current episode`() = runTest {
        playbackStateFlow.value = PlaybackState(episodeId = 99L, currentPosition = 55000L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.savePosition()
        advanceUntilIdle()

        coVerify { episodeDao.updatePlaybackPosition(99L, 55000L) }
    }

    @Test
    fun `showSpeedPicker and hideSpeedPicker toggle the picker state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertFalse(initial.showSpeedPicker)

            viewModel.showSpeedPicker()
            val shown = awaitItem()
            assertTrue(shown.showSpeedPicker)

            viewModel.hideSpeedPicker()
            val hidden = awaitItem()
            assertFalse(hidden.showSpeedPicker)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showSleepTimerPicker and hideSleepTimerPicker toggle the picker state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertFalse(initial.showSleepTimerPicker)

            viewModel.showSleepTimerPicker()
            val shown = awaitItem()
            assertTrue(shown.showSleepTimerPicker)

            viewModel.hideSleepTimerPicker()
            val hidden = awaitItem()
            assertFalse(hidden.showSleepTimerPicker)

            cancelAndConsumeRemainingEvents()
        }
    }

    // -- Chapter navigation tests --

    @Test
    fun `seekToNextChapter delegates to playbackController`() = runTest {
        val viewModel = createViewModel()
        viewModel.seekToNextChapter()

        verify { playbackController.seekToNextChapter() }
    }

    @Test
    fun `seekToPreviousChapter delegates to playbackController`() = runTest {
        val viewModel = createViewModel()
        viewModel.seekToPreviousChapter()

        verify { playbackController.seekToPreviousChapter() }
    }

    @Test
    fun `seekToChapter delegates to playbackController with correct index`() = runTest {
        val viewModel = createViewModel()
        viewModel.seekToChapter(3)

        verify { playbackController.seekToChapter(3) }
    }

    @Test
    fun `showChaptersList and hideChaptersList toggle the chapters state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.showChaptersList)

            viewModel.showChaptersList()
            val shown = awaitItem()
            assertTrue(shown.showChaptersList)

            viewModel.hideChaptersList()
            val hidden = awaitItem()
            assertFalse(hidden.showChaptersList)

            cancelAndConsumeRemainingEvents()
        }
    }

    // -- Per-podcast speed tests --

    @Test
    fun `setPlaybackSpeed saves to podcastDao when podcast is playing`() = runTest {
        playbackStateFlow.value = PlaybackState(podcastId = 42L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(2.0f)
        advanceUntilIdle()

        verify { playbackController.setPlaybackSpeed(2.0f) }
        coVerify { podcastDao.updatePlaybackSpeed(42L, 2.0f) }
        coVerify(exactly = 0) { preferencesManager.setPlaybackSpeed(any()) }
    }

    // -- Save position edge case --

    @Test
    fun `savePosition does nothing when no episode is playing`() = runTest {
        playbackStateFlow.value = PlaybackState(episodeId = 0L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.savePosition()
        advanceUntilIdle()

        coVerify(exactly = 0) { episodeDao.updatePlaybackPosition(any(), any()) }
    }
}
