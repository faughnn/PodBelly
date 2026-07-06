package com.podbelly.core.playback

import org.junit.Assert.assertEquals
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
}
