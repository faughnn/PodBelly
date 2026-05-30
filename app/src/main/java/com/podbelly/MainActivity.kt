package com.podbelly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.podbelly.ui.SplashScreen
import com.podbelly.ui.WhatsNewDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.PreferencesManager
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.podbelly.core.playback.PlaybackController
import com.podbelly.navigation.PodbellNavHost
import com.podbelly.ui.theme.PodbellTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            appViewModel.refreshIfStale()
        }
    }

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
        lifecycle.addObserver(lifecycleObserver)
        requestNotificationPermissionIfNeeded()
        playbackController.connectToService(this)
        // Resolve the persisted theme once, synchronously, before the first frame so
        // LIGHT/OLED_DARK/HIGH_CONTRAST users don't see a SYSTEM-theme flash at launch.
        // Bounded by a short timeout so a pathologically slow first DataStore disk read
        // can't block the main thread long enough to ANR; collectAsStateWithLifecycle
        // applies the real value moments later if the timeout is hit.
        val initialTheme = runBlocking {
            withTimeoutOrNull(150) { preferencesManager.appTheme.first() } ?: AppTheme.SYSTEM
        }
        setContent {
            val appTheme by preferencesManager.appTheme
                .collectAsStateWithLifecycle(initialTheme)

            val versionName = remember {
                packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
            }
            val whatsNewChanges by appViewModel.showWhatsNew
                .collectAsStateWithLifecycle(null)

            PodbellTheme(appTheme = appTheme) {
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(targetState = showSplash, label = "splash") { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        PodbellNavHost(playbackController = playbackController)
                    }
                }

                // Only show the dialog once the splash has finished, otherwise the
                // AlertDialog can pop over the still-animating splash on upgrade launches.
                if (!showSplash) {
                    whatsNewChanges?.let { changes ->
                        WhatsNewDialog(
                            versionName = versionName,
                            changes = changes,
                            onDismiss = { appViewModel.dismissWhatsNew() },
                        )
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

    override fun onResume() {
        super.onResume()
        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
    }

    override fun onDestroy() {
        super.onDestroy()
        // PlaybackController lifecycle is managed at the singleton level;
        // no explicit release needed here since the service continues in the background.
        lifecycle.removeObserver(lifecycleObserver)
    }
}
