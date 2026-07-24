package com.podbelly.core.playback.visualizer

import android.content.Context
import androidx.media3.common.audio.TeeAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * A [DefaultRenderersFactory] that inserts a [TeeAudioProcessor] into the audio
 * pipeline so the visualizer can read decoded PCM.
 *
 * It builds a [DefaultAudioSink.DefaultAudioProcessorChain], which keeps the
 * built-in SilenceSkipping and Sonic processors (playback speed + skip-silence
 * both still work) and simply runs the tee ahead of them. The tee copies the
 * audio read-only, so nothing about playback changes.
 */
@UnstableApi
internal class VisualizerRenderersFactory(
    context: Context,
    private val audioBufferSink: TeeAudioProcessor.AudioBufferSink,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(TeeAudioProcessor(audioBufferSink))
            )
            .build()
    }
}
