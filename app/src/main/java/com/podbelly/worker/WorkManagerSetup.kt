package com.podbelly.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility object for scheduling background work via WorkManager.
 */
object WorkManagerSetup {

    /**
     * Schedules a periodic feed refresh that runs at the specified interval
     * whenever a network connection is available.
     *
     * If a periodic refresh is already scheduled, it will be updated with the new interval.
     *
     * @param context Application context.
     * @param intervalMinutes The interval in minutes between feed refreshes.
     *                        Note: WorkManager enforces a minimum of 15 minutes.
     */
    fun schedulePeriodicRefresh(context: Context, intervalMinutes: Int = 60) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshRequest = PeriodicWorkRequestBuilder<FeedRefreshWorker>(
            intervalMinutes.toLong().coerceAtLeast(15L),
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .addTag(FeedRefreshWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FeedRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            refreshRequest,
        )
    }

    /**
     * Cancels the periodic feed refresh if one is scheduled.
     *
     * @param context Application context.
     */
    fun cancelPeriodicRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FeedRefreshWorker.WORK_NAME)
    }
}
