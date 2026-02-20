package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.podbelly.core.database.entity.DownloadErrorEntity
import kotlinx.coroutines.flow.Flow

data class DownloadErrorWithEpisode(
    val id: Long,
    val episodeId: Long,
    val episodeTitle: String,
    val errorMessage: String,
    val errorCode: Int,
    val timestamp: Long,
    val retryCount: Int,
)

@Dao
interface DownloadErrorDao {

    @Insert
    suspend fun insert(error: DownloadErrorEntity)

    @Query(
        """
        SELECT de.id, de.episodeId, e.title AS episodeTitle, de.errorMessage,
               de.errorCode, de.timestamp, de.retryCount
        FROM download_errors de
        INNER JOIN episodes e ON de.episodeId = e.id
        ORDER BY de.timestamp DESC
        """
    )
    fun getAll(): Flow<List<DownloadErrorWithEpisode>>

    @Query("DELETE FROM download_errors WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @Query("DELETE FROM download_errors")
    suspend fun deleteAll()

    @Query("UPDATE download_errors SET retryCount = retryCount + 1 WHERE episodeId = :episodeId")
    suspend fun incrementRetryCount(episodeId: Long)
}
