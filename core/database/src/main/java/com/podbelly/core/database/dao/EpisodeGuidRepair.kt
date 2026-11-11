package com.podbelly.core.database.dao

import androidx.room.ColumnInfo
import kotlin.math.abs

/** What a feed item says about itself, for [EpisodeDao.repairChangedGuids]. */
data class FeedItemKey(
    val guid: String,
    val title: String,
    val audioUrl: String,
    val publicationDate: Long,
    val durationSeconds: Int,
)

/**
 * The stored columns [EpisodeDao.repairChangedGuids] needs: enough to match an
 * episode and carry its user state over, without loading every description (a
 * long-running Substack feed stores megabytes of show notes).
 */
data class EpisodeMatchRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "guid") val guid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "audioUrl") val audioUrl: String,
    @ColumnInfo(name = "publicationDate") val publicationDate: Long,
    @ColumnInfo(name = "durationSeconds") val durationSeconds: Int,
    @ColumnInfo(name = "played") val played: Boolean,
    @ColumnInfo(name = "playbackPosition") val playbackPosition: Long,
    @ColumnInfo(name = "lastPlayedAt") val lastPlayedAt: Long,
    @ColumnInfo(name = "downloadPath") val downloadPath: String,
    @ColumnInfo(name = "downloadedAt") val downloadedAt: Long,
    @ColumnInfo(name = "fileSize") val fileSize: Long,
    @ColumnInfo(name = "autoDownloaded") val autoDownloaded: Boolean,
)

/**
 * Whether a stored episode and a feed item look like the same episode even though
 * their GUIDs differ. Same rules as AntennaPod's FeedItemDuplicateGuesser: the same
 * audio URL, or the same title published the same day at about the same length.
 */
internal fun EpisodeMatchRow.seemsSameEpisodeAs(item: FeedItemKey): Boolean {
    if (audioUrl.isNotEmpty() && audioUrl == item.audioUrl) return true
    val normalizedTitle = normalizeTitleForMatch(title)
    return normalizedTitle.isNotEmpty() &&
        normalizedTitle == normalizeTitleForMatch(item.title) &&
        publicationDate > 0L && item.publicationDate > 0L &&
        publicationDate / DAY_MS == item.publicationDate / DAY_MS &&
        abs(durationSeconds - item.durationSeconds) < MAX_DURATION_DIFF_SECONDS
}

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val MAX_DURATION_DIFF_SECONDS = 10 * 60
