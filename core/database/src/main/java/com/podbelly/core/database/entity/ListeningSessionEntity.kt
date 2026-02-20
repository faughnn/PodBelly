package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_sessions",
    indices = [
        Index(value = ["episodeId"]),
        Index(value = ["podcastId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "episodeId")
    val episodeId: Long,

    @ColumnInfo(name = "podcastId")
    val podcastId: Long,

    @ColumnInfo(name = "startedAt")
    val startedAt: Long,

    @ColumnInfo(name = "endedAt")
    val endedAt: Long = 0L,

    @ColumnInfo(name = "listenedMs")
    val listenedMs: Long = 0L,

    @ColumnInfo(name = "playbackSpeed")
    val playbackSpeed: Float = 1.0f,

    @ColumnInfo(name = "silenceTrimmedMs")
    val silenceTrimmedMs: Long = 0L,
)
