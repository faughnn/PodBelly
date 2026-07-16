package com.podbelly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.RemoteViews
import com.podbelly.R
import com.podbelly.core.playback.PlaybackService

/**
 * Home-screen playback widget: current episode + play/pause + skip forward.
 *
 * State pushes come from [PlaybackService] as explicit broadcasts
 * ([PlaybackService.ACTION_WIDGET_STATE]) on every play/pause/episode change —
 * the widget never polls. Button taps send standard media-button intents to the
 * service, which media3's MediaSessionService handles natively, so they control
 * whichever player is active (local or Chromecast).
 */
class PlaybackWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Initial placement / launcher-driven refresh: render the last known
        // state (idle when the process is fresh).
        render(context, appWidgetManager, appWidgetIds, lastState)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlaybackService.ACTION_WIDGET_STATE) {
            val state = WidgetState(
                hasItem = intent.getBooleanExtra(PlaybackService.EXTRA_WIDGET_HAS_ITEM, false),
                isPlaying = intent.getBooleanExtra(PlaybackService.EXTRA_WIDGET_IS_PLAYING, false),
                title = intent.getStringExtra(PlaybackService.EXTRA_WIDGET_TITLE).orEmpty(),
                podcast = intent.getStringExtra(PlaybackService.EXTRA_WIDGET_PODCAST).orEmpty(),
            )
            lastState = state
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, PlaybackWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) render(context, manager, ids, state)
            return
        }
        super.onReceive(context, intent)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        state: WidgetState,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_playback).apply {
            if (state.hasItem) {
                setTextViewText(R.id.widget_title, state.title.ifBlank { "Episode" })
                setTextViewText(R.id.widget_podcast, state.podcast)
                setImageViewResource(
                    R.id.widget_play_pause,
                    if (state.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                )
            } else {
                setTextViewText(R.id.widget_title, "Podbelly")
                setTextViewText(R.id.widget_podcast, "Nothing playing")
                setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play)
            }

            setOnClickPendingIntent(
                R.id.widget_play_pause,
                mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, requestCode = 1),
            )
            setOnClickPendingIntent(
                R.id.widget_skip_forward,
                mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, requestCode = 2),
            )
            // Tapping anywhere else opens the app.
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        0,
                        launch,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            }
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /**
     * A standard media-button press delivered straight to the playback service —
     * media3's MediaSessionService routes it to the active player, so play/pause
     * and skip work identically for local playback, Android Auto, and Cast.
     */
    private fun mediaButtonIntent(context: Context, keyCode: Int, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .setClass(context, PlaybackService::class.java)
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private data class WidgetState(
        val hasItem: Boolean = false,
        val isPlaying: Boolean = false,
        val title: String = "",
        val podcast: String = "",
    )

    private companion object {
        /**
         * Last state pushed by the service. Process-local by design: if the
         * process died, nothing is playing, and the idle default is correct.
         */
        @Volatile
        var lastState = WidgetState()
    }
}
