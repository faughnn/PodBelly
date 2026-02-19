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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A [MediaSessionService] that manages audio playback for podcast episodes.
 *
 * Handles:
 * - Background playback with a persistent notification
 * - Media session for system integration (lock screen, Bluetooth, etc.)
 * - Custom session commands for skip-silence and volume-boost toggling
 * - Audio focus management
 * - Wake lock for network streaming
 *
 * Modelled after Pocket Casts' PlaybackService architecture.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        const val CUSTOM_COMMAND_SET_SKIP_SILENCE = "SET_SKIP_SILENCE"
        const val CUSTOM_COMMAND_SET_VOLUME_BOOST = "SET_VOLUME_BOOST"
        private const val NOTIFICATION_CHANNEL_ID = "podbelly_playback"
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

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

        exoPlayer = player

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_COMMAND_SET_SKIP_SILENCE, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_COMMAND_SET_VOLUME_BOOST, Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
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
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

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

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .apply {
                sessionActivityIntent?.let { setSessionActivity(it) }
            }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseLoudnessEnhancer()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    /**
     * Applies volume boost using Android's [LoudnessEnhancer] audio effect.
     *
     * This is the correct way to boost volume beyond the normal range.
     * Player.volume is clamped to [0.0, 1.0] and cannot exceed normal level.
     * LoudnessEnhancer applies gain at the audio output level.
     *
     * Pocket Casts uses a custom AudioProcessor (ShiftyRenderersFactory) for this,
     * but LoudnessEnhancer is simpler and works well for a straightforward boost.
     */
    private fun applyVolumeBoost(enabled: Boolean) {
        val player = exoPlayer ?: return

        if (enabled) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
                }
                loudnessEnhancer?.setTargetGain(800) // ~8 dB boost
                loudnessEnhancer?.enabled = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable LoudnessEnhancer", e)
            }
        } else {
            loudnessEnhancer?.enabled = false
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
