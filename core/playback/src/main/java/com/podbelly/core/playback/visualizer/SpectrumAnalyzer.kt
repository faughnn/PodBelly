package com.podbelly.core.playback.visualizer

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Turns decoded PCM into [VisualizerFrame]s: a windowed 1024-point FFT split
 * into log-spaced bands, a downsampled waveform and an RMS level, produced
 * every [FRAMES_PER_SECOND]th of a second of audio.
 *
 * Each frame is reported with the presentation time (microseconds, in the
 * caller's timebase) of the last sample it covers, so the caller can hold it
 * back until that audio is actually heard. Pure computation; runs on the
 * playback thread.
 */
internal class SpectrumAnalyzer {

    private val fftSize = 1024
    private val fft = Fft(fftSize)

    // Hann window (depends only on size), applied before the FFT to cut leakage.
    private val window = FloatArray(fftSize) { i ->
        (0.5 - 0.5 * kotlin.math.cos(2.0 * Math.PI * i / (fftSize - 1))).toFloat()
    }

    // Circular buffer of the most recent mono samples.
    private val ring = FloatArray(fftSize)
    private var writePos = 0

    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val smoothedBands = FloatArray(VisualizerFrame.BAND_COUNT)
    private var smoothedLevel = 0f

    private var sampleRate = 0
    private var channelCount = 1
    private var encoding = C.ENCODING_INVALID
    private var hopSamples = 512
    private var samplesSinceFrame = 0

    // Per-band FFT bin ranges, recomputed whenever the format changes.
    private val bandStart = IntArray(VisualizerFrame.BAND_COUNT)
    private val bandEnd = IntArray(VisualizerFrame.BAND_COUNT)

    /** True once [configure] has been given a PCM layout this analyzer can read. */
    val isReady: Boolean
        get() = sampleRate > 0 &&
            (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT)

    /** Sets the PCM layout of buffers passed to [analyze] and resets all state. */
    fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        hopSamples = if (sampleRateHz > 0) (sampleRateHz / FRAMES_PER_SECOND).coerceAtLeast(256) else 512
        reset()
        if (sampleRateHz > 0) computeBandRanges()
    }

    /** Forgets buffered audio and smoothing (after a seek or flush). */
    fun reset() {
        samplesSinceFrame = 0
        writePos = 0
        ring.fill(0f)
        smoothedBands.fill(0f)
        smoothedLevel = 0f
    }

    /**
     * Analyses [buffer] (read-only; its position is left untouched), whose first
     * sample plays at [presentationTimeUs]. Calls [onFrame] with each completed
     * frame and the time at which its last sample plays.
     */
    fun analyze(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        onFrame: (timeUs: Long, frame: VisualizerFrame) -> Unit,
    ) {
        if (!isReady) return
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val shorts = data.asShortBuffer()
                val frames = shorts.remaining() / channelCount
                for (f in 0 until frames) {
                    var sum = 0f
                    for (c in 0 until channelCount) sum += shorts.get() / 32768f
                    pushSample(sum / channelCount, presentationTimeUs, f, onFrame)
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                val floats = data.asFloatBuffer()
                val frames = floats.remaining() / channelCount
                for (f in 0 until frames) {
                    var sum = 0f
                    for (c in 0 until channelCount) sum += floats.get()
                    pushSample(sum / channelCount, presentationTimeUs, f, onFrame)
                }
            }
        }
    }

    private inline fun pushSample(
        sample: Float,
        presentationTimeUs: Long,
        indexInBuffer: Int,
        onFrame: (Long, VisualizerFrame) -> Unit,
    ) {
        ring[writePos] = sample
        writePos = (writePos + 1) % fftSize
        if (++samplesSinceFrame >= hopSamples) {
            samplesSinceFrame = 0
            val endTimeUs = presentationTimeUs + (indexInBuffer + 1) * C.MICROS_PER_SECOND / sampleRate
            onFrame(endTimeUs, computeFrame())
        }
    }

    private fun computeFrame(): VisualizerFrame {
        // Copy the ring into chronological order and window it.
        var rms = 0f
        for (k in 0 until fftSize) {
            val s = ring[(writePos + k) % fftSize]
            rms += s * s
            real[k] = s * window[k]
            imag[k] = 0f
        }
        rms = sqrt(rms / fftSize)

        fft.transform(real, imag)

        // Frequency bands: peak magnitude within each log-spaced bin range,
        // dB-scaled to 0..1, with a fast attack / slow decay for lively bars.
        val outBands = FloatArray(VisualizerFrame.BAND_COUNT)
        for (b in 0 until VisualizerFrame.BAND_COUNT) {
            var peak = 0f
            for (bin in bandStart[b]..bandEnd[b]) {
                val mag = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin]) * (2f / fftSize)
                if (mag > peak) peak = mag
            }
            val norm = normalizeDb(peak)
            val prev = smoothedBands[b]
            smoothedBands[b] = if (norm > prev) norm else prev * BAND_DECAY
            outBands[b] = smoothedBands[b]
        }

        // Waveform: evenly-spaced raw samples in chronological order.
        val outWave = FloatArray(VisualizerFrame.WAVE_COUNT)
        val stride = fftSize / VisualizerFrame.WAVE_COUNT
        for (i in 0 until VisualizerFrame.WAVE_COUNT) {
            outWave[i] = ring[(writePos + i * stride) % fftSize]
        }

        // Overall level: smoothed RMS.
        val levelNorm = normalizeDb(rms)
        smoothedLevel = smoothedLevel * 0.8f + levelNorm * 0.2f

        return VisualizerFrame(bands = outBands, waveform = outWave, level = smoothedLevel)
    }

    private fun computeBandRanges() {
        val maxBin = fftSize / 2
        val minFreqBin = (MIN_FREQ_HZ * fftSize / sampleRate).coerceAtLeast(1)
        val maxFreqBin = (MAX_FREQ_HZ * fftSize / sampleRate).coerceIn(minFreqBin + 1, maxBin)
        val ratio = maxFreqBin.toDouble() / minFreqBin
        var lastEnd = minFreqBin - 1
        for (b in 0 until VisualizerFrame.BAND_COUNT) {
            val edge = (minFreqBin * ratio.pow((b + 1).toDouble() / VisualizerFrame.BAND_COUNT)).toInt()
            val start = (lastEnd + 1).coerceIn(1, maxBin)
            val end = edge.coerceIn(start, maxBin)
            bandStart[b] = start
            bandEnd[b] = end
            lastEnd = end
        }
    }

    /** Map an amplitude to 0..1 on a dB scale (MIN_DB..0 dB). */
    private fun normalizeDb(amplitude: Float): Float {
        if (amplitude <= 0f) return 0f
        val db = 20f * log10(amplitude)
        return ((db - MIN_DB) / -MIN_DB).coerceIn(0f, 1f)
    }

    companion object {
        const val FRAMES_PER_SECOND = 40
        private const val BAND_DECAY = 0.86f
        private const val MIN_DB = -66f
        private const val MIN_FREQ_HZ = 40
        private const val MAX_FREQ_HZ = 16000
    }
}
