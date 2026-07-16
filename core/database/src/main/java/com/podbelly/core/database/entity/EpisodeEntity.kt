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

    @ColumnInfo(name = "lastPlayedAt")
    val lastPlayedAt: Long = 0L,

    @ColumnInfo(name = "downloadPath")
    val downloadPath: String = "",

    @ColumnInfo(name = "downloadedAt")
    val downloadedAt: Long = 0L,

    @ColumnInfo(name = "fileSize")
    val fileSize: Long = 0L,

    /**
     * When this episode row was first discovered by a feed *refresh* (epoch ms).
     * Deliberately left 0 for episodes imported when first subscribing to a
     * podcast, so a new subscription's backlog never shows up as "new" on Home.
     */
    @ColumnInfo(name = "addedAt")
    val addedAt: Long = 0L,

    /**
     * Podcasting 2.0 `<podcast:transcript>` URL for this episode; empty when the
     * feed doesn't provide one. Refreshes keep it up to date via updateFeedFields.
     */
    @ColumnInfo(name = "transcriptUrl", defaultValue = "")
    val transcriptUrl: String = "",

    /** MIME type declared for [transcriptUrl] (e.g. "text/vtt"); empty when unknown. */
    @ColumnInfo(name = "transcriptType", defaultValue = "")
    val transcriptType: String = "",
)
