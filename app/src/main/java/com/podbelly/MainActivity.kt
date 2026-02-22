package com.podbelly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.podbelly.ui.SplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.playback.PlaybackController
import com.podbelly.navigation.PodbellNavHost
import com.podbelly.ui.theme.PodbellTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — playback works either way, just no notification */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        playbackController.connectToService(this)
        setContent {
            val appTheme by preferencesManager.appTheme
                .collectAsStateWithLifecycle(AppTheme.SYSTEM)

            PodbellTheme(appTheme = appTheme) {
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(targetState = showSplash, label = "splash") { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        PodbellNavHost(playbackController = playbackController)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // PlaybackController lifecycle is managed at the singleton level;
        // no explicit release needed here since the service continues in the background.
    }
}
