package com.podbelly.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.podbelly.PodbellApp
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class FeedRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val searchRepository: PodcastSearchRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic feed refresh")

        return try {
            val podcasts = podcastDao.getAll().first()

            if (podcasts.isEmpty()) {
                Log.i(TAG, "No subscribed podcasts, skipping refresh")
                return Result.success()
            }

            var refreshedCount = 0
            var newEpisodeCount = 0

            // Track podcasts with new episodes for notifications
            val podcastsWithNewEpisodes = mutableMapOf<String, Int>()

            for (podcast in podcasts) {
                try {
                    val feed = searchRepository.fetchFeed(podcast.feedUrl)

                    // Check existing GUIDs to find truly new episodes
                    val newEpisodes = feed.episodes.mapNotNull { rssEpisode ->
                        val existing = episodeDao.getByGuid(rssEpisode.guid)
                        if (existing == null) {
                            EpisodeEntity(
                                podcastId = podcast.id,
                                guid = rssEpisode.guid,
                                title = rssEpisode.title,
                                description = rssEpisode.description,
                                audioUrl = rssEpisode.audioUrl,
                                publicationDate = rssEpisode.publishedAt,
                                durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                artworkUrl = rssEpisode.artworkUrl ?: podcast.artworkUrl,
                                fileSize = rssEpisode.fileSize,
                            )
                        } else {
                            null
                        }
                    }

                    if (newEpisodes.isNotEmpty()) {
                        episodeDao.insertAll(newEpisodes)
                        newEpisodeCount += newEpisodes.size

                        // Track for notification if podcast has notifications enabled
                        if (podcast.notifyNewEpisodes) {
                            podcastsWithNewEpisodes[podcast.title] = newEpisodes.size
                        }
                    }

                    podcastDao.update(
                        podcast.copy(
                            lastRefreshedAt = System.currentTimeMillis(),
                            episodeCount = feed.episodes.size,
                        )
                    )

                    refreshedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to refresh feed for podcast: ${podcast.title}", e)
                }
            }

            // Post notifications for new episodes
            if (podcastsWithNewEpisodes.isNotEmpty()) {
                postNewEpisodesNotification(podcastsWithNewEpisodes)
            }

            Log.i(TAG, "Feed refresh complete: $refreshedCount podcasts refreshed, $newEpisodeCount new episodes inserted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Feed refresh failed", e)
            Result.retry()
        }
    }

    private fun postNewEpisodesNotification(podcastsWithNewEpisodes: Map<String, Int>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val totalNew = podcastsWithNewEpisodes.values.sum()
        val title = if (podcastsWithNewEpisodes.size == 1) {
            val podcastName = podcastsWithNewEpisodes.keys.first()
            val count = podcastsWithNewEpisodes.values.first()
            "$count new episode${if (count > 1) "s" else ""} from $podcastName"
        } else {
            "$totalNew new episodes from ${podcastsWithNewEpisodes.size} podcasts"
        }

        val body = if (podcastsWithNewEpisodes.size > 1) {
            podcastsWithNewEpisodes.entries.joinToString("\n") { (name, count) ->
                "$name: $count new"
            }
        } else {
            null
        }

        // Launch the main activity when notification is tapped
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                applicationContext, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            PodbellApp.CHANNEL_NEW_EPISODES
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .apply { if (body != null) setContentText(body) }
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        notificationManager.notify(NOTIFICATION_ID_NEW_EPISODES, notification)
    }

    companion object {
        private const val TAG = "FeedRefreshWorker"
        const val WORK_NAME = "feed_refresh_work"
        private const val NOTIFICATION_ID_NEW_EPISODES = 1001
    }
}
