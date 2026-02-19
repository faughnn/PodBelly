package com.podbelly.core.playback

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for the interaction between [PlaybackController] and [SleepTimer].
 *
 * These tests verify that the SleepTimer correctly observes PlaybackController state
 * and triggers pause when appropriate, without requiring a real MediaController or
 * ExoPlayer instance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playbackController: PlaybackController
    private lateinit var playbackStateFlow: MutableStateFlow<PlaybackState>
    private lateinit var sleepTimer: SleepTimer

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        playbackStateFlow = MutableStateFlow(PlaybackState())
        playbackController = mockk(relaxed = true) {
            every { playbackState } returns playbackStateFlow
        }

        sleepTimer = SleepTimer(playbackController)
    }

    @After
    fun tearDown() {
        sleepTimer.cancel()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Timed countdown -> pause interaction
    // -------------------------------------------------------------------------

    @Test
    fun `timed sleep timer pauses PlaybackController when it expires`() = runTest {
        sleepTimer.start(1) // 1 minute

        // Advance past the full 60 seconds
        advanceTimeBy(61_000L)
        advanceUntilIdle()

        verify(atLeast = 1) { playbackController.pause() }
    }

    @Test
    fun `timed sleep timer does not pause PlaybackController before expiry`() = runTest {
        sleepTimer.start(5) // 5 minutes = 300,000ms

        // Only advance 30 seconds -- well before expiry
        advanceTimeBy(30_000L)

        verify(exactly = 0) { playbackController.pause() }
    }

    @Test
    fun `timed sleep timer resets remaining to zero after pausing`() = runTest {
        sleepTimer.start(1) // 1 minute

        advanceTimeBy(61_000L)
        advanceUntilIdle()

        assertEquals(
            "remainingMillis should be 0 after timer expires and pause is called",
            0L,
            sleepTimer.remainingMillis.value
        )
    }

    // -------------------------------------------------------------------------
    // End-of-episode mode -> PlaybackController state monitoring
    // -------------------------------------------------------------------------

    @Test
    fun `end-of-episode mode monitors playbackState and pauses when episode ends`() = runTest {
        // Simulate an episode that is playing at 50% progress
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 30_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        // Simulate the episode reaching its end (position >= duration - 500ms, not playing)
        playbackStateFlow.value = PlaybackState(
            isPlaying = false,
            currentPosition = 60_000L,
            duration = 60_000L,
        )
        advanceUntilIdle()

        verify(atLeast = 1) { playbackController.pause() }
    }

    @Test
    fun `end-of-episode mode updates remaining millis based on distance to end`() = runTest {
        // Episode is playing with 20 seconds remaining
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 40_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        // remaining should reflect the distance to the end of the episode
        val remaining = sleepTimer.remainingMillis.value
        assertEquals(
            "remainingMillis should reflect distance to episode end",
            20_000L,
            remaining
        )
    }

    @Test
    fun `end-of-episode mode does not pause while episode is still playing`() = runTest {
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 10_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        verify(exactly = 0) { playbackController.pause() }
    }

    @Test
    fun `end-of-episode mode resets remaining to zero after pausing`() = runTest {
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 50_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        // Episode finishes
        playbackStateFlow.value = PlaybackState(
            isPlaying = false,
            currentPosition = 60_000L,
            duration = 60_000L,
        )
        advanceUntilIdle()

        assertEquals(
            "remainingMillis should be 0 after end-of-episode triggers",
            0L,
            sleepTimer.remainingMillis.value
        )
    }

    @Test
    fun `end-of-episode mode considers position within 500ms of duration as ended`() = runTest {
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 30_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        // Position is 59,600ms out of 60,000ms (within 500ms threshold), not playing
        playbackStateFlow.value = PlaybackState(
            isPlaying = false,
            currentPosition = 59_600L,
            duration = 60_000L,
        )
        advanceUntilIdle()

        verify(atLeast = 1) { playbackController.pause() }
    }

    // -------------------------------------------------------------------------
    // Starting a new timer cancels the old one
    // -------------------------------------------------------------------------

    @Test
    fun `starting a timed countdown cancels previous timed countdown`() = runTest {
        sleepTimer.start(10) // 10 minutes
        advanceTimeBy(5_000L) // tick 5 seconds

        val remainingBefore = sleepTimer.remainingMillis.value
        assertTrue(
            "Timer should be running with remaining > 0",
            remainingBefore > 0L
        )

        // Start a new timer -- this should cancel the old one
        sleepTimer.start(2) // 2 minutes = 120,000ms
        assertEquals(
            "Starting a new timer should reset to the new duration",
            120_000L,
            sleepTimer.remainingMillis.value
        )
    }

    @Test
    fun `starting end-of-episode cancels previous timed countdown`() = runTest {
        sleepTimer.start(10) // 10 minutes
        advanceTimeBy(5_000L)

        sleepTimer.startEndOfEpisode()

        assertEquals(
            "Starting end-of-episode should replace timed countdown with sentinel",
            Long.MAX_VALUE,
            sleepTimer.remainingMillis.value
        )
    }

    @Test
    fun `starting timed countdown cancels previous end-of-episode`() = runTest {
        playbackStateFlow.value = PlaybackState(
            isPlaying = true,
            currentPosition = 30_000L,
            duration = 60_000L,
        )

        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        // Now start a timed countdown -- should replace end-of-episode mode
        sleepTimer.start(5) // 5 minutes = 300,000ms
        assertEquals(
            "Timed countdown should replace end-of-episode timer",
            300_000L,
            sleepTimer.remainingMillis.value
        )

        // Simulate episode ending -- pause should NOT be called since end-of-episode was cancelled
        playbackStateFlow.value = PlaybackState(
            isPlaying = false,
            currentPosition = 60_000L,
            duration = 60_000L,
        )
        advanceTimeBy(1_000L)

        // pause should not have been called by end-of-episode trigger
        verify(exactly = 0) { playbackController.pause() }
    }

    @Test
    fun `cancelling timer prevents pause even if countdown would have expired`() = runTest {
        sleepTimer.start(1) // 1 minute

        advanceTimeBy(30_000L) // 30 seconds in
        sleepTimer.cancel()

        // Advance well past when the timer would have expired
        advanceTimeBy(60_000L)
        advanceUntilIdle()

        verify(exactly = 0) { playbackController.pause() }
    }

    @Test
    fun `multiple rapid timer starts only keep the last one`() = runTest {
        sleepTimer.start(10)
        sleepTimer.start(20)
        sleepTimer.start(3) // 3 minutes = 180,000ms

        assertEquals(
            "Only the last timer's duration should be active",
            180_000L,
            sleepTimer.remainingMillis.value
        )
    }
}
