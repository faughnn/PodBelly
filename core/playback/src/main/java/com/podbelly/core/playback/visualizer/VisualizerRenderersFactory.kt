package com.podbelly.core.playback.visualizer

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink

/**
 * A [DefaultRenderersFactory] whose audio sink is wrapped in a
 * [VisualizerAudioSink] so the Now Playing visualizer can read decoded PCM,
 * timed to when it is actually heard.
 *
 * The underlying sink is exactly the stock one (with its Sonic and
 * SilenceSkipping processors, so playback speed and skip-silence still work);
 * the wrapper only observes buffers and forwards every call unchanged.
 */
@UnstableApi
internal class VisualizerRenderersFactory(
    context: Context,
    private val bus: AudioVisualizerBus,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink? {
        val delegate = super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            ?: return null
        return VisualizerAudioSink(delegate, bus)
    }
}
