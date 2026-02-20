package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_errors",
    indices = [
        Index(value = ["episodeId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class DownloadErrorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "episodeId")
    val episodeId: Long,

    @ColumnInfo(name = "errorMessage")
    val errorMessage: String,

    @ColumnInfo(name = "errorCode")
    val errorCode: Int = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "retryCount")
    val retryCount: Int = 0,
)
