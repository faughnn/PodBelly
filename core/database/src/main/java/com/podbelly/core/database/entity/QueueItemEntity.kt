package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_items",
    indices = [
        Index(value = ["episodeId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "episodeId")
    val episodeId: Long,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "addedAt")
    val addedAt: Long
)
