package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.podbelly.core.database.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY title ASC")
    fun getAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun getById(id: Long): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun getByFeedUrl(feedUrl: String): PodcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity): Long

    @Update
    suspend fun update(podcast: PodcastEntity)

    @Delete
    suspend fun delete(podcast: PodcastEntity)

    @Query("UPDATE podcasts SET subscribed = 0 WHERE id = :id")
    suspend fun unsubscribe(id: Long)

    @Query("UPDATE podcasts SET notifyNewEpisodes = :enabled WHERE id = :id")
    suspend fun setNotifyNewEpisodes(id: Long, enabled: Boolean)

    @Query("SELECT playbackSpeed FROM podcasts WHERE id = :id LIMIT 1")
    suspend fun getPlaybackSpeed(id: Long): Float?

    @Query("UPDATE podcasts SET playbackSpeed = :speed WHERE id = :id")
    suspend fun updatePlaybackSpeed(id: Long, speed: Float)
}
