package com.podbelly.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.PodcastSkipSettings
import com.podbelly.core.network.model.TranscriptCue
import com.podbelly.core.network.transcript.TranscriptParser
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
import com.podbelly.core.playback.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val transcript: TranscriptUiState = TranscriptUiState(),
)

/** State of the Podcasting 2.0 transcript panel for the playing episode. */
data class TranscriptUiState(
    /** True when the playing episode declares a `<podcast:transcript>` URL. */
    val available: Boolean = false,
    /** True while the transcript sheet is open. */
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val cues: List<TranscriptCue> = emptyList(),
    /** True when the transcript failed to download or couldn't be parsed. */
    val error: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val preferencesManager: PreferencesManager,
    private val sleepTimer: SleepTimer,
    private val transcriptParser: TranscriptParser,
) : ViewModel() {

    private val _showSleepTimerPicker = MutableStateFlow(false)
    private val _showSpeedPicker = MutableStateFlow(false)
    private val _showChaptersList = MutableStateFlow(false)
    private val _showVolumeBoostWarning = MutableStateFlow(false)
    private val _transcript = MutableStateFlow(TranscriptUiState())

    /** URL whose cues are currently held in [_transcript]; avoids re-downloading. */
    private var loadedTranscriptUrl: String? = null

    private data class TranscriptMeta(val url: String, val type: String)

    /**
     * `<podcast:transcript>` URL + type of the playing episode, or null when it has
     * none. Follows the playing episode via the database so a feed refresh that
     * backfills a transcript makes the button appear without restarting playback.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val transcriptMeta: StateFlow<TranscriptMeta?> = playbackController.playbackState
        .map { it.episodeId }
        .distinctUntilChanged()
        .flatMapLatest { episodeId ->
            if (episodeId == 0L) flowOf(null) else episodeDao.getById(episodeId)
        }
        .map { episode ->
            episode?.transcriptUrl?.takeIf { it.isNotBlank() }
                ?.let { url -> TranscriptMeta(url = url, type = episode.transcriptType) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Intro/outro auto-skip settings of the playing episode's podcast, followed via
     * the database so edits made anywhere (here or the podcast page) stay in sync.
     * Null while nothing is playing.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val skipSettings: StateFlow<PodcastSkipSettings?> = playbackController.playbackState
        .map { it.podcastId }
        .distinctUntilChanged()
        .flatMapLatest { podcastId ->
            if (podcastId == 0L) flowOf(null) else podcastDao.getById(podcastId)
        }
        .map { podcast ->
            podcast?.let {
                PodcastSkipSettings(
                    skipIntroSeconds = it.skipIntroSeconds,
                    skipOutroSeconds = it.skipOutroSeconds,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.playbackState,
        sleepTimer.remainingMillis,
        _showSleepTimerPicker,
        _showSpeedPicker,
        combine(_showChaptersList, _showVolumeBoostWarning, _transcript, ::Triple),
    ) { playback, timerRemaining, showSleep, showSpeed, (showChapters, showBoostWarning, transcript) ->
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
            transcript = transcript,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(),
    )

    init {
        // Track transcript availability for the playing episode, and drop stale
        // cues when the episode (or its transcript URL) changes.
        viewModelScope.launch {
            transcriptMeta.collect { meta ->
                _transcript.update { current ->
                    when {
                        meta == null -> TranscriptUiState()
                        // Different transcript than the loaded one (episode changed):
                        // drop stale cues and close the sheet.
                        meta.url != loadedTranscriptUrl -> TranscriptUiState(available = true)
                        else -> current.copy(available = true)
                    }
                }
            }
        }
        // Speed is now applied in PlaybackController.play() so it's correct
        // from the first moment regardless of which screen started playback.
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

    fun setSkipIntroSeconds(seconds: Int) {
        val podcastId = playbackController.playbackState.value.podcastId
        if (podcastId == 0L) return
        viewModelScope.launch {
            podcastDao.updateSkipIntroSeconds(podcastId, seconds.coerceAtLeast(0))
            playbackController.refreshSkipSettings()
        }
    }

    fun setSkipOutroSeconds(seconds: Int) {
        val podcastId = playbackController.playbackState.value.podcastId
        if (podcastId == 0L) return
        viewModelScope.launch {
            podcastDao.updateSkipOutroSeconds(podcastId, seconds.coerceAtLeast(0))
            // Re-arm the playing episode's outro so "after now is ads" takes
            // effect immediately, not on the next episode.
            playbackController.refreshSkipSettings()
        }
    }

    fun savePosition() {
        viewModelScope.launch {
            val state = playbackController.playbackState.value
            if (state.episodeId != 0L && state.currentPosition > 0L) {
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

    /**
     * Opens the transcript sheet, downloading and parsing the transcript on first
     * open (cached afterwards, until the playing episode changes).
     */
    fun showTranscript() {
        val meta = transcriptMeta.value ?: return
        _transcript.update { it.copy(visible = true) }
        if (loadedTranscriptUrl == meta.url && _transcript.value.cues.isNotEmpty()) return

        viewModelScope.launch {
            _transcript.update { it.copy(isLoading = true, error = false) }
            val cues = try {
                transcriptParser.fetch(meta.url, meta.type)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            // The episode may have changed while downloading; don't clobber the
            // new episode's transcript state with this one's result.
            if (transcriptMeta.value?.url != meta.url) return@launch

            if (cues.isEmpty()) {
                loadedTranscriptUrl = null
                _transcript.update { it.copy(isLoading = false, cues = emptyList(), error = true) }
            } else {
                loadedTranscriptUrl = meta.url
                _transcript.update { it.copy(isLoading = false, cues = cues, error = false) }
            }
        }
    }

    fun hideTranscript() {
        _transcript.update { it.copy(visible = false) }
    }

    /** Seeks playback to a transcript cue's start time. */
    fun seekToCue(startMs: Long) {
        playbackController.seekTo(startMs)
    }
}
