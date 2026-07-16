package com.podbelly

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import com.podbelly.core.common.CrashLogStore
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.playback.PlaybackController
import com.podbelly.worker.WorkManagerSetup
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PodbellApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var crashLogStore: CrashLogStore

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var playbackController: PlaybackController

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)
        installCrashLogHandler()
        createNotificationChannels()
        scheduleFeedRefresh()
        // Connect the PlaybackController at process start, not only from MainActivity:
        // when Android Auto cold-starts the process by binding to PlaybackService, no
        // Activity ever runs, and without this connection the controller's position
        // loop (position saving, mark-played, outro-skip, queue auto-advance) would
        // never observe Auto-initiated playback. connectToService is idempotent, so
        // MainActivity's existing call remains a harmless no-op.
        playbackController.connectToService(this)
    }

    /**
     * Schedules (or cancels) the periodic background feed refresh to match the user's
     * configured interval. Collecting the preference keeps the schedule in sync when the
     * interval is changed in Settings; an interval of 0 means "Manual only", so the periodic
     * work is cancelled. Without this, [WorkManagerSetup.schedulePeriodicRefresh] was never
     * called and the background refresh feature never ran.
     */
    private fun scheduleFeedRefresh() {
        appScope.launch {
            preferencesManager.feedRefreshIntervalMinutes.collectLatest { minutes ->
                if (minutes <= 0) {
                    WorkManagerSetup.cancelPeriodicRefresh(this@PodbellApp)
                } else {
                    WorkManagerSetup.schedulePeriodicRefresh(this@PodbellApp, minutes)
                }
            }
        }
    }

    private fun installCrashLogHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashLogStore.append(throwable, thread.name, versionName)
            } catch (_: Throwable) {
                // Never let crash logging itself break crash handling.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_NEW_EPISODES,
                "New Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new podcast episodes"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_NEW_EPISODES = "new_episodes"
    }
}
