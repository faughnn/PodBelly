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
    fun getMostDownloadedPodcasts(limit: Int = 5): Flow<List<PodcastDownloadStat>>
}
