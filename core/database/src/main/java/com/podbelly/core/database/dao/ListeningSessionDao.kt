package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.podbelly.core.database.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow

data class PodcastListeningStat(
    val podcastId: Long,
    val podcastTitle: String,
    val artworkUrl: String,
    val totalListenedMs: Long,
    val episodeCount: Long,
)

data class EpisodeListeningStat(
    val episodeId: Long,
    val episodeTitle: String,
    val podcastTitle: String,
    val totalListenedMs: Long,
)

data class PodcastDownloadStat(
    val podcastId: Long,
    val podcastTitle: String,
    val artworkUrl: String,
    val downloadCount: Long,
)

data class DayOfWeekStat(
    val dayOfWeek: Int,
    val totalListenedMs: Long,
)

data class HourOfDayStat(
    val hour: Int,
    val totalListenedMs: Long,
)

data class EpisodeCompletionStat(
    val episodeId: Long,
    val totalListenedMs: Long,
    val durationMs: Long,
)

/**
 * Engagement summary for one subscribed podcast, for the Stats "Podcasts" tab
 * (least-listened first, so barely-touched subscriptions surface for pruning).
 */
data class PodcastEngagementStat(
    val podcastId: Long,
    val podcastTitle: String,
    val artworkUrl: String,
    val subscribedAt: Long,
    val totalListenedMs: Long,
    /** Start of the most recent listening session, 0 = never listened. */
    val lastListenedAt: Long,
    val episodeCount: Long,
    val playedCount: Long,
    val inProgressCount: Long,
    val downloadedCount: Long,
    val downloadedBytes: Long,
    /** Publication date of the newest stored episode, 0 = none. */
    val latestEpisodeAt: Long,
)

@Dao
interface ListeningSessionDao {

    @Insert
    suspend fun insert(session: ListeningSessionEntity): Long

