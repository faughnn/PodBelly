package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
     *
     * A publicationDate still in the future is not taken yet (see [insertAll]): the
     * stored date is kept, a stored future date (saved before this rule existed)
     * falls back to when the episode was discovered, and the feed's real date is
     * adopted once it has passed. Otherwise every refresh would re-pin a
     * pre-scheduled episode to the top of date-sorted lists.
     */
    @Query(
        """
        UPDATE episodes
        SET title = :title,
            description = :description,
            audioUrl = :audioUrl,
            publicationDate = CASE
                WHEN :publicationDate <= $NOW_MS THEN :publicationDate
                WHEN publicationDate BETWEEN 1 AND $NOW_MS THEN publicationDate
                WHEN addedAt BETWEEN 1 AND $NOW_MS THEN addedAt
                ELSE $NOW_MS
            END,
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

    /**
     * Inserts new episodes, ignoring ones already stored. Some publishers put an episode
     * in the feed with a pubDate hours or days ahead (e.g. scheduled for tomorrow but
     * already live); taken at face value it shows as "In 4 hr" and sits above every
     * genuinely newer episode until that date passes. Like AntennaPod (which discards a
     * future pubDate and falls back to the time the item was first seen), such an
     * episode is dated when it was discovered instead. [updateFeedFields] adopts the
     * feed's date once it's no longer in the future.
     */
    suspend fun insertAll(episodes: List<EpisodeEntity>): List<Long> {
        val now = System.currentTimeMillis()
        return insertAllAsIs(
            episodes.map { if (it.publicationDate > now) it.copy(publicationDate = now) else it }
        )
    }

    /** Raw insert behind [insertAll]; call that instead so future dates get clamped. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAsIs(episodes: List<EpisodeEntity>): List<Long>

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

    /**
     * Repairs episodes whose GUID the publisher changed. Run before a refresh matches
     * the feed against stored episodes by GUID. Some hosts rewrite GUIDs across a whole
     * back catalogue (Substack added an ":unlocked" suffix to most of Blocked and
     * Reported's episodes), and every rewritten episode then looked brand new: it was
     * inserted a second time and flooded Home's New section.
     *
     * Adapted from AntennaPod's FeedDatabaseWriter + FeedItemDuplicateGuesser. A
     * stored episode whose GUID is gone from the feed, and which [seemsSameEpisodeAs]
     * a feed item, takes that item's GUID, keeping its play state, download and
     * addedAt, so the refresh updates it instead of inserting a copy. If an earlier
     * refresh already inserted that copy, the copy's play state, download and
     * listening history move to the original and the copy is deleted. Episodes still
     * in the feed under their own GUID are never matched, so two genuinely separate
     * episodes with the same title are left alone.
     *
     * @return how many episodes were repaired.
     */
    @Transaction
    suspend fun repairChangedGuids(podcastId: Long, feedItems: List<FeedItemKey>): Int {
        val feedGuids = feedItems.mapTo(HashSet()) { it.guid }
        val storedGuids = getGuidsForPodcast(podcastId)
        // Fast path: nearly every refresh, every stored GUID is still in the feed.
        if (storedGuids.all { it in feedGuids }) return 0

        val stored = getMatchRowsForPodcast(podcastId)
        val storedByGuid = stored.associateBy { it.guid }
        val stale = stored.filter { it.guid !in feedGuids }
        val staleByAudioUrl = stale.filter { it.audioUrl.isNotEmpty() }.groupBy { it.audioUrl }
        val staleByTitle = stale.groupBy { normalizeTitleForMatch(it.title) }
        val claimed = HashSet<Long>()
        var repaired = 0

        for (item in feedItems) {
            val candidates = staleByAudioUrl[item.audioUrl].orEmpty() +
                staleByTitle[normalizeTitleForMatch(item.title)].orEmpty()
            val original = candidates.firstOrNull { it.id !in claimed && it.seemsSameEpisodeAs(item) }
                ?: continue

            val copy = storedByGuid[item.guid]
            if (copy != null) {
                // Both downloaded: deleting the copy's row would orphan its file.
                if (copy.downloadPath.isNotBlank() && original.downloadPath.isNotBlank()) continue
                if (copy.played || copy.playbackPosition > 0L || copy.lastPlayedAt > 0L) {
                    mergePlayState(
                        id = original.id,
                        played = copy.played,
                        position = copy.playbackPosition,
                        lastPlayedAt = copy.lastPlayedAt,
                    )
                }
                if (copy.downloadPath.isNotBlank()) {
                    adoptDownloadIfNone(
                        id = original.id,
                        path = copy.downloadPath,
                        downloadedAt = copy.downloadedAt,
                        fileSize = copy.fileSize,
                        autoDownloaded = copy.autoDownloaded,
                    )
                }
                moveListeningSessions(fromEpisodeId = copy.id, toEpisodeId = original.id)
                // Frees the GUID before the original takes it (unique per podcast).
                deleteById(copy.id)
            }
            setGuid(original.id, item.guid)
            claimed += original.id
            repaired++
        }
        return repaired
    }

    @Query("SELECT guid FROM episodes WHERE podcastId = :podcastId")
    suspend fun getGuidsForPodcast(podcastId: Long): List<String>

    @Query(
        """
        SELECT id, guid, title, audioUrl, publicationDate, durationSeconds, played,
            playbackPosition, lastPlayedAt, downloadPath, downloadedAt, fileSize, autoDownloaded
        FROM episodes WHERE podcastId = :podcastId
        """
    )
    suspend fun getMatchRowsForPodcast(podcastId: Long): List<EpisodeMatchRow>

    /** Play state only moves forward: played can only turn on, position/lastPlayedAt only grow. */
    @Query(
        """
        UPDATE episodes
        SET played = CASE WHEN :played THEN 1 ELSE played END,
            playbackPosition = MAX(playbackPosition, :position),
            lastPlayedAt = MAX(lastPlayedAt, :lastPlayedAt)
        WHERE id = :id
        """
    )
    suspend fun mergePlayState(id: Long, played: Boolean, position: Long, lastPlayedAt: Long)

    @Query(
        """
        UPDATE episodes
        SET downloadPath = :path, downloadedAt = :downloadedAt, fileSize = :fileSize,
            autoDownloaded = :autoDownloaded
        WHERE id = :id AND downloadPath = ''
        """
    )
    suspend fun adoptDownloadIfNone(
        id: Long,
        path: String,
        downloadedAt: Long,
        fileSize: Long,
        autoDownloaded: Boolean,
    )

    @Query("UPDATE listening_sessions SET episodeId = :toEpisodeId WHERE episodeId = :fromEpisodeId")
    suspend fun moveListeningSessions(fromEpisodeId: Long, toEpisodeId: Long)

    @Query("UPDATE episodes SET guid = :guid WHERE id = :id")
    suspend fun setGuid(id: Long, guid: String)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteById(id: Long)

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

/** SQLite's current time in epoch millis, for queries comparing against stored dates. */
private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
