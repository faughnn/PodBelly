package com.podbelly.core.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import javax.inject.Inject

/**
 * A [MediaLibraryService] that manages audio playback for podcast episodes and serves
 * the media browse tree to Android Auto and other MediaBrowser clients.
 *
 * Handles:
 * - Background playback with a persistent notification
 * - Media session for system integration (lock screen, Bluetooth, Android Auto, etc.)
 * - The Android Auto browse tree (Queue + Podcasts folders, see [BrowseTree])
 * - Resolving browse-tree media ids to playable items (download-first: only
 *   downloaded episodes are playable from Auto)
 * - Custom session commands for skip-silence and volume-boost toggling
 * - Audio focus management
 * - Wake lock for network streaming
 *
 * Modelled after Pocket Casts' PlaybackService architecture and the androidx media3
 * session demo's MediaLibraryService/MediaItemTree pattern.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    companion object {
        private const val TAG = "PlaybackService"
        const val CUSTOM_COMMAND_SET_SKIP_SILENCE = "SET_SKIP_SILENCE"
        const val CUSTOM_COMMAND_SET_VOLUME_BOOST = "SET_VOLUME_BOOST"
        const val CUSTOM_COMMAND_REWIND = "REWIND_10S"
        const val CUSTOM_COMMAND_FAST_FORWARD = "FAST_FORWARD_30S"
        private const val NOTIFICATION_CHANNEL_ID = "podbelly_playback"
    }

    @Inject
    lateinit var episodeDao: EpisodeDao

    @Inject
    lateinit var podcastDao: PodcastDao

    @Inject
    lateinit var queueDao: QueueDao

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    /**
     * Scope for browse-tree queries and item resolution. Main.immediate matches the
     * session callback threading model; Room suspend queries hop to their own IO
     * executor internally.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Podcast episodes should never loop. Explicitly disable repeat so that
        // external controllers (Bluetooth, Android Auto, system media UI) cannot
        // accidentally enable it and cause an episode to restart after finishing.
        player.repeatMode = Player.REPEAT_MODE_OFF

        exoPlayer = player

        // Session activity: tapping the notification opens the app
        val sessionActivityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .apply {
                sessionActivityIntent?.let { setSessionActivity(it) }
            }
            .build()
    }

    /**
     * Seeks [player] by [offsetMs] relative to its current position for the
     * notification / lock-screen rewind and fast-forward buttons.
     *
     * Rapidly tapping these buttons used to crash the app: a forward skip could land
     * exactly on the duration, driving the player to [Player.STATE_ENDED], which
     * clears the media item — the next tap then called [Player.seekTo] on an empty
     * timeline and threw IllegalSeekPositionException. It also made the episode reopen
     * as "completed" because STATE_ENDED marks it played.
     *
     * Guards here:
     * - Skip entirely when there is no seekable media (IDLE / ENDED / no current item,
     *   or the seek command isn't available for this controller).
     * - [computeSkipTarget] caps a forward skip a second short of the end so it can't
     *   trigger STATE_ENDED.
     * - The [Player.seekTo] call is wrapped defensively so a lost race (media cleared
     *   between the guard and the seek) is logged instead of crashing.
     */
    private fun seekByOffset(player: Player, offsetMs: Long) {
        if (player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED ||
            player.currentMediaItem == null ||
            !player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        ) {
            return
        }
        val newPos = computeSkipTarget(player.currentPosition, offsetMs, player.duration)
        try {
            player.seekTo(newPos)
        } catch (e: Exception) {
            Log.w(TAG, "Ignored notification seek to $newPos", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseLoudnessEnhancer()
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        exoPlayer = null
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // MediaLibrarySession callback: connection, custom commands, browse tree
    // -------------------------------------------------------------------------

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        private val rewindCommand = SessionCommand(CUSTOM_COMMAND_REWIND, Bundle.EMPTY)
        private val rewindButton = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName("Rewind 10s")
            .setSessionCommand(rewindCommand)
            .build()

        private val fastForwardCommand = SessionCommand(CUSTOM_COMMAND_FAST_FORWARD, Bundle.EMPTY)
        private val fastForwardButton = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setDisplayName("Fast forward 30s")
            .setSessionCommand(fastForwardCommand)
            .build()

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // DEFAULT_SESSION_AND_LIBRARY_COMMANDS (not DEFAULT_SESSION_COMMANDS):
            // without the library commands Android Auto cannot browse at all.
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_COMMAND_SET_SKIP_SILENCE, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_COMMAND_SET_VOLUME_BOOST, Bundle.EMPTY))
                    .add(rewindCommand)
                    .add(fastForwardCommand)
                    .build()

            // Prevent external controllers from enabling repeat or shuffle —
            // podcast episodes should never loop.
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .remove(Player.COMMAND_SET_REPEAT_MODE)
                .remove(Player.COMMAND_SET_SHUFFLE_MODE)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setCustomLayout(listOf(rewindButton, fastForwardButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_SET_SKIP_SILENCE -> {
                    val enabled = customCommand.customExtras.getBoolean("enabled", false)
                    exoPlayer?.skipSilenceEnabled = enabled
                    Log.d(TAG, "Skip silence set to $enabled")
                }
                CUSTOM_COMMAND_SET_VOLUME_BOOST -> {
                    val enabled = customCommand.customExtras.getBoolean("enabled", false)
                    applyVolumeBoost(enabled)
                    Log.d(TAG, "Volume boost set to $enabled")
                }
                CUSTOM_COMMAND_REWIND -> {
                    session.player.let { player -> seekByOffset(player, -10_000L) }
                    Log.d(TAG, "Rewound 10 seconds")
                }
                CUSTOM_COMMAND_FAST_FORWARD -> {
                    session.player.let { player -> seekByOffset(player, 30_000L) }
                    Log.d(TAG, "Fast forwarded 30 seconds")
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        // ---------------------------------------------------------------------
        // Browse tree (Android Auto)
        // ---------------------------------------------------------------------

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.rootItem(), params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            try {
                val children: List<MediaItem>? = when {
                    parentId == BrowseTree.ROOT_ID ->
                        BrowseTree.rootChildren(preferencesManager.queueEnabled.first())
                    parentId == BrowseTree.QUEUE_ID -> queueChildren()
                    parentId == BrowseTree.PODCASTS_ID -> podcastFolders()
                    else -> BrowseTree.parsePodcastId(parentId)?.let { podcastEpisodes(it) }
                }
                if (children != null) {
                    LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
                } else {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load browse children for $parentId", e)
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = serviceScope.future {
            try {
                val item: MediaItem? = when {
                    mediaId == BrowseTree.ROOT_ID -> BrowseTree.rootItem()
                    mediaId == BrowseTree.QUEUE_ID -> BrowseTree.queueFolderItem()
                    mediaId == BrowseTree.PODCASTS_ID -> BrowseTree.podcastsFolderItem()
                    BrowseTree.parsePodcastId(mediaId) != null ->
                        podcastDao.getByIdOnce(BrowseTree.parsePodcastId(mediaId)!!)
                            ?.let { BrowseTree.podcastItem(it) }
                    else -> BrowseTree.parseEpisodeId(mediaId)?.let { episodeId ->
                        episodeDao.getByIdOnce(episodeId)?.let { episode ->
                            val podcast = podcastDao.getByIdOnce(episode.podcastId)
                            BrowseTree.episodeBrowseItem(
                                episode,
                                podcast?.title ?: "",
                                podcast?.artworkUrl ?: "",
                            )
                        }
                    }
                }
                if (item != null) {
                    LibraryResult.ofItem(item, /* params= */ null)
                } else {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load browse item $mediaId", e)
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
            }
        }

        // ---------------------------------------------------------------------
        // Item resolution (playing from Android Auto)
        // ---------------------------------------------------------------------

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = serviceScope.future {
            resolveMediaItems(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceScope.future {
            val resolved = resolveMediaItems(mediaItems)

            // An unspecified start position marks playback initiated from a browse
            // client (Android Auto double-tap): the app's own play() path always
            // passes an explicit position. Resume from the saved position and apply
            // the per-podcast playback defaults (speed + intro skip) that
            // PlaybackController.play() would otherwise apply.
            if (startPositionMs == C.TIME_UNSET && resolved.size == 1) {
                val episodeId = BrowseTree.parseEpisodeId(resolved[0].mediaId)
                val episode = episodeId?.let { runCatching { episodeDao.getByIdOnce(it) }.getOrNull() }
                if (episode != null) {
                    applyPodcastPlaybackSpeed(episode.podcastId)
                    val skipIntroSeconds = runCatching {
                        podcastDao.getSkipSettings(episode.podcastId)?.skipIntroSeconds
                    }.getOrNull() ?: 0
                    val startPos = resolveExternalStartPosition(
                        savedPositionMs = episode.playbackPosition,
                        durationMs = episode.durationSeconds * 1000L,
                        skipIntroSeconds = skipIntroSeconds,
                    )
                    return@future MediaSession.MediaItemsWithStartPosition(resolved, 0, startPos)
                }
            }

            val safeIndex = if (startIndex == C.INDEX_UNSET || resolved.isEmpty()) {
                startIndex
            } else {
                startIndex.coerceIn(0, resolved.size - 1)
            }
            MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs)
        }
    }

    // -------------------------------------------------------------------------
    // Browse tree data loading
    // -------------------------------------------------------------------------

    /** Queue folder: queued episodes, downloaded ones only (download-first). */
    private suspend fun queueChildren(): List<MediaItem> {
        val queue = queueDao.getQueueOnce()
        val podcasts = queue.map { it.episode.podcastId }.distinct()
            .mapNotNull { podcastDao.getByIdOnce(it) }
            .associateBy { it.id }
        return queue.mapNotNull { entry ->
            val episode = entry.episode
            if (episode.downloadPath.isBlank()) return@mapNotNull null
            val podcast = podcasts[episode.podcastId]
            BrowseTree.episodeBrowseItem(
                episode,
                podcast?.title ?: "",
                podcast?.artworkUrl ?: "",
            )
        }
    }

    /** Podcasts folder: subscribed shows that have at least one downloaded episode. */
    private suspend fun podcastFolders(): List<MediaItem> {
        val withDownloads = episodeDao.getPodcastIdsWithDownloads().toSet()
        return podcastDao.getAll().first()
            .filter { it.id in withDownloads }
            .map { BrowseTree.podcastItem(it) }
    }

    /** One podcast's folder: its downloaded episodes, newest first. */
    private suspend fun podcastEpisodes(podcastId: Long): List<MediaItem> {
        val podcast = podcastDao.getByIdOnce(podcastId) ?: return emptyList()
        return episodeDao.getDownloadedByPodcastIdOnce(podcastId)
            .map { BrowseTree.episodeBrowseItem(it, podcast.title, podcast.artworkUrl) }
    }

    /**
     * Resolves incoming MediaItems to fully populated playable items.
     *
     * - Items that already carry a URI (the app's own play() path when the URI
     *   survives the controller/session hop) pass through untouched.
     * - Browse-tree ids ("episode_{id}", i.e. picked from Android Auto) are looked
     *   up in the database and must be downloaded — download-first, undownloaded
     *   episodes are silently dropped (they are never listed as playable anyway).
     * - Plain numeric ids (the app's convention) fall back to the remote stream URL
     *   when there is no download, matching PlaybackController's behaviour.
     */
    private suspend fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        val resolved = mutableListOf<MediaItem>()
        for (item in mediaItems) {
            if (item.localConfiguration != null) {
                resolved += item
                continue
            }
            val episodeId = BrowseTree.parseEpisodeId(item.mediaId) ?: continue
            val episode = try {
                episodeDao.getByIdOnce(episodeId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve media item ${item.mediaId}", e)
                null
            } ?: continue
            if (BrowseTree.isBrowseEpisodeId(item.mediaId) && episode.downloadPath.isBlank()) {
                // Download-first: never start streaming from an Auto browse pick.
                continue
            }
            val podcast = try {
                podcastDao.getByIdOnce(episode.podcastId)
            } catch (e: Exception) {
                null
            }
            val playable = BrowseTree.playableEpisodeItem(
                episode,
                podcast?.title ?: "",
                podcast?.artworkUrl ?: "",
            ) ?: continue
            resolved += playable
        }
        return resolved
    }

    /**
     * Applies the per-podcast playback speed (falling back to the global default)
     * to the session player — the same rule as PlaybackController.play(), for
     * playback started from Android Auto where play() is not in the path.
     */
    private suspend fun applyPodcastPlaybackSpeed(podcastId: Long) {
        val speed = try {
            podcastDao.getPlaybackSpeed(podcastId)?.takeIf { it > 0f }
                ?: preferencesManager.playbackSpeed.first()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load playback speed for podcast $podcastId", e)
            return
        }
        mediaLibrarySession?.player?.playbackParameters =
            PlaybackParameters(speed.coerceIn(0.25f, 3.0f))
    }

    /**
     * Applies volume boost using Android's [LoudnessEnhancer] audio effect.
     *
     * This is the correct way to boost volume beyond the normal range:
     * [LoudnessEnhancer] applies gain at the audio output level. We deliberately do
     * NOT touch the system [AudioManager.STREAM_MUSIC] volume — doing so mutated a
     * global device setting that could be left maxed out on process death or on a
     * re-entrant enable, silently destroying the user's chosen volume. Toggling the
     * enhancer is idempotent, so repeated enable/disable calls are safe.
     *
     * Pocket Casts uses a custom AudioProcessor (ShiftyRenderersFactory) for this,
     * but LoudnessEnhancer is simpler and works well for a straightforward boost.
     */
    private fun applyVolumeBoost(enabled: Boolean) {
        val player = exoPlayer ?: return
        try {
            if (enabled) {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
                }
                loudnessEnhancer?.setTargetGain(2000) // ~20 dB boost
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle LoudnessEnhancer", e)
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {
            // Ignore release errors
        }
        loudnessEnhancer = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Podcast playback controls"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
