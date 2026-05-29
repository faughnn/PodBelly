package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    indices = [
        Index(value = ["podcastId"]),
        // GUIDs are only unique *within* a feed, so scope uniqueness to the podcast.
        // A global unique index caused episodes to be silently dropped when two
        // feeds happened to share a GUID (e.g. plain integer ids).
        Index(value = ["podcastId", "guid"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "podcastId")
    val podcastId: Long,

    @ColumnInfo(name = "guid")
    val guid: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "audioUrl")
    val audioUrl: String,

    @ColumnInfo(name = "publicationDate")
    val publicationDate: Long,

    @ColumnInfo(name = "durationSeconds")
    val durationSeconds: Int = 0,

    @ColumnInfo(name = "artworkUrl")
    val artworkUrl: String = "",

    @ColumnInfo(name = "played")
    val played: Boolean = false,

    @ColumnInfo(name = "playbackPosition")
    val playbackPosition: Long = 0L,

    @ColumnInfo(name = "downloadPath")
    val downloadPath: String = "",

    @ColumnInfo(name = "downloadedAt")
    val downloadedAt: Long = 0L,

    @ColumnInfo(name = "fileSize")
    val fileSize: Long = 0L
)
