package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "podcasts",
    indices = [Index(value = ["feedUrl"], unique = true)]
)
data class PodcastEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "feedUrl")
    val feedUrl: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "artworkUrl")
    val artworkUrl: String,

    @ColumnInfo(name = "link")
    val link: String,

    @ColumnInfo(name = "language")
    val language: String,

    @ColumnInfo(name = "lastBuildDate")
    val lastBuildDate: Long,

    @ColumnInfo(name = "subscribed")
    val subscribed: Boolean = true,

    @ColumnInfo(name = "subscribedAt")
    val subscribedAt: Long,

    @ColumnInfo(name = "lastRefreshedAt")
    val lastRefreshedAt: Long = 0L,

    @ColumnInfo(name = "episodeCount")
    val episodeCount: Int = 0,

    @ColumnInfo(name = "notifyNewEpisodes", defaultValue = "1")
    val notifyNewEpisodes: Boolean = true,

    @ColumnInfo(name = "playbackSpeed", defaultValue = "0.0")
    val playbackSpeed: Float = 0.0f,

    /**
     * Seconds to auto-skip at the start of every episode of this podcast
     * (per-feed intro skip, AntennaPod pattern). 0 = disabled.
     */
    @ColumnInfo(name = "skipIntroSeconds", defaultValue = "0")
    val skipIntroSeconds: Int = 0,

    /**
     * Seconds to cut off the end of every episode of this podcast
     * (per-feed outro/ending skip, AntennaPod pattern). 0 = disabled.
     */
    @ColumnInfo(name = "skipOutroSeconds", defaultValue = "0")
    val skipOutroSeconds: Int = 0,
)
