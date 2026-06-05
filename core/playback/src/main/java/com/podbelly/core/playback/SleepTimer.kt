package com.podbelly.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A countdown-based sleep timer that pauses playback when it expires.
 *
 * Exposes [remainingMillis] (0 when inactive) and [isActive] for the UI to observe.
 * Supports both a fixed-duration countdown and an "end of episode" mode that pauses
 * playback when the current episode finishes.
 */
@Singleton
class SleepTimer @Inject constructor(
    private val playbackController: PlaybackController,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var countdownJob: Job? = null

    private val _remainingMillis = MutableStateFlow(0L)

    /** Milliseconds remaining on the sleep timer. 0 means the timer is inactive. */
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /**
     * Whether the sleep timer is currently counting down. Derived directly from
     * [remainingMillis] (which is set to a non-zero sentinel in end-of-episode mode),
     * giving a single source of truth without an unchecked cast.
     */
    val isActive: StateFlow<Boolean> = _remainingMillis
        .map { it > 0L }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** When true, playback will be paused at the end of the current episode. */
    @Volatile
    private var endOfEpisode: Boolean = false

    /** The episode that was playing when end-of-episode mode was armed. */
    private var armedEpisodeId: Long = 0L

    private var endOfEpisodeJob: Job? = null

    /**
     * Starts a countdown sleep timer for the given number of [minutes].
     * If a timer is already running it will be replaced.
     */
    fun start(minutes: Int) {
        cancel()

        val totalMillis = minutes * 60_000L
        _remainingMillis.value = totalMillis

        countdownJob = scope.launch {
            var remaining = totalMillis
            while (remaining > 0L && isActive) {
                val tick = 1_000L.coerceAtMost(remaining)
                delay(tick)
                remaining -= tick
                _remainingMillis.value = remaining
            }

            if (isActive) {
                // Timer expired -- pause playback
                playbackController.pause()
                _remainingMillis.value = 0L
            }
        }
    }

    /**
     * Configures the timer to pause playback when the current episode ends.
     *
     * This does not set a fixed countdown. Instead, it monitors the playback state
     * and pauses as soon as [PlaybackState.isPlaying] becomes false after the episode
     * finishes (duration reached).
     */
    fun startEndOfEpisode() {
        cancel()

        endOfEpisode = true
        armedEpisodeId = 0L
        // Tell the controller to stop at the end of the episode instead of
        // auto-advancing the queue.
        playbackController.setPauseAtEpisodeEnd(true)
        // Set remaining to a sentinel value so isActive reads as true.
        _remainingMillis.value = Long.MAX_VALUE

        endOfEpisodeJob = scope.launch {
            // Trigger: react to the actual end-of-episode event from the controller.
            // We can't infer the end from currentPosition because it is reset to 0 on
            // STATE_ENDED before any near-duration position is ever observed.
            launch {
                playbackController.episodeEnded.collect {
                    if (!endOfEpisode) return@collect
                    endOfEpisode = false
                    _remainingMillis.value = 0L
                }
            }

            // Display: keep the remaining-time readout updated as the episode plays.
            // Also disarm if the user starts a *different* episode — the timer was
            // armed for the episode that was playing when it was set.
            launch {
                playbackController.playbackState.collect { state ->
                    if (!endOfEpisode) return@collect
                    val episodeId = state.episodeId
                    if (episodeId != 0L) {
                        if (armedEpisodeId == 0L) {
                            armedEpisodeId = episodeId
                        } else if (episodeId != armedEpisodeId) {
                            cancel()
                            return@collect
                        }
                    }
                    if (state.duration > 0L && state.isPlaying) {
                        _remainingMillis.value =
                            (state.duration - state.currentPosition).coerceAtLeast(0L)
                    }
                }
            }
        }
    }

    /**
     * Cancels any running sleep timer (fixed countdown or end-of-episode).
     */
    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        endOfEpisodeJob?.cancel()
        endOfEpisodeJob = null
        endOfEpisode = false
        armedEpisodeId = 0L
        // Re-enable queue auto-advance when the end-of-episode timer is cleared.
        playbackController.setPauseAtEpisodeEnd(false)
        _remainingMillis.value = 0L
    }
}
