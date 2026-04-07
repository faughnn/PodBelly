package com.podbelly.core.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * A [CoroutineWorker] that downloads a podcast episode in the background.
 *
 * Runs as a foreground worker with a persistent notification so the download
 * survives app switches and process priority changes. Modelled after Pocket
 * Casts' DownloadEpisodeWorker which uses WorkManager + setForeground().
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadManager: DownloadManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getLong(KEY_EPISODE_ID, -1L)
        if (episodeId == -1L) {
            Log.e(TAG, "No episode ID provided")
            return Result.failure()
        }

        // Promote to foreground so the OS keeps us alive while downloading.
        // Wrapped in try/catch following Pocket Casts' pattern — on some devices
        // (e.g. Android 12+ background start restrictions) this can fail.
        try {
            setForeground(createForegroundInfo(episodeId))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground for episode $episodeId", e)
        }

        return try {
            downloadManager.downloadEpisode(episodeId)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for episode $episodeId", e)
            Result.failure()
        }
    }

    private fun createForegroundInfo(episodeId: Long): ForegroundInfo {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading episode")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID_BASE + episodeId.toInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_DOWNLOADS) == null) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Episode download progress"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "DownloadWorker"
        const val KEY_EPISODE_ID = "episode_id"
        const val CHANNEL_DOWNLOADS = "podbelly_downloads"
        private const val NOTIFICATION_ID_BASE = 2000
    }
}
