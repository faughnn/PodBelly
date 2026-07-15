package com.podbelly.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.ListeningSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton controller that manages a [MediaController] connection to [PlaybackService]
 * and exposes a reactive [playbackState] for the UI layer.
 *
 * The UI observes [playbackState] and calls the various control methods (play, pause,
 * seekTo, etc.) to drive playback. Internally this class connects to the [PlaybackService]
 * via a Media3 [MediaController].
 */
@Singleton
class PlaybackController @Inject constructor(
    private val queueDao: QueueDao,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val preferencesManager: PreferencesManager,
    private val listeningSessionDao: ListeningSessionDao,
) {

    companion object {
        private const val TAG = "PlaybackController"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** Emitted once each time the current episode reaches its natural end (STATE_ENDED). */
    private val _episodeEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val episodeEnded: SharedFlow<Unit> = _episodeEnded.asSharedFlow()

    /**
     * When true, the player pauses at the end of the current episode instead of
     * auto-advancing the queue. Set by the sleep timer's "end of episode" mode.
     */
    @Volatile
    private var pauseAtEpisodeEnd: Boolean = false

    private var positionUpdateJob: Job? = null
    private var speedLoadJob: Job? = null

    /**
     * Per-podcast outro auto-skip (seconds) for the currently playing podcast, loaded
     * alongside the per-podcast speed when playback starts. 0 = disabled.
     */
    @Volatile
    private var currentSkipOutroSeconds: Int = 0

    /**
     * Guards the outro-skip so it ends the episode at most once per playback.
     * Reset whenever a new episode starts.
     */
    @Volatile
    private var outroEndFired: Boolean = false

    /** Flag indicating that resume() was called and we should apply auto-rewind on next isPlaying=true. */
    private var isResuming = false

    /** The episode ID currently being played, tracked locally for metadata purposes. */
    private var currentEpisodeId: Long = 0L
    private var currentPodcastId: Long = 0L
    private var currentAudioUrl: String = ""
    private var currentArtworkUrl: String = ""
    private var currentPodcastTitle: String = ""
    private var currentEpisodeTitle: String = ""

    /**
     * Per-session state for an in-flight insert. Each [startListeningSession] creates a
     * fresh holder that its own insert continuation captures, so two overlapping sessions
     * (e.g. a rapid auto-advance / track change while the prior insert is still
     * committing) no longer share single mutable fields. If a session is ended before its
     * insert completes, [ended] is set and the insert continuation finalizes *its own*
     * row — previously this clobbered the stash and orphaned the earlier session.
     */
    private class PendingSession {
        var ended: Boolean = false
        var endTime: Long = 0L
        var listenedMs: Long = 0L
    }

    /** Listening session tracking */
    private var currentSessionId: Long = 0L
    private var sessionStartTime: Long = 0L
    private var pendingSession: PendingSession? = null
    private var lastSessionSaveTime: Long = 0L

    /** Timestamp of last periodic position save to the database */
    private var lastPositionSaveTime: Long = 0L

    // -------------------------------------------------------------------------
    // Player.Listener -- keeps PlaybackState in sync with the actual player
    // -------------------------------------------------------------------------

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                if (isResuming) {
                    isResuming = false
                    applyAutoRewind()
                }
                startListeningSession()
                startPositionUpdates()
            } else {
                endListeningSession()
                stopPositionUpdates()
                autoSavePosition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = mediaController ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(isLoading = true) }
                }
                Player.STATE_READY -> {
                    // If we connected before the service finished restoring its state,
                    // the initial sync was skipped (STATE_IDLE at connection time). Do a
                    // full sync now so episode metadata isn't left blank.
                    if (_playbackState.value.episodeId == 0L && controller.currentMediaItem != null) {
                        syncStateFromController(controller)
                    } else {
                        _playbackState.update {
                            it.copy(
                                isLoading = false,
                                duration = controller.duration.coerceAtLeast(0L),
                                currentPosition = controller.currentPosition.coerceAtLeast(0L),
                            )
                        }
                        refreshQueueFlags()
                    }
                }
                Player.STATE_ENDED -> {
                    handleEpisodeFinished()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(isLoading = false) }
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _playbackState.update {
                it.copy(
                    episodeTitle = mediaMetadata.title?.toString() ?: currentEpisodeTitle,
                    podcastTitle = mediaMetadata.artist?.toString() ?: currentPodcastTitle,
                    artworkUrl = mediaMetadata.artworkUri?.toString() ?: currentArtworkUrl,
                )
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    /**
     * Connects to the [PlaybackService] via a [MediaController].
     * Must be called once (typically from Application.onCreate or an Activity) before
     * any playback operations are invoked.
     *
     * Safe to call multiple times -- subsequent calls are no-ops if already connecting
     * or connected.
     */
    fun connectToService(context: Context) {
        if (mediaController != null || controllerFuture != null) return

        // Always use the application context: this PlaybackController is a Singleton
        // that outlives any individual Activity. If we bind the MediaController to an
        // Activity context, Android tears down that context's service-connection
        // dispatchers when the Activity is destroyed; a later release() (including
        // Media3's internal release on service disconnect) then crashes with
        // "Service not registered" inside LoadedApk.forgetServiceDispatcher.
        val appContext = context.applicationContext

        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )

        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(controller: MediaController) {
                    mediaController = controller
                    controller.addListener(playerListener)

                    // If the service was already playing (e.g., after config change), sync up.
                    if (controller.isPlaying || controller.playbackState == Player.STATE_READY) {
                        syncStateFromController(controller)
                        if (controller.isPlaying) {
                            startPositionUpdates()
                        }
                    } else if (controller.currentMediaItem == null) {
                        // Service was killed and restarted with no media loaded — restore
                        // the last in-progress episode from the database so the player
                        // UI isn't blank when the user returns after a long pause.
                        restoreStateFromDatabase()
                    }
                }

                override fun onFailure(t: Throwable) {
                    Log.e(TAG, "Failed to connect to PlaybackService", t)
                    mediaController = null
                    controllerFuture = null
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    // -------------------------------------------------------------------------
    // Playback controls
    // -------------------------------------------------------------------------

    /**
     * Starts playback of a specific episode.
     *
     * @param episodeId     Database primary key of the episode.
     * @param audioUrl      URL (or local file path) of the audio.
     * @param title         Episode title shown in the notification and UI.
     * @param podcastTitle  Podcast/show name.
     * @param artworkUrl    Artwork URL for the notification and UI.
     * @param startPosition Position in milliseconds to resume from (default 0).
     */
    fun play(
        episodeId: Long,
        audioUrl: String,
        title: String,
        podcastTitle: String,
        artworkUrl: String,
        startPosition: Long = 0L,
        podcastId: Long = 0L,
    ) {
        val controller = mediaController ?: return

        // Flush the outgoing episode's position before we overwrite state. Position is
        // otherwise only persisted on pause or via the ~10s periodic save, so switching
        // episodes mid-playback (which calls play() directly, without pausing) would lose
        // up to ~10s of the previous episode's progress and leave a stale resume point.
        val previous = _playbackState.value
        if (previous.episodeId != 0L && previous.episodeId != episodeId && previous.currentPosition > 0L) {
            val previousId = previous.episodeId
            val previousPosition = previous.currentPosition
            scope.launch {
                try {
                    episodeDao.updatePlaybackPosition(previousId, previousPosition)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save outgoing episode position", e)
                }
            }
        }

        // End any existing listening session before starting new playback
        endListeningSession()

        // Reset per-episode outro-skip tracking before the new episode's settings load,
        // so a stale value from the previous podcast can't end the new episode.
        currentSkipOutroSeconds = 0
        outroEndFired = false

        currentEpisodeId = episodeId
        currentPodcastId = podcastId
        currentAudioUrl = audioUrl
        currentArtworkUrl = artworkUrl
        currentPodcastTitle = podcastTitle
        currentEpisodeTitle = title

        _playbackState.update {
            it.copy(
                episodeId = episodeId,
                podcastId = podcastId,
                episodeTitle = title,
                podcastTitle = podcastTitle,
                artworkUrl = artworkUrl,
                audioUrl = audioUrl,
                currentPosition = startPosition,
                duration = 0L,
                isLoading = true,
                chapters = emptyList(),
                currentChapterIndex = -1,
            )
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(podcastTitle)
            .setArtworkUri(if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(episodeId.toString())
            .setUri(audioUrl)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem, startPosition)
        controller.prepare()
        controller.play()
        refreshQueueFlags()

        // Apply per-podcast speed immediately so it's correct from the first moment.
        // Cancel any prior speed-load and bail if the episode changed while loading,
        // so a rapid play()/auto-advance can't apply an earlier episode's speed and
        // can't clobber a user speed change made in the gap.
        speedLoadJob?.cancel()
        speedLoadJob = scope.launch {
            val speed = if (podcastId != 0L) {
                val podcastSpeed = podcastDao.getPlaybackSpeed(podcastId)
                if (podcastSpeed != null && podcastSpeed > 0f) podcastSpeed
                else preferencesManager.playbackSpeed.first()
            } else {
                preferencesManager.playbackSpeed.first()
            }
            if (currentEpisodeId == episodeId) {
                setPlaybackSpeed(speed)
                // Same load path also applies the per-podcast intro/outro auto-skip.
                // Living inside play() means both the direct play path and the queue
                // auto-advance path get it for free.
                applySkipSettings(podcastId, episodeId, startPosition)
            }
        }
    }

    /**
     * Loads the podcast's intro/outro auto-skip settings (AntennaPod's per-feed
     * "Skip introduction / ending" pattern) and applies the intro skip.
     *
     * Intro: only applied when the episode is starting from *before* the intro's end
     * ([startPosition] < skipIntro) — resuming an episode beyond the intro must not
     * yank the user back to (or forward past) their position. Mirrors AntennaPod's
     * guard of not skipping when the intro would cover the whole episode; an unknown
     * duration (<= 0, typical right after prepare()) is allowed through and the
     * player clamps the seek once the real duration is known.
     *
     * Outro: just records the setting; the periodic position loop ends the episode
     * via [shouldEndForOutro] once playback enters the outro window.
     */
    private suspend fun applySkipSettings(podcastId: Long, episodeId: Long, startPosition: Long) {
        if (podcastId == 0L) return
        val skip = try {
            podcastDao.getSkipSettings(podcastId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load skip settings", e)
            null
        } ?: return
        if (currentEpisodeId != episodeId) return

        currentSkipOutroSeconds = skip.skipOutroSeconds

        val skipIntroMs = skip.skipIntroSeconds * 1000L
        if (skipIntroMs <= 0L || startPosition >= skipIntroMs) return
        val controller = mediaController ?: return
        val durationMs = controller.duration
        if (durationMs > 0L && skipIntroMs >= durationMs) return
        controller.seekTo(skipIntroMs)
        _playbackState.update { it.copy(currentPosition = skipIntroMs) }
    }

    /**
     * Pauses the current playback and records the pause timestamp for auto-rewind.
     */
    fun pause() {
        mediaController?.pause()
        scope.launch {
            preferencesManager.setPausedAt(System.currentTimeMillis())
        }
    }

    /**
     * Resumes playback if paused. Sets the resuming flag so auto-rewind is applied.
     * If the service was killed and restarted (no media item loaded), re-sets up the
     * media item from the restored state before playing.
     */
    fun resume() {
        val controller = mediaController ?: return
        if (controller.currentMediaItem != null) {
            isResuming = true
            controller.play()
        } else if (currentEpisodeId != 0L) {
            // Service was reset but we restored episode info from DB — re-setup the media
            play(
                episodeId = currentEpisodeId,
                audioUrl = currentAudioUrl,
                title = currentEpisodeTitle,
                podcastTitle = currentPodcastTitle,
                artworkUrl = currentArtworkUrl,
                startPosition = _playbackState.value.currentPosition,
                podcastId = currentPodcastId,
            )
        }
    }

    /**
     * Seeks to the given position in milliseconds.
     */
    fun seekTo(position: Long) {
        val controller = mediaController ?: return
        // duration is C.TIME_UNSET (negative) while buffering or for streams with no
        // known length. Only clamp to the upper bound when the duration is known;
        // otherwise clamping to 0 would jump the user to the very start.
        val duration = controller.duration
        val clamped = if (duration > 0L) position.coerceIn(0L, duration) else position.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _playbackState.update { it.copy(currentPosition = clamped) }
    }

    /**
     * Skips forward by the given number of seconds (default 30).
     */
    fun skipForward(seconds: Int = 30) {
        val controller = mediaController ?: return
        // computeSkipTarget caps a second short of the end so a forward skip can't land
        // on the duration and drive the player to STATE_ENDED (which would mark the
        // episode played). When duration is unknown (C.TIME_UNSET) it returns the raw
        // target and the player clamps to the real end once it's known. Shared with the
        // notification skip in PlaybackService.seekByOffset().
        val newPos = computeSkipTarget(controller.currentPosition, seconds * 1000L, controller.duration)
        controller.seekTo(newPos)
        _playbackState.update { it.copy(currentPosition = newPos) }
    }

    /**
     * Skips backward by the given number of seconds (default 10).
     */
    fun skipBack(seconds: Int = 10) {
        val controller = mediaController ?: return
        val newPos = computeSkipTarget(controller.currentPosition, -seconds * 1000L, controller.duration)
        controller.seekTo(newPos)
        _playbackState.update { it.copy(currentPosition = newPos) }
    }

    /**
     * Sets the playback speed (e.g., 0.5f, 1.0f, 1.5f, 2.0f).
     * Clamped to the range [0.25, 3.0].
     */
    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        val clamped = speed.coerceIn(0.25f, 3.0f)
        controller.setPlaybackParameters(PlaybackParameters(clamped))
        _playbackState.update { it.copy(playbackSpeed = clamped) }
    }

    /**
     * Enables or disables silence skipping via a custom session command
     * handled by [PlaybackService].
     */
    @OptIn(UnstableApi::class)
    fun setSkipSilence(enabled: Boolean) {
        val controller = mediaController ?: return
        val extras = Bundle().apply { putBoolean("enabled", enabled) }
        val command = SessionCommand(
            PlaybackService.CUSTOM_COMMAND_SET_SKIP_SILENCE,
            extras,
        )
        controller.sendCustomCommand(command, Bundle.EMPTY)
        // Update local state so the UI reflects the change immediately
        _playbackState.update { it.copy(skipSilence = enabled) }
    }

    /**
     * Enables or disables volume boost via a custom session command
     * handled by [PlaybackService] using Android's LoudnessEnhancer.
     */
    @OptIn(UnstableApi::class)
    fun setVolumeBoost(enabled: Boolean) {
        val controller = mediaController ?: return
        val extras = Bundle().apply { putBoolean("enabled", enabled) }
        val command = SessionCommand(
            PlaybackService.CUSTOM_COMMAND_SET_VOLUME_BOOST,
            extras,
        )
        controller.sendCustomCommand(command, Bundle.EMPTY)
        // Update local state so the UI reflects the change immediately
        _playbackState.update { it.copy(volumeBoost = enabled) }
    }

    /**
     * Stops playback completely, clears the current media item, and resets state.
     */
    fun stop() {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        stopPositionUpdates()

        currentEpisodeId = 0L
        currentAudioUrl = ""
        currentArtworkUrl = ""
        currentPodcastTitle = ""
        currentEpisodeTitle = ""
        currentSkipOutroSeconds = 0

        _playbackState.value = PlaybackState()
    }

    /**
     * Arms or disarms "pause at end of episode" mode. When armed, the player stops
     * at the end of the current episode instead of auto-advancing the queue.
     * Used by the sleep timer's end-of-episode mode.
     */
    fun setPauseAtEpisodeEnd(enabled: Boolean) {
        pauseAtEpisodeEnd = enabled
    }

    /**
     * Advances to the next episode in the playback queue.
     * If the queue is empty, playback stops.
     */
    fun playNext() {
        scope.launch {
            advanceQueue()
        }
    }

    /**
     * Releases the [MediaController] and cancels all coroutines.
     * Call when the controller is no longer needed (e.g., the app process is finishing).
     */
    fun release() {
        stopPositionUpdates()

        mediaController?.removeListener(playerListener)
        mediaController = null

        // Use releaseFuture so that an in-flight buildAsync is cancelled cleanly
        // rather than leaking a half-built controller. Wrap in try/catch because
        // Media3 can throw IllegalArgumentException("Service not registered") if
        // the underlying service binding was already torn down (a known race —
        // see androidx/media issue #239).
        controllerFuture?.let { future ->
            try {
                MediaController.releaseFuture(future)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaController", e)
            }
        }
        controllerFuture = null

        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Queue helpers
    // -------------------------------------------------------------------------

    /**
     * Shared natural-end path: runs when the current episode finishes, either because
     * the player reached [Player.STATE_ENDED] or because the per-podcast outro-skip
     * window was entered (see [shouldEndForOutro]). Marks the episode played, emits
     * [episodeEnded], and either stops (sleep timer armed) or auto-advances the queue.
     */
    private fun handleEpisodeFinished() {
        val controller = mediaController ?: return

        // Explicitly clear playWhenReady so the player cannot be
        // accidentally restarted by external controllers or media
        // button events while we decide what to do next.
        controller.playWhenReady = false

        // Mark the finished episode as played before clearing state
        val finishedEpisodeId = currentEpisodeId
        if (finishedEpisodeId != 0L) {
            scope.launch {
                try {
                    episodeDao.markAsPlayed(finishedEpisodeId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to mark episode as played", e)
                }
            }
        }

        _playbackState.update {
            it.copy(
                isPlaying = false,
                isLoading = false,
                currentPosition = 0L,
            )
        }
        stopPositionUpdates()

        // Notify observers (e.g. the end-of-episode sleep timer) that the
        // episode actually finished. This is a real signal rather than
        // inferring from currentPosition, which is reset to 0 above.
        _episodeEnded.tryEmit(Unit)

        if (pauseAtEpisodeEnd) {
            // End-of-episode sleep timer is armed: stop here instead of
            // auto-advancing to the next queued episode. Drop the player to
            // IDLE (as we do after a normal end) so a Bluetooth/car play
            // command can't restart the finished episode from 0.
            pauseAtEpisodeEnd = false
            stopPlayerAfterEnded()
        } else {
            // Automatically advance to the next queued episode
            onEpisodeEnded()
        }
    }

    /**
     * Called when the current episode reaches [Player.STATE_ENDED].
     * Removes the finished episode from the queue and starts the next one.
     */
    private fun onEpisodeEnded() {
        scope.launch {
            advanceQueue()
        }
    }

    /**
     * Fetches the next item from the queue, removes the current one, and starts
     * playback of the next episode. If there is nothing in the queue, playback stops.
     *
     * Looks up the actual podcast title from the database for each episode,
     * since queue items may come from different podcasts.
     */
    private suspend fun advanceQueue() {
        // Only auto-advance if queue feature is enabled
        val queueEnabled = preferencesManager.queueEnabled.first()
        if (!queueEnabled) {
            stopPlayerAfterEnded()
            return
        }

        try {
            val finishedEpisodeId = currentEpisodeId

            // Determine the next episode based on the finished episode's *position*
            // in the queue, not simply the lowest-position item. The finished episode
            // may not be the queue head (e.g. the user tapped a later item to play it),
            // so blindly taking the head would replay an earlier episode and skip the
            // correct next one.
            val queue = queueDao.getQueueOnce()
            val currentIndex = queue.indexOfFirst { it.episode.id == finishedEpisodeId }
            val next = when {
                // Finished episode is in the queue: take the item after it.
                currentIndex in 0 until queue.size - 1 -> queue[currentIndex + 1]
                // Finished episode is the last item: nothing to advance to.
                currentIndex == queue.size - 1 -> null
                // Finished episode isn't in the queue (played from outside it):
                // fall back to starting the queue from its head.
                else -> queue.firstOrNull()
            }

            // Remove the episode that just finished from the queue.
            if (finishedEpisodeId != 0L) {
                queueDao.removeFromQueue(finishedEpisodeId)
            }

            if (next != null && next.episode.id != finishedEpisodeId) {
                val episode = next.episode

                // Look up the actual podcast title from the database
                val podcastTitle = podcastDao.getByIdOnce(episode.podcastId)?.title ?: ""

                play(
                    episodeId = episode.id,
                    // Prefer the downloaded file (download-first); fall back to the remote
                    // URL only if not downloaded — matching every other play() call site.
                    // Streaming the remote URL here would break offline auto-advance and
                    // waste mobile data for an episode the user explicitly downloaded.
                    audioUrl = episode.downloadPath.ifBlank { episode.audioUrl },
                    title = episode.title,
                    podcastTitle = podcastTitle,
                    artworkUrl = episode.artworkUrl,
                    startPosition = episode.playbackPosition,
                    podcastId = episode.podcastId,
                )
            } else {
                // Nothing left in queue
                _playbackState.update {
                    it.copy(hasNext = false, hasPrevious = false)
                }
                stopPlayerAfterEnded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to advance queue", e)
        }
    }

    // A STATE_ENDED player still has the finished media item loaded, so any
    // play() from a Bluetooth headset, car head unit, or system notification
    // seeks back to 0 and restarts the same episode. Dropping to IDLE prevents
    // that.
    private fun stopPlayerAfterEnded() {
        val controller = mediaController ?: return
        controller.clearMediaItems()
        controller.stop()
        stopPositionUpdates()

        // Reset state so the just-finished episode doesn't linger in the mini-player at
        // 0:00 (where tapping play would restart the already-played episode). Mirrors
        // stop(); without this the cleared media item leaves episodeId/title populated.
        currentEpisodeId = 0L
        currentPodcastId = 0L
        currentAudioUrl = ""
        currentArtworkUrl = ""
        currentPodcastTitle = ""
        currentEpisodeTitle = ""
        currentSkipOutroSeconds = 0

        _playbackState.value = PlaybackState()
    }

    /**
     * Checks the queue to determine whether there is a next/previous episode and
     * updates the [PlaybackState] flags accordingly.
     */
    private fun refreshQueueFlags() {
        scope.launch {
            try {
                val queue = queueDao.getQueueOnce()
                val currentIndex = queue.indexOfFirst { it.episode.id == currentEpisodeId }
                _playbackState.update {
                    it.copy(
                        hasNext = currentIndex >= 0 && currentIndex < queue.size - 1,
                        hasPrevious = currentIndex > 0,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh queue flags", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Position auto-save
    // -------------------------------------------------------------------------

    /**
     * Saves the current playback position to the database.
     * Called automatically when playback pauses.
     */
    private fun autoSavePosition() {
        val state = _playbackState.value
        if (state.episodeId != 0L && state.currentPosition > 0L) {
            scope.launch {
                try {
                    episodeDao.updatePlaybackPosition(state.episodeId, state.currentPosition)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to auto-save position", e)
                }
            }
        }
    }

    /**
     * Periodically saves the playback position to the database during active
     * playback. This ensures the Continue Listening carousel shows an accurate
     * resume point even if the app is killed without an explicit pause.
     * Saves at most once every 10 seconds to avoid excessive DB writes.
     */
    private fun periodicSavePosition() {
        val state = _playbackState.value
        if (state.episodeId == 0L || state.currentPosition <= 0L) return
        val now = System.currentTimeMillis()
        if (now - lastPositionSaveTime < 10_000L) return
        lastPositionSaveTime = now
        scope.launch {
            try {
                episodeDao.updatePlaybackPosition(state.episodeId, state.currentPosition)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to periodically save position", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Listening session tracking
    // -------------------------------------------------------------------------

    private fun startListeningSession() {
        if (currentEpisodeId == 0L) return
        val startTime = System.currentTimeMillis()
        sessionStartTime = startTime
        lastSessionSaveTime = startTime
        currentSessionId = 0L
        // Fresh holder for this session; the insert continuation below captures it, so a
        // later session starting mid-insert can't clobber this one's end-of-session stash.
        val pending = PendingSession()
        pendingSession = pending
        val episodeId = currentEpisodeId
        scope.launch {
            try {
                // Look up podcastId if we don't have it
                val podcastId = if (currentPodcastId != 0L) currentPodcastId else {
                    episodeDao.getByIdOnce(episodeId)?.podcastId ?: 0L
                }
                currentPodcastId = podcastId
                val session = ListeningSessionEntity(
                    episodeId = episodeId,
                    podcastId = podcastId,
                    startedAt = startTime,
                    playbackSpeed = _playbackState.value.playbackSpeed,
                )
                val insertedId = listeningSessionDao.insert(session)

                if (pending.ended) {
                    // This session was ended before its insert completed. Finalize its
                    // own row now so endedAt/listenedMs are not lost (previously this
                    // could leave an orphaned, never-updated session).
                    listeningSessionDao.updateSession(insertedId, pending.endTime, pending.listenedMs)
                } else if (pendingSession === pending) {
                    // Still the active session — record its id. The identity check guards
                    // against a newer session having superseded this one in the meantime.
                    currentSessionId = insertedId
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start listening session", e)
            }
        }
    }

    private fun endListeningSession() {
        if (sessionStartTime == 0L) return
        val endTime = System.currentTimeMillis()
        val listenedMs = endTime - sessionStartTime
        val sessionId = currentSessionId
        sessionStartTime = 0L
        lastSessionSaveTime = 0L
        if (sessionId != 0L) {
            currentSessionId = 0L
            scope.launch {
                try {
                    listeningSessionDao.updateSession(sessionId, endTime, listenedMs)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to end listening session", e)
                }
            }
        } else {
            // Insert still in flight: mark this session's holder ended so its own insert
            // continuation finalizes the row once it has a real id.
            pendingSession?.let {
                it.ended = true
                it.endTime = endTime
                it.listenedMs = listenedMs
            }
        }
    }

    /**
     * Periodically saves the current listening session's accumulated time to the
     * database. This ensures listening time is not lost if the app is killed.
     */
    private fun saveSessionProgress() {
        val sessionId = currentSessionId
        val startTime = sessionStartTime
        if (sessionId == 0L || startTime == 0L) return
        val now = System.currentTimeMillis()
        // Save at most once every 30 seconds to avoid excessive DB writes
        if (now - lastSessionSaveTime < 30_000L) return
        lastSessionSaveTime = now
        val listenedMs = now - startTime
        scope.launch {
            try {
                listeningSessionDao.updateSession(sessionId, now, listenedMs)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save session progress", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chapter navigation
    // -------------------------------------------------------------------------

    fun seekToNextChapter() {
        val state = _playbackState.value
        if (state.chapters.isEmpty()) return
        val nextIndex = state.currentChapterIndex + 1
        if (nextIndex < state.chapters.size) {
            seekTo(state.chapters[nextIndex].startTimeMs)
        }
    }

    fun seekToPreviousChapter() {
        val state = _playbackState.value
        if (state.chapters.isEmpty()) return
        val prevIndex = (state.currentChapterIndex - 1).coerceAtLeast(0)
        seekTo(state.chapters[prevIndex].startTimeMs)
    }

    fun seekToChapter(index: Int) {
        val state = _playbackState.value
        if (index in state.chapters.indices) {
            seekTo(state.chapters[index].startTimeMs)
        }
    }

    // -------------------------------------------------------------------------
    // Auto-rewind (AntennaPod pattern)
    // -------------------------------------------------------------------------

    /**
     * Calculates and applies an auto-rewind based on how long playback was paused.
     * Pattern from AntennaPod:
     * - <1 min pause = 0s rewind
     * - 1-60 min = 3s rewind
     * - 1-24 hr = 10s rewind
     * - >24 hr = 20s rewind
     */
    private fun applyAutoRewind() {
        scope.launch {
            try {
                val pausedAt = preferencesManager.pausedAt.first()
                if (pausedAt <= 0L) return@launch

                val pauseDurationMs = System.currentTimeMillis() - pausedAt
                val rewindSeconds = when {
                    pauseDurationMs < 60_000L -> 0        // <1 min
                    pauseDurationMs < 3_600_000L -> 3      // 1-60 min
                    pauseDurationMs < 86_400_000L -> 10    // 1-24 hr
                    else -> 20                              // >24 hr
                }
                if (rewindSeconds > 0) {
                    val controller = mediaController ?: return@launch
                    val newPos = (controller.currentPosition - rewindSeconds * 1000L).coerceAtLeast(0L)
                    controller.seekTo(newPos)
                    _playbackState.update { it.copy(currentPosition = newPos) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply auto-rewind", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Position update loop
    // -------------------------------------------------------------------------

    /**
     * Starts a coroutine that polls the [MediaController] for the current position
     * every 250ms and updates [playbackState].
     */
    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return

        positionUpdateJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val durationMs = controller.duration.coerceAtLeast(0L)
                    _playbackState.update { state ->
                        val chapterIndex = state.chapters.indexOfLast { pos >= it.startTimeMs }
                        state.copy(
                            currentPosition = pos,
                            duration = durationMs,
                            currentChapterIndex = chapterIndex,
                        )
                    }
                    saveSessionProgress()
                    periodicSavePosition()

                    // Per-podcast outro-skip: once playback enters the configured
                    // window before the end, treat the episode as finished (mark
                    // played + advance the queue — the same path STATE_ENDED takes).
                    // outroEndFired guards against firing more than once per episode.
                    if (shouldEndForOutro(pos, durationMs, currentSkipOutroSeconds, outroEndFired)) {
                        outroEndFired = true
                        handleEpisodeFinished()
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // -------------------------------------------------------------------------
    // Internal state sync
    // -------------------------------------------------------------------------

    /**
     * Restores the last in-progress episode from the database into [playbackState].
     * Called when the service was killed and restarted with no media loaded, so the
     * player UI shows the last episode (paused) rather than a blank screen.
     * The media item is also prepared in the controller so that [resume] works
     * immediately when the user taps Play.
     */
    private fun restoreStateFromDatabase() {
        scope.launch {
            try {
                val episode = episodeDao.getLastInProgressEpisode() ?: return@launch
                val podcast = podcastDao.getByIdOnce(episode.podcastId)
                val podcastTitle = podcast?.title ?: ""
                val effectiveUrl = if (episode.downloadPath.isNotBlank()) episode.downloadPath
                                   else episode.audioUrl

                val podcastSpeed = podcastDao.getPlaybackSpeed(episode.podcastId)
                val speed = if (podcastSpeed != null && podcastSpeed > 0f) podcastSpeed
                            else preferencesManager.playbackSpeed.first()

                // Re-load the outro-skip so a resumed episode still honours it. The
                // intro skip is deliberately NOT re-applied here: this restores an
                // in-progress episode, and resuming must never move the position.
                outroEndFired = false
                currentSkipOutroSeconds =
                    podcastDao.getSkipSettings(episode.podcastId)?.skipOutroSeconds ?: 0

                currentEpisodeId = episode.id
                currentPodcastId = episode.podcastId
                currentEpisodeTitle = episode.title
                currentPodcastTitle = podcastTitle
                currentArtworkUrl = episode.artworkUrl
                currentAudioUrl = effectiveUrl

                _playbackState.update {
                    it.copy(
                        episodeId = episode.id,
                        podcastId = episode.podcastId,
                        episodeTitle = episode.title,
                        podcastTitle = podcastTitle,
                        artworkUrl = episode.artworkUrl,
                        audioUrl = effectiveUrl,
                        currentPosition = episode.playbackPosition,
                        duration = (episode.durationSeconds * 1000L).coerceAtLeast(0L),
                        isPlaying = false,
                        isLoading = false,
                        playbackSpeed = speed,
                    )
                }

                // Prepare the media item in the controller (not playing) so that the
                // user can tap Play and have it resume without navigating away.
                val controller = mediaController ?: return@launch
                val metadata = MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(podcastTitle)
                    .setArtworkUri(
                        if (episode.artworkUrl.isNotBlank()) Uri.parse(episode.artworkUrl)
                        else null
                    )
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaId(episode.id.toString())
                    .setUri(effectiveUrl)
                    .setMediaMetadata(metadata)
                    .build()
                controller.setMediaItem(mediaItem, episode.playbackPosition)
                controller.prepare()
                controller.setPlaybackParameters(PlaybackParameters(speed))
                // playWhenReady remains false — user must explicitly tap Play.

                refreshQueueFlags()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore state from database", e)
            }
        }
    }

    /**
     * Synchronises [PlaybackState] from the current [MediaController] state.
     * Used when reconnecting to an already-running service.
     */
    private fun syncStateFromController(controller: MediaController) {
        val metadata = controller.mediaMetadata
        val mediaId = controller.currentMediaItem?.mediaId
        val episodeId = mediaId?.toLongOrNull() ?: 0L

        currentEpisodeId = episodeId
        currentEpisodeTitle = metadata.title?.toString() ?: ""
        currentPodcastTitle = metadata.artist?.toString() ?: ""
        currentArtworkUrl = metadata.artworkUri?.toString() ?: ""

        _playbackState.update {
            it.copy(
                episodeId = episodeId,
                episodeTitle = currentEpisodeTitle,
                podcastTitle = currentPodcastTitle,
                artworkUrl = currentArtworkUrl,
                audioUrl = controller.currentMediaItem?.localConfiguration?.uri?.toString() ?: "",
                isPlaying = controller.isPlaying,
                isLoading = controller.playbackState == Player.STATE_BUFFERING,
                currentPosition = controller.currentPosition.coerceAtLeast(0L),
                duration = controller.duration.coerceAtLeast(0L),
                playbackSpeed = controller.playbackParameters.speed,
            )
        }

        refreshQueueFlags()
    }
}
