package com.podbelly.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
import com.podbelly.core.playback.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val sleepTimerRemaining: Long = 0L,
    val isSleepTimerActive: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val skipSilence: Boolean = false,
    val volumeBoost: Boolean = false,
    val showSleepTimerPicker: Boolean = false,
    val showSpeedPicker: Boolean = false,
    val showChaptersList: Boolean = false,
    val showVolumeBoostWarning: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val queueDao: QueueDao,
    private val preferencesManager: PreferencesManager,
    private val sleepTimer: SleepTimer,
) : ViewModel() {

    private val _showSleepTimerPicker = MutableStateFlow(false)
    private val _showSpeedPicker = MutableStateFlow(false)
    private val _showChaptersList = MutableStateFlow(false)
    private val _showVolumeBoostWarning = MutableStateFlow(false)

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.playbackState,
        sleepTimer.remainingMillis,
        _showSleepTimerPicker,
        _showSpeedPicker,
        combine(_showChaptersList, _showVolumeBoostWarning, ::Pair),
    ) { playback, timerRemaining, showSleep, showSpeed, (showChapters, showBoostWarning) ->
        PlayerUiState(
            playbackState = playback,
            sleepTimerRemaining = timerRemaining,
            isSleepTimerActive = timerRemaining > 0L,
            playbackSpeed = playback.playbackSpeed,
            skipSilence = playback.skipSilence,
            volumeBoost = playback.volumeBoost,
            showSleepTimerPicker = showSleep,
            showSpeedPicker = showSpeed,
            showChaptersList = showChapters,
            showVolumeBoostWarning = showBoostWarning,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(),
    )

    init {
        // Apply per-podcast playback speed when the podcast changes
        viewModelScope.launch {
            playbackController.playbackState
                .map { it.podcastId }
                .distinctUntilChanged()
                .collect { podcastId ->
                    if (podcastId != 0L) {
                        val podcastSpeed = podcastDao.getPlaybackSpeed(podcastId)
                        if (podcastSpeed != null && podcastSpeed > 0f) {
                            playbackController.setPlaybackSpeed(podcastSpeed)
                        } else {
                            val globalSpeed = preferencesManager.playbackSpeed.first()
                            playbackController.setPlaybackSpeed(globalSpeed)
                        }
                    }
                }
        }
        // Apply global speed changes only if the current podcast has no override
        viewModelScope.launch {
            preferencesManager.playbackSpeed.collect { globalSpeed ->
                val podcastId = playbackController.playbackState.value.podcastId
                if (podcastId != 0L) {
                    val podcastSpeed = podcastDao.getPlaybackSpeed(podcastId)
                    if (podcastSpeed == null || podcastSpeed <= 0f) {
                        playbackController.setPlaybackSpeed(globalSpeed)
                    }
                } else {
                    playbackController.setPlaybackSpeed(globalSpeed)
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.skipSilence.collect { enabled ->
                playbackController.setSkipSilence(enabled)
            }
        }
        viewModelScope.launch {
            preferencesManager.volumeBoost.collect { enabled ->
                playbackController.setVolumeBoost(enabled)
            }
        }
    }

    fun togglePlayPause() {
        val state = playbackController.playbackState.value
        if (state.isPlaying) {
            playbackController.pause()
        } else {
            playbackController.resume()
        }
    }

    fun seekTo(position: Long) {
        playbackController.seekTo(position)
    }

    fun skipForward() {
        playbackController.skipForward(seconds = 30)
    }

    fun skipBack() {
        playbackController.skipBack(seconds = 10)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackController.setPlaybackSpeed(speed)
        viewModelScope.launch {
            val podcastId = playbackController.playbackState.value.podcastId
            if (podcastId != 0L) {
                podcastDao.updatePlaybackSpeed(podcastId, speed)
            } else {
                preferencesManager.setPlaybackSpeed(speed)
            }
        }
        _showSpeedPicker.value = false
    }

    fun toggleSkipSilence() {
        // Read from PlaybackState which is now updated by PlaybackController
        val current = playbackController.playbackState.value.skipSilence
        val newValue = !current
        playbackController.setSkipSilence(newValue)
        viewModelScope.launch {
            preferencesManager.setSkipSilence(newValue)
        }
    }

    fun toggleVolumeBoost() {
        val current = playbackController.playbackState.value.volumeBoost
        if (current) {
            // Disabling — no warning needed
            playbackController.setVolumeBoost(false)
            viewModelScope.launch {
                preferencesManager.setVolumeBoost(false)
            }
        } else {
            // Enabling — show warning first
            _showVolumeBoostWarning.value = true
        }
    }

    fun confirmVolumeBoost() {
        _showVolumeBoostWarning.value = false
        playbackController.setVolumeBoost(true)
        viewModelScope.launch {
            preferencesManager.setVolumeBoost(true)
        }
    }

    fun dismissVolumeBoostWarning() {
        _showVolumeBoostWarning.value = false
    }

    /**
     * Starts a fixed-duration sleep timer for the given number of [minutes].
     */
    fun startSleepTimer(minutes: Int) {
        sleepTimer.start(minutes)
        viewModelScope.launch {
            preferencesManager.setSleepTimerMinutes(minutes)
        }
        _showSleepTimerPicker.value = false
    }

    /**
     * Starts the "end of episode" sleep timer mode.
     * Playback will pause when the current episode finishes.
     */
    fun startSleepTimerEndOfEpisode() {
        sleepTimer.startEndOfEpisode()
        _showSleepTimerPicker.value = false
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
        viewModelScope.launch {
            preferencesManager.setSleepTimerMinutes(0)
        }
    }

    fun savePosition() {
        viewModelScope.launch {
            val state = playbackController.playbackState.value
            if (state.episodeId != 0L) {
                episodeDao.updatePlaybackPosition(state.episodeId, state.currentPosition)
            }
        }
    }

    fun seekToNextChapter() {
        playbackController.seekToNextChapter()
    }

    fun seekToPreviousChapter() {
        playbackController.seekToPreviousChapter()
    }

    fun seekToChapter(index: Int) {
        playbackController.seekToChapter(index)
    }

    fun showSpeedPicker() {
        _showSpeedPicker.value = true
    }

    fun hideSpeedPicker() {
        _showSpeedPicker.value = false
    }

    fun showSleepTimerPicker() {
        _showSleepTimerPicker.value = true
    }

    fun hideSleepTimerPicker() {
        _showSleepTimerPicker.value = false
    }

    fun showChaptersList() {
        _showChaptersList.value = true
    }

    fun hideChaptersList() {
        _showChaptersList.value = false
    }
}
