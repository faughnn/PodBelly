package com.podbelly.feature.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import java.util.concurrent.atomic.AtomicReference

/** Android 17 (API 37). No named Build.VERSION_CODES constant is used so this
 *  still compiles if the platform constant is renamed. */
private const val ANDROID_17 = 37

/** Runtime permission gating local-network discovery from Android 17 onwards.
 *  Referenced as a literal rather than Manifest.permission so the module does
 *  not require compiling against API 37 to build. */
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

/**
 * A Chromecast button for the player's top bar: the standard mediarouter
 * [MediaRouteButton] (which shows the device picker and reflects the connection
 * state) wrapped in an [AndroidView], set up via [CastButtonFactory] — the
 * pattern used by the media3 cast demo and Pocket Casts.
 *
 * Renders nothing on devices where the Cast framework can't initialize (no
 * Google Play services): [CastContext.getSharedInstance] throws there, and the
 * app must keep working without casting rather than crash.
 *
 * From Android 17, finding Cast receivers needs the ACCESS_LOCAL_NETWORK
 * runtime permission — the mediarouter picker discovers devices itself, so it
 * is not covered by the system-picker exemption. On those devices a transparent
 * overlay takes the tap first, asks for the permission if it hasn't been
 * granted, and then opens the picker. Below API 37 the overlay is not added at
 * all and the button behaves exactly as it always has.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val castAvailable = remember(context) { isCastFrameworkAvailable(context) }
    if (!castAvailable) return

    val routeButtonRef = remember { AtomicReference<MediaRouteButton?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Open the picker whatever the answer: if permission was denied the
        // picker simply finds nothing, which is clearer to the user than a tap
        // that appears to do nothing at all.
        routeButtonRef.get()?.performClick()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MediaRouteButton(ctx).also { button ->
                    routeButtonRef.set(button)
                    try {
                        CastButtonFactory.setUpMediaRouteButton(ctx, button)
                    } catch (e: Exception) {
                        Log.w("CastButton", "Failed to set up MediaRouteButton", e)
                    }
                }
            },
        )
        if (Build.VERSION.SDK_INT >= ANDROID_17) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        if (context.checkSelfPermission(ACCESS_LOCAL_NETWORK) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            routeButtonRef.get()?.performClick()
                        } else {
                            permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
                        }
                    },
            )
        }
    }
}

private fun isCastFrameworkAvailable(context: Context): Boolean = try {
    // Same shared instance the PlaybackService initializes; cheap after the first call.
    CastContext.getSharedInstance(context.applicationContext)
    true
} catch (e: Exception) {
    Log.i("CastButton", "Cast framework unavailable; hiding cast button", e)
    false
}
