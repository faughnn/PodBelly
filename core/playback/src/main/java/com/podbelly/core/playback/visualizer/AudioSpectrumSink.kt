package com.podbelly.core.playback.visualizer

import androidx.media3.common.C
import androidx.media3.common.audio.TeeAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Taps the decoded PCM stream (via a [TeeAudioProcessor]) and turns it into
 * [VisualizerFrame]s on the bus. It never modifies the audio — the tee passes a
 * read-only copy — so playback, speed and skip-silence are unaffected.
 *
 * All work runs on the player's audio thread. When the visualizer isn't on
 * screen ([AudioVisualizerBus.active] == false) [handleBuffer] returns
 * immediately, so the tap costs effectively nothing the rest of the time.
 */
@UnstableApi
internal class AudioSpectrumSink(
    private val bus: AudioVisualizerBus,
) : TeeAudioProcessor.AudioBufferSink {

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
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var hopSamples = 512
    private var samplesSinceFrame = 0

    // Per-band FFT bin ranges, recomputed whenever the format changes.
    private val bandStart = IntArray(VisualizerFrame.BAND_COUNT)
    private val bandEnd = IntArray(VisualizerFrame.BAND_COUNT)

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        hopSamples = (sampleRateHz / FRAMES_PER_SECOND).coerceAtLeast(256)
        samplesSinceFrame = 0
        writePos = 0
        ring.fill(0f)
        smoothedBands.fill(0f)
        smoothedLevel = 0f
        computeBandRanges()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!bus.active || sampleRate <= 0) return

        buffer.order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val shorts = buffer.asShortBuffer()
                val frames = shorts.remaining() / channelCount
                for (f in 0 until frames) {
                    var sum = 0f
                    for (c in 0 until channelCount) sum += shorts.get() / 32768f
                    pushSample(sum / channelCount)
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                val floats = buffer.asFloatBuffer()
                val frames = floats.remaining() / channelCount
                for (f in 0 until frames) {
                    var sum = 0f
                    for (c in 0 until channelCount) sum += floats.get()
                    pushSample(sum / channelCount)
                }
            }
            else -> return // Unsupported PCM encoding — skip analysis.
        }
    }

    private fun pushSample(sample: Float) {
        ring[writePos] = sample
        writePos = (writePos + 1) % fftSize
        if (++samplesSinceFrame >= hopSamples) {
            samplesSinceFrame = 0
            computeFrame()
        }
    }

    private fun computeFrame() {
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

        bus.publish(VisualizerFrame(bands = outBands, waveform = outWave, level = smoothedLevel))
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

    private companion object {
        const val FRAMES_PER_SECOND = 40
        const val BAND_DECAY = 0.86f
        const val MIN_DB = -66f
        const val MIN_FREQ_HZ = 40
        const val MAX_FREQ_HZ = 16000
    }
}
