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
import com.google.common.util.concurrent.MoreExecutors
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {

    companion object {
        private const val TAG = "PlaybackController"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var positionUpdateJob: Job? = null

    /** The episode ID currently being played, tracked locally for metadata purposes. */
    private var currentEpisodeId: Long = 0L
    private var currentAudioUrl: String = ""
    private var currentArtworkUrl: String = ""
    private var currentPodcastTitle: String = ""
    private var currentEpisodeTitle: String = ""

    // -------------------------------------------------------------------------
    // Player.Listener -- keeps PlaybackState in sync with the actual player
    // -------------------------------------------------------------------------

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
                // Auto-save position when playback pauses
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
                    _playbackState.update {
                        it.copy(
                            isLoading = false,
                            duration = controller.duration.coerceAtLeast(0L),
                            currentPosition = controller.currentPosition.coerceAtLeast(0L),
                        )
                    }
                    refreshQueueFlags()
                }
                Player.STATE_ENDED -> {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            isLoading = false,
                            currentPosition = 0L,
                        )
                    }
                    stopPositionUpdates()
                    // Automatically advance to the next queued episode
                    onEpisodeEnded()
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
     * Safe to call multiple times -- subsequent calls are no-ops if already connected.
     */
    fun connectToService(context: Context) {
        if (mediaController != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        Futures.addCallback(
            controllerFuture,
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
                    }
                }

                override fun onFailure(t: Throwable) {
                    Log.e(TAG, "Failed to connect to PlaybackService", t)
                    mediaController = null
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
    ) {
        val controller = mediaController ?: return

        currentEpisodeId = episodeId
        currentAudioUrl = audioUrl
        currentArtworkUrl = artworkUrl
        currentPodcastTitle = podcastTitle
        currentEpisodeTitle = title

        _playbackState.update {
            it.copy(
                episodeId = episodeId,
                episodeTitle = title,
                podcastTitle = podcastTitle,
                artworkUrl = artworkUrl,
                audioUrl = audioUrl,
                currentPosition = startPosition,
                duration = 0L,
                isLoading = true,
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

        controller.setMediaItem(mediaItem)
        controller.prepare()

        if (startPosition > 0L) {
            controller.seekTo(startPosition)
        }

        controller.play()
        refreshQueueFlags()
    }

    /**
     * Pauses the current playback.
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * Resumes playback if paused.
     */
    fun resume() {
        mediaController?.play()
    }

    /**
     * Seeks to the given position in milliseconds.
     */
    fun seekTo(position: Long) {
        val controller = mediaController ?: return
        val clamped = position.coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(clamped)
        _playbackState.update { it.copy(currentPosition = clamped) }
    }

    /**
     * Skips forward by the given number of seconds (default 30).
     */
    fun skipForward(seconds: Int = 30) {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition + seconds * 1000L)
            .coerceAtMost(controller.duration.coerceAtLeast(0L))
        controller.seekTo(newPos)
        _playbackState.update { it.copy(currentPosition = newPos) }
    }

    /**
     * Skips backward by the given number of seconds (default 10).
     */
    fun skipBack(seconds: Int = 10) {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition - seconds * 1000L).coerceAtLeast(0L)
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

        _playbackState.value = PlaybackState()
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

        mediaController?.run {
            removeListener(playerListener)
            release()
        }
        mediaController = null
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Queue helpers
    // -------------------------------------------------------------------------

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
        try {
            // Remove the episode that just finished from the queue
            if (currentEpisodeId != 0L) {
                queueDao.removeFromQueue(currentEpisodeId)
            }

            val next = queueDao.getNextInQueue()
            if (next != null) {
                val episode = next.episode

                // Look up the actual podcast title from the database
                val podcastTitle = podcastDao.getByIdOnce(episode.podcastId)?.title ?: ""

                play(
                    episodeId = episode.id,
                    audioUrl = episode.audioUrl,
                    title = episode.title,
                    podcastTitle = podcastTitle,
                    artworkUrl = episode.artworkUrl,
                    startPosition = episode.playbackPosition,
                )
            } else {
                // Nothing left in queue
                _playbackState.update {
                    it.copy(hasNext = false, hasPrevious = false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to advance queue", e)
        }
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
                    _playbackState.update {
                        it.copy(
                            currentPosition = controller.currentPosition.coerceAtLeast(0L),
                            duration = controller.duration.coerceAtLeast(0L),
                        )
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
