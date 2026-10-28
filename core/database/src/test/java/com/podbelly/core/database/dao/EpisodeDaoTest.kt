package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.ListeningSessionEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.hasSameFeedFields
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EpisodeDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var episodeDao: EpisodeDao
    private lateinit var podcastDao: PodcastDao

    private var podcastId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        episodeDao = database.episodeDao()
        podcastDao = database.podcastDao()

        // Insert a parent podcast for foreign key constraints
        podcastId = podcastDao.insert(createPodcast())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPodcast(
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
        subscribed: Boolean = true,
    ) = PodcastEntity(
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        description = "Description",
        artworkUrl = "https://example.com/art.png",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 1000L,
        subscribed = subscribed,
        subscribedAt = 1000L,
    )

    private fun createEpisode(
        podcastId: Long = this.podcastId,
        guid: String = "guid-1",
        title: String = "Episode 1",
        description: String = "Episode description",
        audioUrl: String = "https://example.com/audio.mp3",
        publicationDate: Long = 1000L,
        durationSeconds: Int = 3600,
        artworkUrl: String = "",
        played: Boolean = false,
        playbackPosition: Long = 0L,
        downloadPath: String = "",
        downloadedAt: Long = 0L,
        fileSize: Long = 0L,
        addedAt: Long = 0L,
    ) = EpisodeEntity(
        podcastId = podcastId,
        guid = guid,
        title = title,
        description = description,
        audioUrl = audioUrl,
        publicationDate = publicationDate,
        durationSeconds = durationSeconds,
        artworkUrl = artworkUrl,
        played = played,
        playbackPosition = playbackPosition,
        downloadPath = downloadPath,
        downloadedAt = downloadedAt,
        fileSize = fileSize,
        addedAt = addedAt,
    )

    @Test
    fun `insertAll with IGNORE does not overwrite existing episodes with same guid`() = runTest {
        val original = createEpisode(guid = "guid-1", title = "Original Title")
        episodeDao.insertAll(listOf(original))

        val duplicate = createEpisode(guid = "guid-1", title = "New Title")
        episodeDao.insertAll(listOf(duplicate))

        val episode = episodeDao.getByGuid("guid-1")
        assertNotNull(episode)
        assertEquals("Original Title", episode!!.title)
    }

    @Test
    fun `getByPodcastId returns episodes ordered by publicationDate DESC`() = runTest {
        val episodes = listOf(
            createEpisode(guid = "old", title = "Old Episode", publicationDate = 1000L),
            createEpisode(guid = "mid", title = "Mid Episode", publicationDate = 2000L),
            createEpisode(guid = "new", title = "New Episode", publicationDate = 3000L),
        )
        episodeDao.insertAll(episodes)

        episodeDao.getByPodcastId(podcastId).test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("New Episode", items[0].title)
            assertEquals("Mid Episode", items[1].title)
            assertEquals("Old Episode", items[2].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getRecentEpisodes returns episodes from subscribed podcasts only`() = runTest {
        val unsubscribedPodcastId = podcastDao.insert(
            createPodcast(feedUrl = "https://unsubscribed.com/feed", title = "Unsubscribed", subscribed = false)
        )

        episodeDao.insertAll(
            listOf(
                createEpisode(podcastId = podcastId, guid = "sub-ep", title = "Subscribed Episode", publicationDate = 2000L),
                createEpisode(podcastId = unsubscribedPodcastId, guid = "unsub-ep", title = "Unsubscribed Episode", publicationDate = 3000L),
            )
        )

        episodeDao.getRecentEpisodes(50).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Subscribed Episode", items[0].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getByIdOnce returns correct episode`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "target", title = "Target Episode")))
        val target = episodeDao.getByGuid("target")!!

        val result = episodeDao.getByIdOnce(target.id)
        assertNotNull(result)
        assertEquals("Target Episode", result!!.title)
        assertEquals(target.id, result.id)
    }

    @Test
    fun `getByIdOnce returns null for nonexistent id`() = runTest {
        val result = episodeDao.getByIdOnce(999L)
        assertNull(result)
    }

    @Test
    fun `getByGuid returns correct episode`() = runTest {
        episodeDao.insertAll(
            listOf(
                createEpisode(guid = "guid-abc", title = "ABC"),
                createEpisode(guid = "guid-def", title = "DEF"),
            )
        )

        val episode = episodeDao.getByGuid("guid-abc")
        assertNotNull(episode)
        assertEquals("ABC", episode!!.title)
    }

    @Test
    fun `getByGuid returns null for nonexistent guid`() = runTest {
        val result = episodeDao.getByGuid("nonexistent")
        assertNull(result)
    }

    @Test
    fun `markAsPlayed sets played to true`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1", played = false)))
        val episode = episodeDao.getByGuid("ep1")!!

        episodeDao.markAsPlayed(episode.id)

        val updated = episodeDao.getByIdOnce(episode.id)
        assertNotNull(updated)
        assertTrue(updated!!.played)
    }

    @Test
    fun `markAsUnplayed sets played to false`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1", played = true)))
        val episode = episodeDao.getByGuid("ep1")!!

        // The episode was inserted with played=true via IGNORE, but since this is a fresh insert it should work.
        // We first mark as played to ensure the state, then mark unplayed.
        episodeDao.markAsPlayed(episode.id)
        episodeDao.markAsUnplayed(episode.id)

        val updated = episodeDao.getByIdOnce(episode.id)
        assertNotNull(updated)
        assertEquals(false, updated!!.played)
    }

    @Test
    fun `updatePlaybackPosition updates position`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1")))
        val episode = episodeDao.getByGuid("ep1")!!

        episodeDao.updatePlaybackPosition(episode.id, 45_000L)

        val updated = episodeDao.getByIdOnce(episode.id)
        assertNotNull(updated)
        assertEquals(45_000L, updated!!.playbackPosition)
    }

    @Test
    fun `setDownloadPath sets download fields`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1")))
        val episode = episodeDao.getByGuid("ep1")!!

        val path = "/storage/downloads/episode.mp3"
        val fileSize = 50_000_000L
        val downloadedAt = System.currentTimeMillis()
        episodeDao.setDownloadPath(episode.id, path, fileSize, downloadedAt)

        val updated = episodeDao.getByIdOnce(episode.id)
        assertNotNull(updated)
        assertEquals(path, updated!!.downloadPath)
        assertEquals(fileSize, updated.fileSize)
        assertEquals(downloadedAt, updated.downloadedAt)
    }

    @Test
    fun `clearDownload resets download fields`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1")))
        val episode = episodeDao.getByGuid("ep1")!!

        // First set download, then clear it
        episodeDao.setDownloadPath(episode.id, "/some/path.mp3", 1000L, 9999L)
        episodeDao.clearDownload(episode.id)

        val updated = episodeDao.getByIdOnce(episode.id)
        assertNotNull(updated)
        assertEquals("", updated!!.downloadPath)
        assertEquals(0L, updated.fileSize)
        assertEquals(0L, updated.downloadedAt)
    }

    @Test
    fun `deleteByPodcastId removes all episodes for that podcast`() = runTest {
        val otherPodcastId = podcastDao.insert(
            createPodcast(feedUrl = "https://other.com/feed", title = "Other Podcast")
        )

        episodeDao.insertAll(
            listOf(
                createEpisode(podcastId = podcastId, guid = "ep1", title = "EP1"),
                createEpisode(podcastId = podcastId, guid = "ep2", title = "EP2"),
                createEpisode(podcastId = otherPodcastId, guid = "other-ep", title = "Other EP"),
            )
        )

        episodeDao.deleteByPodcastId(podcastId)

        episodeDao.getByPodcastId(podcastId).test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndConsumeRemainingEvents()
        }

        // Other podcast's episodes should be untouched
        episodeDao.getByPodcastId(otherPodcastId).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Other EP", items[0].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getInProgressEpisodes excludes episodes with 30s or less remaining`() = runTest {
        episodeDao.insertAll(
            listOf(
                createEpisode(guid = "halfway", playbackPosition = 1_800_000L, durationSeconds = 3600, downloadPath = "/f"),
                createEpisode(guid = "at-end", playbackPosition = 3_600_000L, durationSeconds = 3600, downloadPath = "/f"),
                createEpisode(guid = "past-end", playbackPosition = 3_700_000L, durationSeconds = 3600, downloadPath = "/f"),
                createEpisode(guid = "15s-left", playbackPosition = 3_585_000L, durationSeconds = 3600, downloadPath = "/f"),
                createEpisode(guid = "45s-left", playbackPosition = 3_555_000L, durationSeconds = 3600, downloadPath = "/f"),
                createEpisode(guid = "unknown-duration", playbackPosition = 500_000L, durationSeconds = 0, downloadPath = "/f"),
                createEpisode(guid = "short-episode", playbackPosition = 5_000L, durationSeconds = 20, downloadPath = "/f"),
            )
        )

        episodeDao.getInProgressEpisodes().test {
            val guids = awaitItem().map { it.guid }.toSet()
            assertEquals(setOf("halfway", "45s-left", "unknown-duration", "short-episode"), guids)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getEpisodesAddedSince returns only episodes discovered after the cutoff ordered by publicationDate`() = runTest {
        val unsubscribedPodcastId = podcastDao.insert(
            createPodcast(feedUrl = "https://unsubscribed.com/feed", title = "Unsubscribed", subscribed = false)
        )

        episodeDao.insertAll(
            listOf(
                createEpisode(guid = "backlog", addedAt = 0L, publicationDate = 9_000L),
                createEpisode(guid = "seen", addedAt = 4_000L, publicationDate = 8_000L),
                createEpisode(guid = "fresh-old-pub", addedAt = 6_000L, publicationDate = 1_000L),
                createEpisode(guid = "fresh-new-pub", addedAt = 5_000L, publicationDate = 7_000L),
                createEpisode(guid = "fresh-played", addedAt = 6_000L, publicationDate = 7_200L, played = true),
                createEpisode(podcastId = unsubscribedPodcastId, guid = "fresh-unsub", addedAt = 6_000L, publicationDate = 7_500L),
            )
        )

        episodeDao.getEpisodesAddedSince(4_000L).test {
            val guids = awaitItem().map { it.guid }
            // Only unplayed subscribed episodes above the cutoff, newest publication first.
            assertEquals(listOf("fresh-new-pub", "fresh-old-pub"), guids)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `CASCADE deleting a podcast also deletes its episodes`() = runTest {
        episodeDao.insertAll(
            listOf(
                createEpisode(podcastId = podcastId, guid = "ep1"),
                createEpisode(podcastId = podcastId, guid = "ep2"),
            )
        )

        // Verify episodes exist
        episodeDao.getByPodcastId(podcastId).test {
            assertEquals(2, awaitItem().size)
            cancelAndConsumeRemainingEvents()
        }

        // Delete the parent podcast
        val podcast = podcastDao.getByFeedUrl("https://example.com/feed.xml")!!
        podcastDao.delete(podcast)

        // Episodes should be cascade-deleted
        episodeDao.getByPodcastId(podcastId).test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `transcript fields default to empty`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1")))

        val episode = episodeDao.getByGuid("ep1")!!
        assertEquals("", episode.transcriptUrl)
        assertEquals("", episode.transcriptType)
    }

    @Test
    fun `updateFeedFields backfills transcript on existing episodes`() = runTest {
        // Episode imported before the feed declared a transcript (transcript empty).
        episodeDao.insertAll(listOf(createEpisode(guid = "ep1", playbackPosition = 5000L)))

        // A later refresh delivers the transcript.
        episodeDao.updateFeedFields(
            podcastId = podcastId,
            guid = "ep1",
            title = "Episode 1",
            description = "Episode description",
            audioUrl = "https://example.com/audio.mp3",
            publicationDate = 1000L,
            durationSeconds = 3600,
            artworkUrl = "",
            fileSize = 0L,
            transcriptUrl = "https://example.com/ep1.vtt",
            transcriptType = "text/vtt",
        )

        val episode = episodeDao.getByGuid("ep1")!!
        assertEquals("https://example.com/ep1.vtt", episode.transcriptUrl)
        assertEquals("text/vtt", episode.transcriptType)
        // User state is untouched by the feed-field refresh.
        assertEquals(5000L, episode.playbackPosition)
    }

    @Test
    fun `getAutoDownloadsBeyondNewest returns only older auto-downloads past the cap`() = runTest {
        val ids = episodeDao.insertAll(
            listOf(
                createEpisode(guid = "a", publicationDate = 4000L, downloadPath = "/f/a.mp3"),
                createEpisode(guid = "b", publicationDate = 3000L, downloadPath = "/f/b.mp3"),
                createEpisode(guid = "c", publicationDate = 2000L, downloadPath = "/f/c.mp3"),
                // Manual download: never eligible for the cap cleanup.
                createEpisode(guid = "d", publicationDate = 1000L, downloadPath = "/f/d.mp3"),
                // Played: the auto-delete-after-N-days setting's business, not this one's.
                createEpisode(guid = "e", publicationDate = 500L, downloadPath = "/f/e.mp3", played = true),
            )
        )
        episodeDao.markAutoDownloaded(ids[0])
        episodeDao.markAutoDownloaded(ids[1])
        episodeDao.markAutoDownloaded(ids[2])
        episodeDao.markAutoDownloaded(ids[4])

        val beyond = episodeDao.getAutoDownloadsBeyondNewest(podcastId, 1)

        // Newest auto-download (a) is kept; b and c are beyond the cap; d (manual)
        // and e (played) are untouched.
        assertEquals(listOf(ids[1], ids[2]), beyond)
    }

    @Test
    fun `clearDownload resets the autoDownloaded flag`() = runTest {
        val ids = episodeDao.insertAll(
            listOf(createEpisode(guid = "a", publicationDate = 1000L, downloadPath = "/f/a.mp3"))
        )
        episodeDao.markAutoDownloaded(ids[0])
        assertEquals(listOf(ids[0]), episodeDao.getAutoDownloadsBeyondNewest(podcastId, 0))

        episodeDao.clearDownload(ids[0])

        assertTrue(episodeDao.getAutoDownloadsBeyondNewest(podcastId, 0).isEmpty())
        assertEquals(false, episodeDao.getByIdOnce(ids[0])!!.autoDownloaded)
    }

    private suspend fun refreshFeedDate(guid: String, publicationDate: Long) {
        episodeDao.updateFeedFields(
            podcastId = podcastId,
            guid = guid,
            title = "Episode 1",
            description = "Episode description",
            audioUrl = "https://example.com/audio.mp3",
            publicationDate = publicationDate,
            durationSeconds = 3600,
            artworkUrl = "",
            fileSize = 0L,
            transcriptUrl = "",
            transcriptType = "",
        )
    }

    @Test
    fun `insertAll dates a future-dated episode when it was discovered`() = runTest {
        val before = System.currentTimeMillis()
        val tomorrow = before + DAY_MS
        episodeDao.insertAll(listOf(createEpisode(guid = "early", publicationDate = tomorrow)))
        val after = System.currentTimeMillis()

        val date = episodeDao.getByGuid("early")!!.publicationDate
        assertTrue("expected discovery time, was $date", date in before..after)
    }

    @Test
    fun `insertAll keeps past publication dates as published`() = runTest {
        val yesterday = System.currentTimeMillis() - DAY_MS
        episodeDao.insertAll(listOf(createEpisode(guid = "old", publicationDate = yesterday)))

        assertEquals(yesterday, episodeDao.getByGuid("old")!!.publicationDate)
    }

    @Test
    fun `updateFeedFields keeps the stored date while the feed date is still in the future`() = runTest {
        val discovered = System.currentTimeMillis() - 60_000L
        episodeDao.insertAll(listOf(createEpisode(guid = "early", publicationDate = discovered)))

        refreshFeedDate("early", System.currentTimeMillis() + DAY_MS)

        assertEquals(discovered, episodeDao.getByGuid("early")!!.publicationDate)
    }

    @Test
    fun `updateFeedFields adopts the feed date once it has passed`() = runTest {
        val discovered = System.currentTimeMillis() - 60_000L
        episodeDao.insertAll(listOf(createEpisode(guid = "early", publicationDate = discovered)))

        val published = System.currentTimeMillis() - 1_000L
        refreshFeedDate("early", published)

        assertEquals(published, episodeDao.getByGuid("early")!!.publicationDate)
    }

    @Test
    fun `updateFeedFields repairs a stored future date to when the episode was discovered`() = runTest {
        // A row saved before future dates were clamped on insert.
        val addedAt = System.currentTimeMillis() - 60_000L
        val tomorrow = System.currentTimeMillis() + DAY_MS
        episodeDao.insertAllAsIs(
            listOf(createEpisode(guid = "early", publicationDate = tomorrow, addedAt = addedAt))
        )

        refreshFeedDate("early", tomorrow)

        assertEquals(addedAt, episodeDao.getByGuid("early")!!.publicationDate)
    }

    @Test
    fun `hasSameFeedFields ignores a future feed date that would not be applied`() {
        val now = 10_000_000L
        val stored = createEpisode(publicationDate = now - 1_000L)

        fun same(publicationDate: Long) = stored.hasSameFeedFields(
            title = stored.title,
            description = stored.description,
            audioUrl = stored.audioUrl,
            publicationDate = publicationDate,
            durationSeconds = stored.durationSeconds,
            artworkUrl = stored.artworkUrl,
            fileSize = stored.fileSize,
            transcriptUrl = stored.transcriptUrl,
            transcriptType = stored.transcriptType,
            now = now,
        )

        assertTrue(same(now + DAY_MS))
        assertEquals(false, same(now - 2_000L))

        // A stored future date (saved before clamping) must still be written so the
        // update can repair it, even when the feed repeats the same value.
        val storedFuture = stored.copy(publicationDate = now + DAY_MS)
        assertEquals(
            false,
            storedFuture.hasSameFeedFields(
                title = stored.title,
                description = stored.description,
                audioUrl = stored.audioUrl,
                publicationDate = now + DAY_MS,
                durationSeconds = stored.durationSeconds,
                artworkUrl = stored.artworkUrl,
                fileSize = stored.fileSize,
                transcriptUrl = stored.transcriptUrl,
                transcriptType = stored.transcriptType,
                now = now,
            ),
        )
    }

    // -- repairChangedGuids --

    private fun feedItem(
        guid: String,
        title: String = "Episode 1",
        audioUrl: String = "https://example.com/audio.mp3",
        publicationDate: Long = 1000L,
        durationSeconds: Int = 3600,
    ) = FeedItemKey(guid, title, audioUrl, publicationDate, durationSeconds)

    @Test
    fun `repairChangedGuids re-keys an episode whose GUID the host changed`() = runTest {
        val ids = episodeDao.insertAll(
            listOf(createEpisode(guid = "substack:post:1", played = true, addedAt = 500L))
        )

        val repaired = episodeDao.repairChangedGuids(podcastId, listOf(feedItem("substack:post:1:unlocked")))

        assertEquals(1, repaired)
        val episode = episodeDao.getByPodcastAndGuid(podcastId, "substack:post:1:unlocked")!!
        // Same row: play state kept, and its original addedAt keeps it out of New.
        assertEquals(ids[0], episode.id)
        assertTrue(episode.played)
        assertEquals(500L, episode.addedAt)
        assertEquals(1, episodeDao.countByPodcastId(podcastId))
    }

    @Test
    fun `repairChangedGuids matches by title, day and length when the audio URL changed too`() = runTest {
        val day = 20_000L * DAY_MS
        episodeDao.insertAll(
            listOf(
                createEpisode(
                    guid = "old",
                    audioUrl = "https://old-host.example/ep.mp3",
                    publicationDate = day + 1_000L,
                    durationSeconds = 3600,
                )
            )
        )

        val repaired = episodeDao.repairChangedGuids(
            podcastId,
            listOf(
                feedItem(
                    guid = "new",
                    audioUrl = "https://tracker.example/new-host.example/ep.mp3",
                    publicationDate = day + 5_000L,
                    durationSeconds = 3540,
                )
            ),
        )

        assertEquals(1, repaired)
        assertNotNull(episodeDao.getByPodcastAndGuid(podcastId, "new"))
        assertNull(episodeDao.getByPodcastAndGuid(podcastId, "old"))
    }

    @Test
    fun `repairChangedGuids folds a copy already inserted under the new GUID into the original`() = runTest {
        val ids = episodeDao.insertAll(
            listOf(
                createEpisode(guid = "substack:post:1", played = true, addedAt = 500L),
                createEpisode(
                    guid = "substack:post:1:unlocked",
                    playbackPosition = 9_000L,
                    downloadPath = "/files/copy.mp3",
                    downloadedAt = 7_000L,
                    fileSize = 1234L,
                    addedAt = 99_000L,
                ),
            )
        )
        database.listeningSessionDao().insert(
            ListeningSessionEntity(episodeId = ids[1], podcastId = podcastId, startedAt = 8_000L)
        )

        val repaired = episodeDao.repairChangedGuids(podcastId, listOf(feedItem("substack:post:1:unlocked")))

        assertEquals(1, repaired)
        assertEquals(1, episodeDao.countByPodcastId(podcastId))
        val episode = episodeDao.getByPodcastAndGuid(podcastId, "substack:post:1:unlocked")!!
        assertEquals(ids[0], episode.id)
        assertEquals(500L, episode.addedAt)
        assertTrue(episode.played)
        assertEquals(9_000L, episode.playbackPosition)
        assertEquals("/files/copy.mp3", episode.downloadPath)
        assertEquals(1234L, episode.fileSize)
        // Moved, not cascade-deleted with the copy.
        assertEquals(1, database.listeningSessionDao().getSessionCount().first())
    }

    @Test
    fun `repairChangedGuids leaves same-titled episodes alone while both are in the feed`() = runTest {
        episodeDao.insertAll(listOf(createEpisode(guid = "a"), createEpisode(guid = "b")))

        val repaired = episodeDao.repairChangedGuids(podcastId, listOf(feedItem("a"), feedItem("b")))

        assertEquals(0, repaired)
        assertEquals(2, episodeDao.countByPodcastId(podcastId))
    }

    @Test
    fun `repairChangedGuids does not match an unrelated episode that left the feed`() = runTest {
        episodeDao.insertAll(
            listOf(
                createEpisode(
                    guid = "dropped",
                    title = "An Old Episode",
                    audioUrl = "https://example.com/old.mp3",
                )
            )
        )

        val repaired = episodeDao.repairChangedGuids(
            podcastId,
            listOf(feedItem("brand-new", title = "A New Episode", audioUrl = "https://example.com/new.mp3")),
        )

        assertEquals(0, repaired)
        assertNotNull(episodeDao.getByPodcastAndGuid(podcastId, "dropped"))
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
