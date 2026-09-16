package com.podbelly.core.playback.visualizer

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/**
 * Wraps the real [AudioSink] so the visualizer sees decoded PCM *and* knows
 * when that PCM is actually heard.
 *
 * Every buffer handed to [handleBuffer] arrives with its presentation time, and
 * the sink's own [getCurrentPositionUs] reports, in the same timebase, the
 * position currently coming out of the speaker (it tracks the `AudioTrack`
 * playback head, so it already accounts for the sink's internal buffering and
 * the device's output latency). Frames are analysed as buffers arrive, tagged
 * with the time of the audio they describe, and only published once the
 * playback position has reached that time. Without this the bars react a
 * quarter to a full second before the sound, and in bursts as buffers land.
 *
 * Playback itself is untouched: every call is forwarded unchanged, and analysis
 * is skipped entirely while the visualizer isn't on screen
 * ([AudioVisualizerBus.active] == false).
 */
@UnstableApi
internal class VisualizerAudioSink(
    delegate: AudioSink,
    private val bus: AudioVisualizerBus,
    private val analyzer: SpectrumAnalyzer = SpectrumAnalyzer(),
    private val pending: AudibleFrameQueue = AudibleFrameQueue(),
) : ForwardingAudioSink(delegate) {

    /** The buffer the delegate hasn't fully consumed yet, so we analyse each buffer once. */
    private var partiallyConsumed: ByteBuffer? = null

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        super.configure(audioSinkConfig)
        val format = audioSinkConfig.format
        analyzer.configure(format.sampleRate, format.channelCount, format.pcmEncoding)
        pending.clear()
        partiallyConsumed = null
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        // The renderer re-offers the very same buffer object until we return
        // true, so only analyse it the first time it appears.
        if (buffer !== partiallyConsumed) {
            if (bus.active) {
                analyzer.analyze(buffer, presentationTimeUs) { timeUs, frame -> pending.add(timeUs, frame) }
            }
            partiallyConsumed = buffer
        }
        val consumed = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        if (consumed) partiallyConsumed = null
        return consumed
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        // The renderer asks for this on every playback-thread tick (~10 ms), which
        // makes it a steady clock for releasing frames as their audio is heard.
        val positionUs = super.getCurrentPositionUs(sourceEnded)
        publishAudible(positionUs)
        return positionUs
    }

    override fun flush() {
        super.flush()
        discardPending()
    }

    override fun reset() {
        super.reset()
        discardPending()
    }

    private fun publishAudible(positionUs: Long) {
        if (positionUs == AudioSink.CURRENT_POSITION_NOT_SET) return
        if (!bus.active) {
            if (pending.size > 0) pending.clear()
            return
        }
        pending.pollAudible(positionUs)?.let(bus::publish)
    }

    private fun discardPending() {
        pending.clear()
        analyzer.reset()
        partiallyConsumed = null
    }
}
