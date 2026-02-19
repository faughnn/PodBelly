package com.podbelly.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    /** Whether the sleep timer is currently counting down. */
    val isActive: StateFlow<Boolean> = MutableStateFlow(false).also { active ->
        // Mirror the remainingMillis state into a boolean for convenience.
        scope.launch {
            _remainingMillis.collect { remaining ->
                (active as MutableStateFlow<Boolean>).value = remaining > 0L || endOfEpisode
            }
        }
    }

    /** When true, playback will be paused at the end of the current episode. */
    @Volatile
    private var endOfEpisode: Boolean = false

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
        // Set remaining to a sentinel value so isActive reads as true.
        _remainingMillis.value = Long.MAX_VALUE

        endOfEpisodeJob = scope.launch {
            playbackController.playbackState.collect { state ->
                if (!endOfEpisode) return@collect

                // If the episode reached the end and is no longer playing, trigger pause.
                val reachedEnd = state.duration > 0L &&
                        state.currentPosition >= state.duration - 500L &&
                        !state.isPlaying

                if (reachedEnd) {
                    playbackController.pause()
                    endOfEpisode = false
                    _remainingMillis.value = 0L
                    return@collect
                }

                // Update remaining millis based on distance to end of episode.
                if (state.duration > 0L && state.isPlaying) {
                    val left = (state.duration - state.currentPosition).coerceAtLeast(0L)
                    _remainingMillis.value = left
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
        _remainingMillis.value = 0L
    }
}
