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

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getAllOnce(): List<QueueItemEntity>

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getQueueWithEpisodes(): Flow<List<QueueEpisode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(item: QueueItemEntity)

    @Query("DELETE FROM queue_items WHERE episodeId = :episodeId")
    suspend fun removeFromQueue(episodeId: Long)

    @Query("UPDATE queue_items SET position = position + 1")
    suspend fun shiftAllPositionsUp()

    /**
     * Atomically inserts [episodeId] at the front of the queue (position 0), shifting
     * existing items down — so an interruption can't leave the queue shifted with no
     * head item. No-op if the episode is already queued.
     */
    @Transaction
    suspend fun addToFront(episodeId: Long, addedAt: Long) {
        if (isInQueue(episodeId)) return
        shiftAllPositionsUp()
        addToQueue(QueueItemEntity(episodeId = episodeId, position = 0, addedAt = addedAt))
    }

    /**
     * Atomically appends [episodeId] to the end of the queue. Reading MAX(position) and
     * inserting inside one @Transaction prevents two concurrent enqueues from both reading
     * the same max and inserting at the same position (duplicate positions, which leave the
     * queue order undefined). No-op if the episode is already queued.
     */
    @Transaction
    suspend fun addToEnd(episodeId: Long, addedAt: Long) {
        if (isInQueue(episodeId)) return
        val nextPosition = (getMaxPosition() ?: -1) + 1
        addToQueue(QueueItemEntity(episodeId = episodeId, position = nextPosition, addedAt = addedAt))
    }

    /**
     * Atomically moves the item identified by [queueId] by [delta] positions and
     * renumbers the whole queue (0..n-1). Reading the snapshot and writing the new
     * positions inside one @Transaction prevents a concurrent mutation (auto-advance
     * removal, a Play Next/Last insert) from interleaving between the read and the
     * write and leaving duplicate or gapped positions. No-op if the item is missing
     * or the move is out of bounds.
     */
    @Transaction
    suspend fun moveItem(queueId: Long, delta: Int) {
        val items = getAllOnce()
        val fromIndex = items.indexOfFirst { it.id == queueId }
        if (fromIndex < 0) return
        val toIndex = fromIndex + delta
        if (toIndex !in items.indices) return

        val reordered = items.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)
        updatePositions(reordered.mapIndexed { index, item -> item.copy(position = index) })
    }

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Update
    suspend fun updatePositions(items: List<QueueItemEntity>)

    @Query("SELECT episodeId FROM queue_items ORDER BY position ASC LIMIT 1")
    suspend fun getNextEpisodeId(): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM queue_items WHERE episodeId = :episodeId)")
    suspend fun isInQueue(episodeId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM queue_items WHERE episodeId = :episodeId)")
    fun isInQueueFlow(episodeId: Long): Flow<Boolean>

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC LIMIT 1")
    suspend fun getNextInQueue(): QueueEpisode?

    @Transaction
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getQueueOnce(): List<QueueEpisode>
}
