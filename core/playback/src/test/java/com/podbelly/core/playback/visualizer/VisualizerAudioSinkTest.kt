package com.podbelly.core.playback.visualizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

@OptIn(UnstableApi::class)
class VisualizerAudioSinkTest {

    private val sampleRate = 44_100
    private val hopUs = (sampleRate / SpectrumAnalyzer.FRAMES_PER_SECOND) * C.MICROS_PER_SECOND / sampleRate

    private val delegate = mockk<AudioSink>(relaxed = true)
    private val bus = AudioVisualizerBus()
    private val queue = AudibleFrameQueue()
    private var position = AudioSink.CURRENT_POSITION_NOT_SET
    private var consumeBuffers = true

    private lateinit var sink: VisualizerAudioSink

    private val config = AudioSink.AudioSinkConfig.Builder(
        Format.Builder()
            .setSampleMimeType("audio/raw")
            .setSampleRate(sampleRate)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build(),
    ).build()

    @Before
    fun setUp() {
        every { delegate.getCurrentPositionUs(any()) } answers { position }
        every { delegate.handleBuffer(any(), any(), any()) } answers { consumeBuffers }
        sink = VisualizerAudioSink(delegate, bus, pending = queue)
        bus.setActive(true)
        sink.configure(config)
    }

    /** 250 ms of a 1 kHz stereo tone, 16-bit LE. */
    private fun tone(): ByteBuffer {
        val frames = sampleRate / 4
        val buf = ByteBuffer.allocate(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val s = (sin(2 * PI * 1000.0 * i / sampleRate) * 0.8 * Short.MAX_VALUE).toInt().toShort()
            buf.putShort(s); buf.putShort(s)
        }
        buf.flip()
        return buf
    }

    private fun tick(atUs: Long): Long {
        position = atUs
        return sink.getCurrentPositionUs(false)
    }

    @Test
    fun `forwards configure and buffers to the real sink unchanged`() {
        val buf = tone()

        assertTrue(sink.handleBuffer(buf, 5_000_000L, 1))

        verify { delegate.configure(config) }
        verify { delegate.handleBuffer(buf, 5_000_000L, 1) }
        assertEquals(0, buf.position())
    }

    @Test
    fun `frames are held back until their audio is actually playing`() {
        val pts = 10_000_000L
        sink.handleBuffer(tone(), pts, 1)
        assertTrue("frames should be queued, not published", queue.size > 0)
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)

        // Position not yet known: nothing published.
        assertEquals(AudioSink.CURRENT_POSITION_NOT_SET, tick(AudioSink.CURRENT_POSITION_NOT_SET))
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)

        // Playing, but still before the first frame's audio has finished.
        assertEquals(pts, tick(pts))
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)

        // Once the first frame's audio has played, it appears.
        tick(pts + hopUs + 1_000)
        val first = bus.frames.value
        assertNotSame(VisualizerFrame.EMPTY, first)
        assertTrue(first.level > 0f)

        // Advancing well past everything publishes the newest frame and drains the queue.
        tick(pts + 300_000L)
        assertNotSame(first, bus.frames.value)
        assertEquals(0, queue.size)

        // Nothing new to say: the last frame stays put.
        val last = bus.frames.value
        tick(pts + 400_000L)
        assertSame(last, bus.frames.value)
    }

    @Test
    fun `a buffer the sink could not finish is analysed only once`() {
        consumeBuffers = false
        val buf = tone()

        assertTrue(!sink.handleBuffer(buf, 0L, 1))
        val afterFirst = queue.size
        assertTrue(!sink.handleBuffer(buf, 0L, 1))
        assertEquals(afterFirst, queue.size)

        // Once consumed, the same buffer object coming back means new audio.
        consumeBuffers = true
        assertTrue(sink.handleBuffer(buf, 0L, 1))
        assertEquals(afterFirst, queue.size)
        assertTrue(sink.handleBuffer(buf, 250_000L, 1))
        assertEquals(afterFirst * 2, queue.size)
    }

    @Test
    fun `flush and reset discard frames from before a seek`() {
        sink.handleBuffer(tone(), 0L, 1)
        sink.flush()
        tick(1_000_000L)
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)
        assertEquals(0, queue.size)
        verify { delegate.flush() }

        sink.handleBuffer(tone(), 2_000_000L, 1)
        sink.reset()
        tick(3_000_000L)
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)
        verify { delegate.reset() }
    }

    @Test
    fun `no analysis while the visualizer is off screen`() {
        bus.setActive(false)

        sink.handleBuffer(tone(), 0L, 1)

        assertEquals(0, queue.size)
        verify { delegate.handleBuffer(any(), 0L, 1) }
    }

    @Test
    fun `frames queued before the visualizer closes are dropped, not shown later`() {
        sink.handleBuffer(tone(), 0L, 1)
        bus.setActive(false)

        tick(1_000_000L)

        assertEquals(0, queue.size)
        assertSame(VisualizerFrame.EMPTY, bus.frames.value)
    }
}
