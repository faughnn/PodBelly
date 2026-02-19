package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.podbelly.core.database.entity.QueueItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getAll(): Flow<List<QueueItemEntity>>

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getQueueWithEpisodes(): Flow<List<QueueEpisode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(item: QueueItemEntity)

    @Query("DELETE FROM queue_items WHERE episodeId = :episodeId")
    suspend fun removeFromQueue(episodeId: Long)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Update
    suspend fun updatePositions(items: List<QueueItemEntity>)

    @Query("SELECT episodeId FROM queue_items ORDER BY position ASC LIMIT 1")
    suspend fun getNextEpisodeId(): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM queue_items WHERE episodeId = :episodeId)")
    suspend fun isInQueue(episodeId: Long): Boolean

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC LIMIT 1")
    suspend fun getNextInQueue(): QueueEpisode?

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getQueueOnce(): List<QueueEpisode>
}
