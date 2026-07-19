package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.podbelly.core.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publicationDate DESC")
    fun getByPodcastId(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun getById(id: Long): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): EpisodeEntity?

    /**
     * Looks up an episode by GUID alone, ignoring which feed it belongs to. GUIDs are
     * only unique *within* a feed (see the composite unique index), so this can return
     * an arbitrary row when two feeds share a GUID. Production code must use
     * [getByPodcastAndGuid]; this remains only for tests operating on a single feed.
     */
    @Query("SELECT * FROM episodes WHERE guid = :guid LIMIT 1")
    suspend fun getByGuid(guid: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND guid = :guid LIMIT 1")
    suspend fun getByPodcastAndGuid(podcastId: Long, guid: String): EpisodeEntity?

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId")
    suspend fun countByPodcastId(podcastId: Long): Int

    /**
     * Refreshes the feed-derived fields of an existing episode (identified by its feed
     * and GUID) while preserving user state (played / playbackPosition / download* /
     * lastPlayedAt). Lets publisher corrections (e.g. a changed audioUrl) propagate,
     * which a plain IGNORE insert never would.
     *
     * fileSize is only taken from the feed for *not-yet-downloaded* episodes. Once an
     * episode is downloaded its fileSize holds the real on-disk byte count (written by
     * [setDownloadPath]); the RSS enclosure length is frequently 0 or inaccurate, so
     * overwriting it would corrupt storage accounting (getTotalDownloadedBytes).
     */
    @Query(
        """
        UPDATE episodes
        SET title = :title,
            description = :description,
            audioUrl = :audioUrl,
            publicationDate = :publicationDate,
            durationSeconds = :durationSeconds,
            artworkUrl = :artworkUrl,
            fileSize = CASE WHEN downloadPath != '' THEN fileSize ELSE :fileSize END,
            transcriptUrl = :transcriptUrl,
            transcriptType = :transcriptType
        WHERE podcastId = :podcastId AND guid = :guid
        """
    )
    suspend fun updateFeedFields(
        podcastId: Long,
        guid: String,
        title: String,
        description: String,
        audioUrl: String,
        publicationDate: Long,
        durationSeconds: Int,
        artworkUrl: String,
        fileSize: Long,
        transcriptUrl: String,
        transcriptType: String,
    )

    @Query(
        """
        SELECT episodes.* FROM episodes
        INNER JOIN podcasts ON episodes.podcastId = podcasts.id
        WHERE podcasts.subscribed = 1
        ORDER BY episodes.publicationDate DESC
        LIMIT :limit
        """
    )
    fun getRecentEpisodes(limit: Int = 50): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND played = 0 ORDER BY publicationDate DESC")
    fun getUnplayedByPodcastId(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadPath != '' ORDER BY downloadedAt DESC")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadPath != ''")
    suspend fun getDownloadedEpisodesOnce(): List<EpisodeEntity>

    /** Downloaded episodes of one podcast, newest first. Powers the Android Auto browse tree. */
    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND downloadPath != '' ORDER BY publicationDate DESC")
    suspend fun getDownloadedByPodcastIdOnce(podcastId: Long): List<EpisodeEntity>

    /** Ids of podcasts that have at least one downloaded episode (Android Auto browse tree). */
    @Query("SELECT DISTINCT podcastId FROM episodes WHERE downloadPath != ''")
    suspend fun getPodcastIdsWithDownloads(): List<Long>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM episodes WHERE downloadPath != ''")
    fun getTotalDownloadedBytes(): Flow<Long>

    /** Downloads of episodes finished before [cutoff], for auto-cleanup. */
    @Query(
        """
        SELECT id FROM episodes
        WHERE downloadPath != '' AND played = 1
          AND lastPlayedAt > 0 AND lastPlayedAt < :cutoff
        """
    )
    suspend fun getPlayedDownloadIdsOlderThan(cutoff: Long): List<Long>

    /**
     * How well the user keeps up with incoming episodes: of the episodes a feed
     * refresh discovered since [since], how many have been played. Rows with
     * addedAt = 0 (a new subscription's imported backlog) are excluded — nobody
     * "falls behind" on a backlog they never intended to clear.
     */
    @Query(
        """
        SELECT COUNT(*) AS totalCount,
               COALESCE(SUM(played), 0) AS playedCount
        FROM episodes
        WHERE addedAt >= :since AND addedAt > 0
        """
    )
    fun getKeepUpStats(since: Long): Flow<KeepUpStat>

    /** Whole-library episode totals for the Stats library summary card. */
    @Query(
        """
        SELECT COUNT(*) AS episodeCount,
               COALESCE(SUM(played), 0) AS playedCount
        FROM episodes
        """
    )
    fun getLibraryStats(): Flow<LibraryStat>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("UPDATE episodes SET playbackPosition = :position, lastPlayedAt = (strftime('%s','now') * 1000) WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Long, position: Long)

    @Query("UPDATE episodes SET played = 1 WHERE id = :id")
    suspend fun markAsPlayed(id: Long)

    @Query("UPDATE episodes SET played = 0 WHERE id = :id")
    suspend fun markAsUnplayed(id: Long)

    @Query("UPDATE episodes SET downloadPath = :path, fileSize = :fileSize, downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setDownloadPath(id: Long, path: String, fileSize: Long, downloadedAt: Long)

    @Query("UPDATE episodes SET downloadPath = '', fileSize = 0, downloadedAt = 0, autoDownloaded = 0 WHERE id = :id")
    suspend fun clearDownload(id: Long)

    /** Flags a download as auto-queued (smart auto-download); see EpisodeEntity.autoDownloaded. */
    @Query("UPDATE episodes SET autoDownloaded = 1 WHERE id = :id")
    suspend fun markAutoDownloaded(id: Long)

    /**
     * Auto-downloaded, unplayed downloads of one show beyond the newest [keep]
     * (by publication date) — the ones "keep newest N per show" should delete.
     * Manual downloads (autoDownloaded = 0) and played episodes are never returned;
     * played ones are the auto-delete-after-N-days setting's business.
     */
    @Query(
        """
        SELECT id FROM episodes
        WHERE podcastId = :podcastId AND downloadPath != ''
          AND played = 0 AND autoDownloaded = 1
        ORDER BY publicationDate DESC
        LIMIT -1 OFFSET :keep
        """
    )
    suspend fun getAutoDownloadsBeyondNewest(podcastId: Long, keep: Int): List<Long>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteByPodcastId(podcastId: Long)

    @Query("SELECT * FROM episodes WHERE played = 1 AND downloadPath != ''")
    suspend fun getPlayedDownloadedEpisodes(): List<EpisodeEntity>

    // An episode can reach the end without the player firing STATE_ENDED (process
    // killed at the end, or feed-reported duration shorter than the real audio),
    // leaving played = 0 with position at/past the duration. Treat anything with
    // 30s or less remaining as finished so it can't linger in Continue Listening
    // showing "0 seconds left" (30s matches AntennaPod's smart-finish default).
    // Episodes at most 30s long (or with unknown duration, 0) are exempt so a
    // short episode isn't hidden the moment it starts.
    @Query(
        """
        SELECT episodes.* FROM episodes
        INNER JOIN podcasts ON episodes.podcastId = podcasts.id
        WHERE podcasts.subscribed = 1
          AND episodes.playbackPosition > 0
          AND episodes.played = 0
          AND episodes.downloadPath != ''
          AND (
            episodes.durationSeconds <= 30
            OR episodes.playbackPosition < (episodes.durationSeconds - 30) * 1000
          )
        ORDER BY episodes.lastPlayedAt DESC, episodes.publicationDate DESC
        """
    )
    fun getInProgressEpisodes(): Flow<List<EpisodeEntity>>

    // Powers the "New" section on Home: unplayed episodes a feed refresh discovered
    // after the given cutoff. addedAt is 0 for rows imported on initial subscribe,
    // so they can never match (cutoff is always >= 0).
    @Query(
        """
        SELECT episodes.* FROM episodes
        INNER JOIN podcasts ON episodes.podcastId = podcasts.id
        WHERE podcasts.subscribed = 1
          AND episodes.addedAt > :since
          AND episodes.played = 0
        ORDER BY episodes.publicationDate DESC
        """
    )
    fun getEpisodesAddedSince(since: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT podcastId, COUNT(*) AS unplayedCount FROM episodes WHERE played = 0 GROUP BY podcastId")
    fun getUnplayedCountsByPodcast(): Flow<List<PodcastUnplayedCount>>

    @Query(
        """
        SELECT episodes.* FROM episodes
        INNER JOIN podcasts ON episodes.podcastId = podcasts.id
        WHERE podcasts.subscribed = 1
          AND episodes.playbackPosition > 0
          AND episodes.played = 0
          AND episodes.downloadPath != ''
        ORDER BY episodes.lastPlayedAt DESC, episodes.publicationDate DESC
        LIMIT 1
        """
    )
    suspend fun getLastInProgressEpisode(): EpisodeEntity?

    @Query("SELECT podcastId, MAX(publicationDate) AS latestPublicationDate FROM episodes GROUP BY podcastId")
    fun getLatestEpisodeDateByPodcast(): Flow<List<PodcastLatestEpisode>>
}

data class PodcastLatestEpisode(
    val podcastId: Long,
    val latestPublicationDate: Long,
)

data class PodcastUnplayedCount(
    val podcastId: Long,
    val unplayedCount: Int,
)

/** Played-vs-arrived counts for the Stats keep-up rate. */
data class KeepUpStat(
    val totalCount: Int,
    val playedCount: Int,
)

/** Whole-library episode totals. */
data class LibraryStat(
    val episodeCount: Int,
    val playedCount: Int,
)
