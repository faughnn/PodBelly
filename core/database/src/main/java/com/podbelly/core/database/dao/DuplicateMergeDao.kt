package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.podbelly.core.database.entity.EpisodeEntity

/**
 * Normalization used to decide two shows (or two episodes of duplicate feeds)
 * are "the same": lowercase, punctuation collapsed to spaces, whitespace
 * collapsed. "The Rest Is Politics: US" == "the rest is politics us".
 */
fun normalizeTitleForMatch(title: String): String =
    title.lowercase().replace(NON_ALNUM, " ").trim()

private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")

/**
 * Merges one duplicate subscription into the copy being kept: per-episode play
 * state, downloads and listening history move to the kept feed's matching
 * episodes (matched by normalized title), then the spare is unsubscribed.
 */
@Dao
interface DuplicateMergeDao {

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId")
    suspend fun episodesOf(podcastId: Long): List<EpisodeEntity>

    /**
     * Absorbs play state from a duplicate episode without ever regressing the
     * target: played can only turn on, position/lastPlayedAt only move forward.
     */
    @Query(
        """
        UPDATE episodes
        SET played = CASE WHEN :played = 1 THEN 1 ELSE played END,
            playbackPosition = MAX(playbackPosition, :position),
            lastPlayedAt = MAX(lastPlayedAt, :lastPlayedAt)
        WHERE id = :id
        """
    )
    suspend fun absorbPlayState(id: Long, played: Int, position: Long, lastPlayedAt: Long)

    /** Adopts the duplicate's downloaded file, only if the target has none. */
    @Query(
        """
        UPDATE episodes
        SET downloadPath = :path, downloadedAt = :downloadedAt, fileSize = :fileSize
        WHERE id = :id AND downloadPath = ''
        """
    )
    suspend fun adoptDownload(id: Long, path: String, downloadedAt: Long, fileSize: Long)

    /** Detaches the file from the duplicate so storage isn't double-counted. */
    @Query("UPDATE episodes SET downloadPath = '', downloadedAt = 0, fileSize = 0 WHERE id = :id")
    suspend fun releaseDownload(id: Long)

    @Query(
        """
        UPDATE listening_sessions
        SET episodeId = :toEpisodeId, podcastId = :toPodcastId
        WHERE episodeId = :fromEpisodeId
        """
    )
    suspend fun reassignEpisodeSessions(fromEpisodeId: Long, toEpisodeId: Long, toPodcastId: Long)

    /** Sessions on episodes with no counterpart still count toward the kept show. */
    @Query("UPDATE listening_sessions SET podcastId = :toPodcastId WHERE podcastId = :fromPodcastId")
    suspend fun reassignRemainingSessions(fromPodcastId: Long, toPodcastId: Long)

    @Query("UPDATE podcasts SET subscribed = 0 WHERE id = :id")
    suspend fun unsubscribe(id: Long)

    @Transaction
    suspend fun merge(fromPodcastId: Long, toPodcastId: Long) {
        if (fromPodcastId == toPodcastId) return
        val from = episodesOf(fromPodcastId)
        val to = episodesOf(toPodcastId)
        val toByTitle = to.groupBy { normalizeTitleForMatch(it.title) }

        for (src in from) {
            val match = toByTitle[normalizeTitleForMatch(src.title)]?.firstOrNull() ?: continue
            if (src.played || src.playbackPosition > 0L || src.lastPlayedAt > 0L) {
                absorbPlayState(
                    id = match.id,
                    played = if (src.played) 1 else 0,
                    position = src.playbackPosition,
                    lastPlayedAt = src.lastPlayedAt,
                )
            }
            if (src.downloadPath.isNotBlank() && match.downloadPath.isBlank()) {
                adoptDownload(match.id, src.downloadPath, src.downloadedAt, src.fileSize)
                releaseDownload(src.id)
            }
            reassignEpisodeSessions(src.id, match.id, toPodcastId)
        }

        reassignRemainingSessions(fromPodcastId, toPodcastId)
        unsubscribe(fromPodcastId)
    }
}
