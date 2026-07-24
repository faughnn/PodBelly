package com.podbelly.core.playback.visualizer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process bridge that carries analysed audio frames from the audio tap
 * (running on the player's audio thread inside [com.podbelly.core.playback.PlaybackService])
 * to the Compose visualizer on the Now Playing screen.
 *
 * The playback service and the UI run in the same process (the MediaLibrary
 * service has no `android:process`), so a plain [Singleton] holding a
 * [StateFlow] is enough — no session/IPC serialisation, which would be far too
 * heavy for ~40 frames a second.
 *
 * [active] gates the work: the tap only computes and publishes frames while the
 * visualizer is actually on screen and playing, so there's no cost the rest of
 * the time. The UI sets it via [setActive].
 */
@Singleton
class AudioVisualizerBus @Inject constructor() {

    @Volatile
    var active: Boolean = false
        private set

    private val _frames = MutableStateFlow(VisualizerFrame.EMPTY)
    val frames: StateFlow<VisualizerFrame> = _frames.asStateFlow()

    /** Called by the UI: start/stop the tap's per-frame analysis. */
    fun setActive(value: Boolean) {
        active = value
        if (!value) {
            // Fall back to rest so a re-opened visualizer doesn't flash a stale frame.
            _frames.value = VisualizerFrame.EMPTY
        }
    }

    /** Called by the audio tap on the audio thread. */
    fun publish(frame: VisualizerFrame) {
        _frames.value = frame
    }
}
