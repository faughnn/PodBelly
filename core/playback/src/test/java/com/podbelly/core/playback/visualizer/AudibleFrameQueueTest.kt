package com.podbelly.core.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AudibleFrameQueueTest {

    private fun frame(level: Float) = VisualizerFrame(
        bands = FloatArray(VisualizerFrame.BAND_COUNT),
        waveform = FloatArray(VisualizerFrame.WAVE_COUNT),
        level = level,
    )

    @Test
    fun `nothing is audible before the first frame's time`() {
        val queue = AudibleFrameQueue()
        queue.add(1_000_000L, frame(0.1f))

        assertNull(queue.pollAudible(999_999L))
        assertEquals(1, queue.size)
    }

    @Test
    fun `a frame becomes audible exactly at its time`() {
        val queue = AudibleFrameQueue()
        val f = frame(0.1f)
        queue.add(1_000_000L, f)

        assertSame(f, queue.pollAudible(1_000_000L))
        assertEquals(0, queue.size)
    }

    @Test
    fun `polling returns the newest audible frame and drops older ones`() {
        val queue = AudibleFrameQueue()
        val a = frame(0.1f)
        val b = frame(0.2f)
        val c = frame(0.3f)
        queue.add(100L, a)
        queue.add(200L, b)
        queue.add(300L, c)

        assertSame(b, queue.pollAudible(250L))
        assertEquals(1, queue.size)
        assertNull(queue.pollAudible(250L))
        assertSame(c, queue.pollAudible(300L))
        assertEquals(0, queue.size)
    }

    @Test
    fun `clear discards everything`() {
        val queue = AudibleFrameQueue()
        queue.add(1L, frame(0.1f))
        queue.add(2L, frame(0.2f))

        queue.clear()

        assertEquals(0, queue.size)
        assertNull(queue.pollAudible(Long.MAX_VALUE))
    }

    @Test
    fun `capacity evicts the oldest frames first`() {
        val queue = AudibleFrameQueue(capacity = 2)
        val a = frame(0.1f)
        val b = frame(0.2f)
        val c = frame(0.3f)
        queue.add(1L, a)
        queue.add(2L, b)
        queue.add(3L, c)

        assertEquals(2, queue.size)
        // `a` was evicted; polling up to time 2 yields `b`.
        assertSame(b, queue.pollAudible(2L))
        assertSame(c, queue.pollAudible(3L))
    }
}