    @Query("UPDATE listening_sessions SET endedAt = :endedAt, listenedMs = :listenedMs WHERE id = :id")
    suspend fun updateSession(id: Long, endedAt: Long, listenedMs: Long)

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM listening_sessions")
    fun getTotalListenedMs(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE WHEN playbackSpeed > 1.0
            THEN CAST(listenedMs * (playbackSpeed - 1.0) / playbackSpeed AS INTEGER)
            ELSE 0 END
        ), 0) FROM listening_sessions
        """
    )
    fun getTimeSavedBySpeed(): Flow<Long>

    @Query("SELECT COALESCE(SUM(silenceTrimmedMs), 0) FROM listening_sessions")
    fun getTotalSilenceTrimmedMs(): Flow<Long>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM listening_sessions WHERE startedAt >= :since")
    fun getListenedMsSince(since: Long): Flow<Long>

    @Query(
        """
        SELECT ls.podcastId, p.title AS podcastTitle, p.artworkUrl,
               SUM(ls.listenedMs) AS totalListenedMs,
               COUNT(DISTINCT ls.episodeId) AS episodeCount
        FROM listening_sessions ls
        INNER JOIN podcasts p ON ls.podcastId = p.id
        GROUP BY ls.podcastId
        ORDER BY totalListenedMs DESC
        LIMIT :limit
        """
    )
    fun getMostListenedPodcasts(limit: Int = 5): Flow<List<PodcastListeningStat>>

    @Query(
        """
        SELECT ls.episodeId, e.title AS episodeTitle, p.title AS podcastTitle,
               SUM(ls.listenedMs) AS totalListenedMs
        FROM listening_sessions ls
        INNER JOIN episodes e ON ls.episodeId = e.id
        INNER JOIN podcasts p ON ls.podcastId = p.id
        GROUP BY ls.episodeId
        ORDER BY totalListenedMs DESC
        LIMIT :limit
        """
    )
    fun getMostListenedEpisodes(limit: Int = 5): Flow<List<EpisodeListeningStat>>

    @Query(
        """
        SELECT e.podcastId AS podcastId, p.title AS podcastTitle, p.artworkUrl,
               COUNT(*) AS downloadCount
        FROM episodes e
        INNER JOIN podcasts p ON e.podcastId = p.id
        WHERE e.downloadPath != ''
        GROUP BY e.podcastId
        ORDER BY downloadCount DESC
        LIMIT :limit
        """
    )
    fun getMostDownloadedPodcasts(limit: Int = 10): Flow<List<PodcastDownloadStat>>

    @Query("SELECT DISTINCT (startedAt + :tzOffsetMs) / 86400000 AS epochDay FROM listening_sessions ORDER BY epochDay ASC")
    fun getListeningDays(tzOffsetMs: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(AVG(listenedMs), 0) FROM listening_sessions")
    fun getAverageSessionLengthMs(): Flow<Long>

    @Query(
        """
        SELECT CAST(((startedAt + :tzOffsetMs) / 86400000 + 3) % 7 AS INTEGER) AS dayOfWeek,
               SUM(listenedMs) AS totalListenedMs
        FROM listening_sessions
        GROUP BY dayOfWeek
        ORDER BY totalListenedMs DESC
        """
    )
    fun getListeningMsByDayOfWeek(tzOffsetMs: Long): Flow<List<DayOfWeekStat>>

    @Query(
        """
        SELECT CAST(((startedAt + :tzOffsetMs) % 86400000) / 3600000 AS INTEGER) AS hour,
               SUM(listenedMs) AS totalListenedMs
        FROM listening_sessions
        GROUP BY hour
        ORDER BY totalListenedMs DESC
        """
    )
    fun getListeningMsByHourOfDay(tzOffsetMs: Long): Flow<List<HourOfDayStat>>

    @Query(
        """
        SELECT ls.episodeId,
               SUM(ls.listenedMs) AS totalListenedMs,
               CAST(e.durationSeconds AS INTEGER) * 1000 AS durationMs
        FROM listening_sessions ls
        INNER JOIN episodes e ON ls.episodeId = e.id
        WHERE e.durationSeconds > 0
        GROUP BY ls.episodeId
        """
    )
    fun getEpisodeCompletionStats(): Flow<List<EpisodeCompletionStat>>

    /**
     * One row per subscribed podcast with its engagement metrics, least listened
     * first. Session and episode aggregates are pre-grouped in subqueries so the
     * two LEFT JOINs can't fan out against each other.
     */
    @Query(
        """
        SELECT p.id AS podcastId,
               p.title AS podcastTitle,
               p.artworkUrl,
               p.subscribedAt,
               COALESCE(ls.totalListenedMs, 0) AS totalListenedMs,
               COALESCE(ls.lastListenedAt, 0) AS lastListenedAt,
               COALESCE(e.episodeCount, 0) AS episodeCount,
               COALESCE(e.playedCount, 0) AS playedCount,
               COALESCE(e.inProgressCount, 0) AS inProgressCount,
               COALESCE(e.downloadedCount, 0) AS downloadedCount,
               COALESCE(e.downloadedBytes, 0) AS downloadedBytes,
               COALESCE(e.latestEpisodeAt, 0) AS latestEpisodeAt
        FROM podcasts p
        LEFT JOIN (
            SELECT podcastId,
                   SUM(listenedMs) AS totalListenedMs,
                   MAX(startedAt) AS lastListenedAt
            FROM listening_sessions
            GROUP BY podcastId
        ) ls ON ls.podcastId = p.id
        LEFT JOIN (
            SELECT podcastId,
                   COUNT(*) AS episodeCount,
                   SUM(CASE WHEN played = 1 THEN 1 ELSE 0 END) AS playedCount,
                   SUM(CASE WHEN played = 0 AND playbackPosition > 0 THEN 1 ELSE 0 END) AS inProgressCount,
                   SUM(CASE WHEN downloadPath != '' THEN 1 ELSE 0 END) AS downloadedCount,
                   SUM(CASE WHEN downloadPath != '' THEN fileSize ELSE 0 END) AS downloadedBytes,
                   MAX(publicationDate) AS latestEpisodeAt
            FROM episodes
            GROUP BY podcastId
        ) e ON e.podcastId = p.id
        WHERE p.subscribed = 1
        ORDER BY totalListenedMs ASC, p.title COLLATE NOCASE ASC
        """
    )
    fun getPodcastEngagementStats(): Flow<List<PodcastEngagementStat>>
}
