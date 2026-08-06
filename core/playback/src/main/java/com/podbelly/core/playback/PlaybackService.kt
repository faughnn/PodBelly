package com.podbelly.core.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.podbelly.core.playback.visualizer.AudioSpectrumSink
import com.podbelly.core.playback.visualizer.AudioVisualizerBus
import com.podbelly.core.playback.visualizer.VisualizerRenderersFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.android.gms.cast.framework.CastContext
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [MediaLibraryService] that manages audio playback for podcast episodes and serves
 * the media browse tree to Android Auto and other MediaBrowser clients.
 *
 * Handles:
 * - Background playback with a persistent notification
 * - Media session for system integration (lock screen, Bluetooth, Android Auto, etc.)
 * - The Android Auto browse tree (Podcasts folder, see [BrowseTree])
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

        // Home-screen widget state pushes (received by the app module's
        // PlaybackWidgetProvider, addressed by class name because core:playback
        // cannot depend on the app module).
        const val ACTION_WIDGET_STATE = "com.podbelly.action.WIDGET_STATE"
        const val EXTRA_WIDGET_HAS_ITEM = "hasItem"
        const val EXTRA_WIDGET_IS_PLAYING = "isPlaying"
        const val EXTRA_WIDGET_TITLE = "title"
        const val EXTRA_WIDGET_PODCAST = "podcast"
        private const val WIDGET_PROVIDER_CLASS = "com.podbelly.widget.PlaybackWidgetProvider"
    }

    @Inject
    lateinit var episodeDao: EpisodeDao

    @Inject
    lateinit var podcastDao: PodcastDao

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var visualizerBus: AudioVisualizerBus

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    /**
     * Remote player for Chromecast. Null on devices without Google Play services
     * (Cast initialization threw) — everything then behaves exactly as before.
     */
    private var castPlayer: CastPlayer? = null

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
            // Custom renderers factory taps decoded PCM for the Now Playing
            // visualizer. It keeps the default Sonic/SilenceSkipping processors,
            // so playback speed and skip-silence are unaffected, and only does
            // per-frame analysis while the visualizer is on screen.
            .setRenderersFactory(
                VisualizerRenderersFactory(this, AudioSpectrumSink(visualizerBus))
            )
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

        player.addListener(widgetStateListener)

        initializeCast()
    }

    // -------------------------------------------------------------------------
    // Home-screen widget state pushes
    // -------------------------------------------------------------------------

    /**
     * Mirrors play/pause and episode changes to the home-screen widget. Attached
     * to BOTH players (local and Cast) so the widget stays honest across handoffs;
     * inactive-player events are harmless duplicates of the active state.
     */
    private val widgetStateListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = broadcastWidgetState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
            broadcastWidgetState()
        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) =
            broadcastWidgetState()
    }

    private fun broadcastWidgetState() {
        val player = mediaLibrarySession?.player ?: return
        val metadata = player.mediaMetadata
        val intent = Intent(ACTION_WIDGET_STATE)
            // Explicit component: implicit broadcasts don't reach manifest
            // receivers on O+. Class-name string because core:playback cannot
            // reference the app module's provider class.
            .setClassName(packageName, WIDGET_PROVIDER_CLASS)
            .putExtra(EXTRA_WIDGET_HAS_ITEM, player.currentMediaItem != null)
            .putExtra(EXTRA_WIDGET_IS_PLAYING, player.isPlaying)
            .putExtra(EXTRA_WIDGET_TITLE, metadata.title?.toString() ?: "")
            .putExtra(EXTRA_WIDGET_PODCAST, metadata.artist?.toString() ?: "")
        try {
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push widget state", e)
        }
    }

    // -------------------------------------------------------------------------
    // Chromecast (media3 cast demo's PlayerManager pattern)
    // -------------------------------------------------------------------------

    /**
     * Sets up the [CastPlayer] and the session-availability listener that swaps the
     * MediaSession between the local and remote player. All connected controllers
     * (the app UI's MediaController, Android Auto) follow the session transparently,
     * so PlaybackController's position loop — outro-skip, mark-played, position
     * saving — keeps observing whichever player is active.
     *
     * Devices without Google Play services must not crash:
     * [CastContext.getSharedInstance] throws there, and we degrade to local-only
     * playback with [castPlayer] left null.
     */
    private fun initializeCast() {
        val castContext = try {
            CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            Log.i(TAG, "Cast framework unavailable; continuing without Chromecast", e)
            return
        }
        try {
            castPlayer = CastPlayer(castContext).apply {
                setSessionAvailabilityListener(object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() = switchToCastPlayer()
                    override fun onCastSessionUnavailable() = switchToLocalPlayer()
                })
                addListener(widgetStateListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create CastPlayer; continuing without Chromecast", e)
            castPlayer = null
        }
    }

    private fun isCasting(): Boolean {
        val cast = castPlayer ?: return false
        return mediaLibrarySession?.player === cast
    }

    /**
     * A cast session connected: hand the MediaSession to the [CastPlayer],
     * transferring the current episode, position and play-when-ready state.
     *
     * Download-first wrinkle: the receiver cannot read files on the phone, so the
     * cast item is rebuilt from the episode's REMOTE audioUrl (with a MIME type for
     * the receiver). If the feed provides no remote URL, the swap is aborted with a
     * message and local playback resumes untouched.
     */
    private fun switchToCastPlayer() {
        val cast = castPlayer ?: return
        val session = mediaLibrarySession ?: return
        if (session.player === cast) return
        val local = session.player

        val episodeId = local.currentMediaItem?.mediaId?.let { BrowseTree.parseEpisodeId(it) }

        if (episodeId == null) {
            // Nothing loaded locally: still hand the session over so anything the
            // user starts next plays on the receiver.
            session.player = cast
            return
        }

        serviceScope.launch {
            val episode = try {
                episodeDao.getByIdOnce(episodeId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load episode $episodeId for cast handoff", e)
                null
            }
            val podcast = episode?.let {
                try {
                    podcastDao.getByIdOnce(it.podcastId)
                } catch (e: Exception) {
                    null
                }
            }
            val castItem = episode?.let {
                BrowseTree.castEpisodeItem(it, podcast?.title ?: "", podcast?.artworkUrl ?: "")
            }
            if (castItem == null) {
                // No remote stream URL — keep playing locally rather than going
                // silent on the receiver.
                Log.w(TAG, "Cannot cast episode $episodeId: feed has no remote audio URL")
                showToast("Can't cast this episode — it has no streaming link. Playing on this device.")
                return@launch
            }

            // Capture the transfer state at the moment of the swap, not when the
            // cast session appeared — local playback kept advancing during the
            // database lookup above. serviceScope is main-thread, same as the player.
            val positionMs = local.currentPosition
            val playWhenReady = local.playWhenReady
            val playbackParameters = local.playbackParameters
            local.stop()
            local.clearMediaItems()
            session.player = cast
            cast.setMediaItem(castItem, positionMs)
            cast.playbackParameters = playbackParameters
            cast.playWhenReady = playWhenReady
            cast.prepare()
        }
    }

    /**
     * The cast session ended: hand the MediaSession back to the local ExoPlayer and
     * resume the DOWNLOADED file at the position the receiver reached (download-first
     * again the moment we're back on-device; falls back to the stream URL only when
     * there is no download, matching every other local play path).
     */
    private fun switchToLocalPlayer() {
        val cast = castPlayer ?: return
        val local = exoPlayer ?: return
        val session = mediaLibrarySession ?: return
        if (session.player === local) return

        val episodeId = cast.currentMediaItem?.mediaId?.let { BrowseTree.parseEpisodeId(it) }
        val positionMs = cast.currentPosition
        val playWhenReady = cast.playWhenReady

        cast.stop()
        cast.clearMediaItems()
        session.player = local

        if (episodeId == null) return

        serviceScope.launch {
            val episode = try {
                episodeDao.getByIdOnce(episodeId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load episode $episodeId after cast ended", e)
                null
            } ?: return@launch
            val podcast = try {
                podcastDao.getByIdOnce(episode.podcastId)
            } catch (e: Exception) {
                null
            }
            val localItem = BrowseTree.playableEpisodeItem(
                episode,
                podcast?.title ?: "",
                podcast?.artworkUrl ?: "",
            ) ?: return@launch
            local.setMediaItem(localItem, positionMs)
            local.playWhenReady = playWhenReady
            local.prepare()
        }
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to show toast", e)
        }
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

    // onTaskRemoved is intentionally not overridden: media3 1.6+ provides a safe
    // default that keeps the service (and its foreground grace period) alive while
    // playback is ongoing and stops it otherwise. The previous stopSelf() override
    // predated that and could tear the service down while the session was still in
    // its paused foreground window.

    override fun onDestroy() {
        serviceScope.cancel()
        releaseLoudnessEnhancer()
        // Release the session first (media3 demo ordering), then both players —
        // the session's current player may be either one, so each is released
        // explicitly to avoid leaking the inactive player.
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        castPlayer?.let { cast ->
            cast.setSessionAvailabilityListener(null)
            cast.release()
        }
        castPlayer = null
        exoPlayer?.release()
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
                    parentId == BrowseTree.ROOT_ID -> BrowseTree.rootChildren()
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
                        savedPositionMs = resolveStartPosition(episode.playbackPosition, episode.played),
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
     *   survives the controller/session hop) pass through untouched — unless we are
     *   casting, in which case they are rebuilt from the remote stream URL because
     *   the receiver cannot read files on the phone.
     * - Browse-tree ids ("episode_{id}", i.e. picked from Android Auto) are looked
     *   up in the database and must be downloaded — download-first, undownloaded
     *   episodes are silently dropped (they are never listed as playable anyway).
     * - Plain numeric ids (the app's convention) fall back to the remote stream URL
     *   when there is no download, matching PlaybackController's behaviour.
     */
    private suspend fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        val casting = isCasting()
        val resolved = mutableListOf<MediaItem>()
        for (item in mediaItems) {
            if (item.localConfiguration != null && !casting) {
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
            val playable = if (casting) {
                // Rebuild for the receiver: remote URL + MIME type. Episodes with no
                // remote URL cannot be cast and are dropped (logged)
                // auto-advancing into such an episode mid-cast simply stops.
                BrowseTree.castEpisodeItem(episode, podcast?.title ?: "", podcast?.artworkUrl ?: "")
                    .also {
                        if (it == null) {
                            Log.w(TAG, "Dropping episode $episodeId while casting: no remote audio URL")
                        }
                    }
            } else {
                BrowseTree.playableEpisodeItem(episode, podcast?.title ?: "", podcast?.artworkUrl ?: "")
            } ?: continue
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
