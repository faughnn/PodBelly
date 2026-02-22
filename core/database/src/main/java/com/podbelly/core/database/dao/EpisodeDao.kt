package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.podbelly.core.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publicationDate DESC")
    fun getByPodcastId(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun getById(id: Long): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE guid = :guid LIMIT 1")
    suspend fun getByGuid(guid: String): EpisodeEntity?

    @Query(
        """
        SELECT episodes.* FROM episodes
        INNER JOIN podcasts ON episodes.podcastId = podcasts.id
        WHERE podcasts.subscribed = 1
        ORDER BY episodes.publicationDate DESC
        LIMIT :limit
        """
    )
    fun getRecentEpisodes(limit: Int = 50): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND played = 0 ORDER BY publicationDate DESC")
    fun getUnplayedByPodcastId(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadPath != '' ORDER BY downloadedAt DESC")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadPath != ''")
    suspend fun getDownloadedEpisodesOnce(): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("UPDATE episodes SET playbackPosition = :position WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Long, position: Long)

    @Query("UPDATE episodes SET played = 1 WHERE id = :id")
    suspend fun markAsPlayed(id: Long)

    @Query("UPDATE episodes SET played = 0 WHERE id = :id")
    suspend fun markAsUnplayed(id: Long)

    @Query("UPDATE episodes SET downloadPath = :path, fileSize = :fileSize, downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setDownloadPath(id: Long, path: String, fileSize: Long, downloadedAt: Long)

    @Query("UPDATE episodes SET downloadPath = '', fileSize = 0, downloadedAt = 0 WHERE id = :id")
    suspend fun clearDownload(id: Long)

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteByPodcastId(podcastId: Long)

    @Query("SELECT * FROM episodes WHERE played = 1 AND downloadPath != ''")
    suspend fun getPlayedDownloadedEpisodes(): List<EpisodeEntity>

    @Query("SELECT podcastId, MAX(publicationDate) AS latestPublicationDate FROM episodes GROUP BY podcastId")
    fun getLatestEpisodeDateByPodcast(): Flow<List<PodcastLatestEpisode>>
}

data class PodcastLatestEpisode(
    val podcastId: Long,
    val latestPublicationDate: Long,
)
