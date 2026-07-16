package com.podbelly.feature.player

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

/**
 * A Chromecast button for the player's top bar: the standard mediarouter
 * [MediaRouteButton] (which shows the device picker and reflects the connection
 * state) wrapped in an [AndroidView], set up via [CastButtonFactory] — the
 * pattern used by the media3 cast demo and Pocket Casts.
 *
 * Renders nothing on devices where the Cast framework can't initialize (no
 * Google Play services): [CastContext.getSharedInstance] throws there, and the
 * app must keep working without casting rather than crash.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val castAvailable = remember(context) { isCastFrameworkAvailable(context) }
    if (!castAvailable) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MediaRouteButton(ctx).also { button ->
                try {
                    CastButtonFactory.setUpMediaRouteButton(ctx, button)
                } catch (e: Exception) {
                    Log.w("CastButton", "Failed to set up MediaRouteButton", e)
                }
            }
        },
    )
}

private fun isCastFrameworkAvailable(context: Context): Boolean = try {
    // Same shared instance the PlaybackService initializes; cheap after the first call.
    CastContext.getSharedInstance(context.applicationContext)
    true
} catch (e: Exception) {
    Log.i("CastButton", "Cast framework unavailable; hiding cast button", e)
    false
}
