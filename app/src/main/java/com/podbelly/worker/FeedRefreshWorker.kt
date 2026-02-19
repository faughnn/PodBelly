package com.podbelly.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

            for (podcast in podcasts) {
                try {
                    val feed = searchRepository.fetchFeed(podcast.feedUrl)

                    val episodes = feed.episodes.map { rssEpisode ->
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
                    }

                    if (episodes.isNotEmpty()) {
                        episodeDao.insertAll(episodes)
                        newEpisodeCount += episodes.size
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

            Log.i(TAG, "Feed refresh complete: $refreshedCount podcasts refreshed, $newEpisodeCount new episodes inserted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Feed refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FeedRefreshWorker"
        const val WORK_NAME = "feed_refresh_work"
    }
}
