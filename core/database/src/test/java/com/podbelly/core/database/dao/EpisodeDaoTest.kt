package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
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
}
