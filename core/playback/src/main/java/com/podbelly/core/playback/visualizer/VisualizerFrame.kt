package com.podbelly.core.playback.visualizer

/**
 * One frame of analysed audio for the Now Playing visualizer. Produced ~40x/sec
 * by the audio tap in [AudioSpectrumSink] and consumed by the Compose renderers.
 *
 * Each field is a different view of the same slice of audio, so a given style
 * only reads the one it needs:
 * - [bands]: log-spaced FFT magnitudes (bass → treble), each normalised 0..1.
 * - [waveform]: evenly-sampled raw waveform, each -1..1 (for oscilloscope looks).
 * - [level]: overall loudness (RMS) of the slice, normalised 0..1.
 */
class VisualizerFrame(
    val bands: FloatArray,
    val waveform: FloatArray,
    val level: Float,
) {
    companion object {
        const val BAND_COUNT = 48
        const val WAVE_COUNT = 128

        /** A silent frame — all bars/level at rest. */
        val EMPTY = VisualizerFrame(
            bands = FloatArray(BAND_COUNT),
            waveform = FloatArray(WAVE_COUNT),
            level = 0f,
        )
    }
}
