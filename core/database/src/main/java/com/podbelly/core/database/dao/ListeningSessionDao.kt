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
               SUM(ls.listenedMs) AS totalListenedMs
        FROM listening_sessions ls
        INNER JOIN podcasts p ON ls.podcastId = p.id
        GROUP BY ls.podcastId
        ORDER BY totalListenedMs DESC
        LIMIT :limit
        """
    )
    fun getMostListenedPodcasts(limit: Int = 10): Flow<List<PodcastListeningStat>>

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
    fun getMostListenedEpisodes(limit: Int = 10): Flow<List<EpisodeListeningStat>>

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

    @Query("SELECT DISTINCT startedAt / 86400000 AS epochDay FROM listening_sessions ORDER BY epochDay ASC")
    fun getListeningDays(): Flow<List<Long>>

    @Query("SELECT COALESCE(AVG(listenedMs), 0) FROM listening_sessions")
    fun getAverageSessionLengthMs(): Flow<Long>

    @Query(
        """
        SELECT CAST((startedAt / 86400000 + 3) % 7 AS INTEGER) AS dayOfWeek,
               SUM(listenedMs) AS totalListenedMs
        FROM listening_sessions
        GROUP BY dayOfWeek
        ORDER BY totalListenedMs DESC
        """
    )
    fun getListeningMsByDayOfWeek(): Flow<List<DayOfWeekStat>>

    @Query(
        """
        SELECT CAST((startedAt % 86400000) / 3600000 AS INTEGER) AS hour,
               SUM(listenedMs) AS totalListenedMs
        FROM listening_sessions
        GROUP BY hour
        ORDER BY totalListenedMs DESC
        """
    )
    fun getListeningMsByHourOfDay(): Flow<List<HourOfDayStat>>

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
}
