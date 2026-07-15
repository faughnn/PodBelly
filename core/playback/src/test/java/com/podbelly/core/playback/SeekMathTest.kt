package com.podbelly.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [computeSkipTarget], the shared rewind / fast-forward target math.
 *
 * These cover the regressions behind the "rapid notification skip crashes / episode
 * reopens completed" bug: a forward skip must never land exactly on the duration.
 */
class SeekMathTest {

    private val duration = 600_000L // 10 minute episode

    @Test
    fun `fast-forward in the middle advances by the offset`() {
        assertEquals(330_000L, computeSkipTarget(300_000L, 30_000L, duration))
    }

    @Test
    fun `rewind in the middle goes back by the offset`() {
        assertEquals(290_000L, computeSkipTarget(300_000L, -10_000L, duration))
    }

    @Test
    fun `rewind past the start parks at zero`() {
        assertEquals(0L, computeSkipTarget(5_000L, -10_000L, duration))
    }

    @Test
    fun `rewind to exactly zero parks at zero`() {
        assertEquals(0L, computeSkipTarget(10_000L, -10_000L, duration))
    }

    @Test
    fun `fast-forward near the end caps a second short of the duration`() {
        // Would otherwise land on 600_000 (the duration) and trigger STATE_ENDED.
        assertEquals(599_000L, computeSkipTarget(590_000L, 30_000L, duration))
    }

    @Test
    fun `fast-forward past the end caps a second short of the duration`() {
        assertEquals(599_000L, computeSkipTarget(599_500L, 30_000L, duration))
    }

    @Test
    fun `fast-forward never returns exactly the duration`() {
        for (pos in 0..duration step 5_000L) {
            val target = computeSkipTarget(pos, 30_000L, duration)
            assert(target < duration) { "target $target should be < duration $duration (pos=$pos)" }
        }
    }

    @Test
    fun `unknown duration returns the raw forward target without clamping`() {
        // C.TIME_UNSET is a large negative value; represent "unknown" as <= 0.
        assertEquals(330_000L, computeSkipTarget(300_000L, 30_000L, 0L))
        assertEquals(330_000L, computeSkipTarget(300_000L, 30_000L, -9_223_372_036_854_775_807L))
    }

    @Test
    fun `unknown duration still clamps a backward skip to zero`() {
        assertEquals(0L, computeSkipTarget(5_000L, -30_000L, 0L))
    }

    @Test
    fun `very short episode caps target at zero rather than going negative`() {
        // duration under 1s: (duration - 1000) coerced to at least 0.
        assertEquals(0L, computeSkipTarget(200L, 30_000L, 500L))
    }

    // -------------------------------------------------------------------------
    // shouldEndForOutro -- per-podcast outro auto-skip end detection
    // -------------------------------------------------------------------------

    @Test
    fun `outro does not fire before the outro window`() {
        // 30s outro on a 10min episode: window starts at 570_000.
        assertFalse(shouldEndForOutro(569_999L, duration, 30, alreadyFired = false))
        assertFalse(shouldEndForOutro(0L, duration, 30, alreadyFired = false))
    }

    @Test
    fun `outro fires exactly at the window boundary`() {
        assertTrue(shouldEndForOutro(570_000L, duration, 30, alreadyFired = false))
    }

    @Test
    fun `outro fires inside the window`() {
        assertTrue(shouldEndForOutro(590_000L, duration, 30, alreadyFired = false))
        assertTrue(shouldEndForOutro(duration, duration, 30, alreadyFired = false))
    }

    @Test
    fun `outro never fires when disabled`() {
        assertFalse(shouldEndForOutro(599_000L, duration, 0, alreadyFired = false))
        assertFalse(shouldEndForOutro(599_000L, duration, -5, alreadyFired = false))
    }

    @Test
    fun `outro never fires when duration is unknown`() {
        assertFalse(shouldEndForOutro(599_000L, 0L, 30, alreadyFired = false))
        assertFalse(shouldEndForOutro(599_000L, -9_223_372_036_854_775_807L, 30, alreadyFired = false))
    }

    @Test
    fun `outro fires at most once per episode playback`() {
        assertFalse(shouldEndForOutro(590_000L, duration, 30, alreadyFired = true))
    }

    @Test
    fun `outro covering the whole episode never fires`() {
        // 60s outro on a 60s episode would end it the moment it starts.
        assertFalse(shouldEndForOutro(0L, 60_000L, 60, alreadyFired = false))
        assertFalse(shouldEndForOutro(30_000L, 60_000L, 60, alreadyFired = false))
        // Longer than the episode: same.
        assertFalse(shouldEndForOutro(30_000L, 60_000L, 90, alreadyFired = false))
    }

    @Test
    fun `outro shorter than the episode fires normally on a short episode`() {
        // 15s outro on a 60s episode: window starts at 45_000.
        assertFalse(shouldEndForOutro(44_999L, 60_000L, 15, alreadyFired = false))
        assertTrue(shouldEndForOutro(45_000L, 60_000L, 15, alreadyFired = false))
    }

    @Test
    fun `seek parked a second short of the end still lands inside the outro window`() {
        // computeSkipTarget parks user seeks at duration - 1000; any outro of >= 2s
        // must still catch that position so the episode completes instead of
        // sitting parked just short of the end.
        val parked = computeSkipTarget(595_000L, 30_000L, duration)
        assertEquals(599_000L, parked)
        assertTrue(shouldEndForOutro(parked, duration, 15, alreadyFired = false))
    }
}
