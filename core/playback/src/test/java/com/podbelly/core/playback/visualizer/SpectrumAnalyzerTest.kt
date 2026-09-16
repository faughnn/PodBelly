package com.podbelly.core.playback.visualizer

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SpectrumAnalyzerTest {

    private val sampleRate = 44_100
    private val hop = sampleRate / SpectrumAnalyzer.FRAMES_PER_SECOND

    /** [frames] sample-frames of a [freqHz] sine at [amplitude], interleaved across [channels], 16-bit LE. */
    private fun sine16(frames: Int, channels: Int, freqHz: Double, amplitude: Double = 0.8): ByteBuffer {
        val buf = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val s = (sin(2 * PI * freqHz * i / sampleRate) * amplitude * Short.MAX_VALUE).toInt().toShort()
            repeat(channels) { buf.putShort(s) }
        }
        buf.flip()
        return buf
    }

    private fun sineFloat(frames: Int, channels: Int, freqHz: Double, amplitude: Float = 0.8f): ByteBuffer {
        val buf = ByteBuffer.allocate(frames * channels * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val s = (sin(2 * PI * freqHz * i / sampleRate) * amplitude).toFloat()
            repeat(channels) { buf.putFloat(s) }
        }
        buf.flip()
        return buf
    }

    private fun collect(analyzer: SpectrumAnalyzer, buffer: ByteBuffer, pts: Long): List<Pair<Long, VisualizerFrame>> {
        val out = mutableListOf<Pair<Long, VisualizerFrame>>()
        analyzer.analyze(buffer, pts) { t, f -> out += t to f }
        return out
    }

    @Test
    fun `emits one frame per hop and stamps each with the time its last sample plays`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 2, C.ENCODING_PCM_16BIT)
        val frames = sampleRate / 4 // 250 ms
        val pts = 10_000_000L

        val out = collect(analyzer, sine16(frames, 2, 1000.0), pts)

        assertEquals(frames / hop, out.size)
        out.forEachIndexed { i, (timeUs, _) ->
            val lastSampleIndex = hop * (i + 1) - 1
            assertEquals(pts + (lastSampleIndex + 1) * C.MICROS_PER_SECOND / sampleRate, timeUs)
        }
        // Every frame is strictly later than the last and later than the buffer start.
        assertTrue(out.first().first > pts)
        assertTrue(out.zipWithNext().all { (a, b) -> b.first > a.first })
    }

    @Test
    fun `hop position carries across buffers`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 1, C.ENCODING_PCM_16BIT)
        val half = hop / 2

        // Two half-hop buffers make exactly one frame, timed by the second buffer.
        assertTrue(collect(analyzer, sine16(half, 1, 440.0), 0L).isEmpty())
        val second = collect(analyzer, sine16(hop - half, 1, 440.0), 500_000L)

        assertEquals(1, second.size)
        assertEquals(500_000L + (hop - half) * C.MICROS_PER_SECOND / sampleRate, second[0].first)
    }

    @Test
    fun `a 1 kHz tone peaks in a mid band and is quiet at the extremes`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 2, C.ENCODING_PCM_16BIT)

        val last = collect(analyzer, sine16(sampleRate / 4, 2, 1000.0), 0L).last().second
        val bands = last.bands
        val peakBand = bands.indices.maxBy { bands[it] }

        assertTrue("peak band should be well above rest, was ${bands[peakBand]}", bands[peakBand] > 0.6f)
        assertTrue("1 kHz should not land in the lowest band", peakBand > 4)
        assertTrue("1 kHz should not land in the highest band", peakBand < VisualizerFrame.BAND_COUNT - 5)
        assertTrue(bands[0] < bands[peakBand] * 0.7f)
        assertTrue(bands[VisualizerFrame.BAND_COUNT - 1] < bands[peakBand] * 0.7f)
        assertTrue("loud tone should have a clear level", last.level > 0.3f)
        assertEquals(VisualizerFrame.WAVE_COUNT, last.waveform.size)
        assertTrue(last.waveform.any { it > 0.5f } && last.waveform.any { it < -0.5f })
    }

    @Test
    fun `silence yields resting frames`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 2, C.ENCODING_PCM_16BIT)
        val silence = ByteBuffer.allocate(sampleRate / 4 * 2 * 2)

        val last = collect(analyzer, silence, 0L).last().second

        assertEquals(0f, last.level, 0f)
        assertTrue(last.bands.all { it == 0f })
        assertTrue(last.waveform.all { it == 0f })
    }

    @Test
    fun `float pcm is analysed too`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 2, C.ENCODING_PCM_FLOAT)

        val out = collect(analyzer, sineFloat(sampleRate / 4, 2, 1000.0), 0L)

        assertEquals((sampleRate / 4) / hop, out.size)
        assertTrue(out.last().second.level > 0.3f)
    }

    @Test
    fun `unsupported encodings and unconfigured analyzers emit nothing`() {
        val unconfigured = SpectrumAnalyzer()
        assertFalse(unconfigured.isReady)
        assertTrue(collect(unconfigured, sine16(sampleRate, 2, 1000.0), 0L).isEmpty())

        val passthrough = SpectrumAnalyzer()
        passthrough.configure(sampleRate, 2, C.ENCODING_AC3)
        assertFalse(passthrough.isReady)
        assertTrue(collect(passthrough, sine16(sampleRate, 2, 1000.0), 0L).isEmpty())
    }

    @Test
    fun `analyze leaves the buffer position untouched`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 2, C.ENCODING_PCM_16BIT)
        val buffer = sine16(hop * 2, 2, 1000.0)

        collect(analyzer, buffer, 0L)

        assertEquals(0, buffer.position())
        assertEquals(hop * 2 * 2 * 2, buffer.remaining())
    }

    @Test
    fun `reset forgets buffered audio and smoothing`() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(sampleRate, 1, C.ENCODING_PCM_16BIT)
        collect(analyzer, sine16(sampleRate / 4, 1, 1000.0), 0L)

        analyzer.reset()
        val silence = ByteBuffer.allocate(hop * 2)
        val out = collect(analyzer, silence, 0L)

        assertEquals(1, out.size)
        assertEquals(0f, out[0].second.level, 0f)
        assertTrue(out[0].second.bands.all { it == 0f })
    }
}
