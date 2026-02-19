package com.podbelly.core.playback

import app.cash.turbine.test
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

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
    // Starting a timed countdown
    // -------------------------------------------------------------------------

    @Test
    fun `start sets remainingMillis greater than zero`() = runTest {
        sleepTimer.start(5) // 5 minutes
        // Check immediately after start, before countdown exhausts
        assertTrue(
            "remainingMillis should be > 0 after start",
            sleepTimer.remainingMillis.value > 0L
        )
    }

    @Test
    fun `start sets remainingMillis to correct initial value`() = runTest {
        sleepTimer.start(10) // 10 minutes = 600,000 ms
        // Immediately after start, before any ticks
        assertEquals(600_000L, sleepTimer.remainingMillis.value)
    }

    @Test
    fun `start sets isActive to true`() = runTest {
        sleepTimer.start(5)
        // Advance just enough for the isActive flow to collect the new remainingMillis
        advanceTimeBy(100)

        sleepTimer.isActive.test {
            val active = awaitItem()
            assertTrue("isActive should be true after start", active)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start replaces existing timer`() = runTest {
        sleepTimer.start(10) // 10 minutes
        advanceTimeBy(2_000) // let it tick briefly

        sleepTimer.start(5) // Replace with 5 minutes = 300,000 ms
        // The timer should be reset to the new duration
        assertEquals(300_000L, sleepTimer.remainingMillis.value)
    }

    // -------------------------------------------------------------------------
    // Countdown ticking
    // -------------------------------------------------------------------------

    @Test
    fun `remainingMillis decreases over time`() = runTest {
        sleepTimer.start(1) // 1 minute = 60,000 ms
        val initialRemaining = sleepTimer.remainingMillis.value

        advanceTimeBy(5_000L) // 5 seconds

        assertTrue(
            "remainingMillis should have decreased after time passes",
            sleepTimer.remainingMillis.value < initialRemaining
        )
    }

    @Test
    fun `timer expiration pauses playback`() = runTest {
        sleepTimer.start(1) // 1 minute

        // Advance past the full timer duration
        advanceTimeBy(61_000L)
        advanceUntilIdle()

        verify { playbackController.pause() }
    }

    @Test
    fun `timer expiration sets remainingMillis to zero`() = runTest {
        sleepTimer.start(1) // 1 minute

        // Advance past the full timer duration
        advanceTimeBy(61_000L)
        advanceUntilIdle()

        assertEquals(0L, sleepTimer.remainingMillis.value)
    }

    // -------------------------------------------------------------------------
    // Cancelling
    // -------------------------------------------------------------------------

    @Test
    fun `cancel sets remainingMillis to zero`() = runTest {
        sleepTimer.start(5)
        advanceTimeBy(2_000)

        sleepTimer.cancel()
        advanceTimeBy(100)

        assertEquals(0L, sleepTimer.remainingMillis.value)
    }

    @Test
    fun `cancel sets isActive to false`() = runTest {
        sleepTimer.start(5)
        advanceTimeBy(2_000)

        sleepTimer.cancel()
        advanceTimeBy(100)

        sleepTimer.isActive.test {
            val active = awaitItem()
            assertFalse("isActive should be false after cancel", active)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `cancel stops the countdown`() = runTest {
        sleepTimer.start(1) // 1 minute

        advanceTimeBy(2_000L) // Let it tick for 2 seconds
        sleepTimer.cancel()

        assertEquals(0L, sleepTimer.remainingMillis.value)

        // Advance more time -- remaining should stay at 0
        advanceTimeBy(10_000L)
        assertEquals(0L, sleepTimer.remainingMillis.value)
    }

    @Test
    fun `cancel when no timer is running is a no-op`() = runTest {
        // Should not throw
        sleepTimer.cancel()
        advanceUntilIdle()

        assertEquals(0L, sleepTimer.remainingMillis.value)
    }

    // -------------------------------------------------------------------------
    // End of episode mode
    // -------------------------------------------------------------------------

    @Test
    fun `startEndOfEpisode sets isActive to true`() = runTest {
        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        sleepTimer.isActive.test {
            val active = awaitItem()
            assertTrue("isActive should be true after startEndOfEpisode", active)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startEndOfEpisode sets remainingMillis to Long MAX_VALUE sentinel`() = runTest {
        sleepTimer.startEndOfEpisode()

        assertEquals(Long.MAX_VALUE, sleepTimer.remainingMillis.value)
    }

    @Test
    fun `startEndOfEpisode can be cancelled`() = runTest {
        sleepTimer.startEndOfEpisode()
        advanceUntilIdle()

        sleepTimer.cancel()
        advanceUntilIdle()

        assertEquals(0L, sleepTimer.remainingMillis.value)

        sleepTimer.isActive.test {
            val active = awaitItem()
            assertFalse("isActive should be false after cancelling end-of-episode", active)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startEndOfEpisode replaces existing timed countdown`() = runTest {
        sleepTimer.start(10) // Start a 10-minute timer
        advanceTimeBy(2_000)

        sleepTimer.startEndOfEpisode() // Switch to end-of-episode mode

        assertEquals(Long.MAX_VALUE, sleepTimer.remainingMillis.value)
    }
}
