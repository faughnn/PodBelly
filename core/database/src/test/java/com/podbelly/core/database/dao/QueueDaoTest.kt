package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class QueueDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var queueDao: QueueDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var podcastDao: PodcastDao

    private var podcastId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        queueDao = database.queueDao()
        episodeDao = database.episodeDao()
        podcastDao = database.podcastDao()

        // Insert a parent podcast for foreign key constraints
        podcastId = podcastDao.insert(
            PodcastEntity(
                feedUrl = "https://example.com/feed.xml",
                title = "Test Podcast",
                author = "Author",
                description = "Description",
                artworkUrl = "https://example.com/art.png",
                link = "https://example.com",
                language = "en",
                lastBuildDate = 1000L,
                subscribed = true,
                subscribedAt = 1000L,
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createEpisode(
        guid: String,
        title: String = "Episode",
        publicationDate: Long = 1000L,
    ) = EpisodeEntity(
        podcastId = podcastId,
        guid = guid,
        title = title,
        description = "Description",
        audioUrl = "https://example.com/$guid.mp3",
        publicationDate = publicationDate,
        durationSeconds = 3600,
    )

    private fun createQueueItem(
        episodeId: Long,
        position: Int,
        addedAt: Long = System.currentTimeMillis(),
    ) = QueueItemEntity(
        episodeId = episodeId,
        position = position,
        addedAt = addedAt,
    )

    /** Insert an episode and return its generated ID. */
    private suspend fun insertEpisode(guid: String, title: String = "Episode $guid"): Long {
        episodeDao.insertAll(listOf(createEpisode(guid = guid, title = title)))
        return episodeDao.getByGuid(guid)!!.id
    }

    @Test
    fun `addToQueue inserts item`() = runTest {
        val episodeId = insertEpisode("ep1")

        queueDao.addToQueue(createQueueItem(episodeId = episodeId, position = 0))

        queueDao.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(episodeId, items[0].episodeId)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getQueueWithEpisodes returns items with episode data joined`() = runTest {
        val ep1Id = insertEpisode("ep1", title = "First Episode")
        val ep2Id = insertEpisode("ep2", title = "Second Episode")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))

        queueDao.getQueueWithEpisodes().test {
            val items = awaitItem()
            assertEquals(2, items.size)

            // Ordered by position ASC
            assertEquals(ep1Id, items[0].queueItem.episodeId)
            assertEquals("First Episode", items[0].episode.title)
            assertEquals(0, items[0].queueItem.position)

            assertEquals(ep2Id, items[1].queueItem.episodeId)
            assertEquals("Second Episode", items[1].episode.title)
            assertEquals(1, items[1].queueItem.position)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `removeFromQueue removes by episodeId`() = runTest {
        val ep1Id = insertEpisode("ep1")
        val ep2Id = insertEpisode("ep2")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))

        queueDao.removeFromQueue(ep1Id)

        queueDao.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(ep2Id, items[0].episodeId)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `clearQueue removes all items`() = runTest {
        val ep1Id = insertEpisode("ep1")
        val ep2Id = insertEpisode("ep2")
        val ep3Id = insertEpisode("ep3")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))
        queueDao.addToQueue(createQueueItem(episodeId = ep3Id, position = 2))

        queueDao.clearQueue()

        queueDao.getAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updatePositions reorders items correctly`() = runTest {
        val ep1Id = insertEpisode("ep1")
        val ep2Id = insertEpisode("ep2")
        val ep3Id = insertEpisode("ep3")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))
        queueDao.addToQueue(createQueueItem(episodeId = ep3Id, position = 2))

        // Get current items to have their IDs for update
        val currentItems = queueDao.getQueueOnce()
        assertEquals(3, currentItems.size)

        // Reverse the order: ep3 -> 0, ep2 -> 1, ep1 -> 2
        val reordered = listOf(
            currentItems[2].queueItem.copy(position = 0),
            currentItems[1].queueItem.copy(position = 1),
            currentItems[0].queueItem.copy(position = 2),
        )
        queueDao.updatePositions(reordered)

        queueDao.getAll().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            // Ordered by position ASC
            assertEquals(ep3Id, items[0].episodeId)
            assertEquals(0, items[0].position)
            assertEquals(ep2Id, items[1].episodeId)
            assertEquals(1, items[1].position)
            assertEquals(ep1Id, items[2].episodeId)
            assertEquals(2, items[2].position)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getNextInQueue returns item at lowest position`() = runTest {
        val ep1Id = insertEpisode("ep1", title = "First")
        val ep2Id = insertEpisode("ep2", title = "Second")

        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 5))
        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 1))

        val next = queueDao.getNextInQueue()
        assertNotNull(next)
        assertEquals(ep1Id, next!!.queueItem.episodeId)
        assertEquals("First", next.episode.title)
    }

    @Test
    fun `getNextInQueue returns null when queue is empty`() = runTest {
        val next = queueDao.getNextInQueue()
        assertNull(next)
    }

    @Test
    fun `getQueueOnce returns current snapshot`() = runTest {
        val ep1Id = insertEpisode("ep1", title = "Alpha")
        val ep2Id = insertEpisode("ep2", title = "Beta")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))

        val queue = queueDao.getQueueOnce()
        assertEquals(2, queue.size)
        assertEquals(ep1Id, queue[0].queueItem.episodeId)
        assertEquals("Alpha", queue[0].episode.title)
        assertEquals(ep2Id, queue[1].queueItem.episodeId)
        assertEquals("Beta", queue[1].episode.title)
    }

    @Test
    fun `getQueueOnce returns empty list when queue is empty`() = runTest {
        val queue = queueDao.getQueueOnce()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `isInQueue returns true for queued episode`() = runTest {
        val episodeId = insertEpisode("ep1")
        queueDao.addToQueue(createQueueItem(episodeId = episodeId, position = 0))

        assertTrue(queueDao.isInQueue(episodeId))
    }

    @Test
    fun `isInQueue returns false for non-queued episode`() = runTest {
        val episodeId = insertEpisode("ep1")
        assertFalse(queueDao.isInQueue(episodeId))
    }

    @Test
    fun `getMaxPosition returns highest position`() = runTest {
        val ep1Id = insertEpisode("ep1")
        val ep2Id = insertEpisode("ep2")
        val ep3Id = insertEpisode("ep3")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 5))
        queueDao.addToQueue(createQueueItem(episodeId = ep3Id, position = 3))

        val maxPosition = queueDao.getMaxPosition()
        assertEquals(5, maxPosition)
    }

    @Test
    fun `getMaxPosition returns null when queue is empty`() = runTest {
        val maxPosition = queueDao.getMaxPosition()
        assertNull(maxPosition)
    }

    @Test
    fun `CASCADE deleting an episode removes its queue item`() = runTest {
        val ep1Id = insertEpisode("ep1")
        val ep2Id = insertEpisode("ep2")

        queueDao.addToQueue(createQueueItem(episodeId = ep1Id, position = 0))
        queueDao.addToQueue(createQueueItem(episodeId = ep2Id, position = 1))

        // Verify both items in queue
        assertEquals(2, queueDao.getQueueOnce().size)

        // Delete episode ep1 via deleteByPodcastId or directly
        // We need to delete the episode itself. EpisodeDao doesn't have a single delete,
        // but we can delete all episodes for the podcast and re-insert ep2.
        // Actually, the simplest way is to delete the podcast (cascade to episodes, then cascade to queue).
        // But for this test, we want to test episode -> queue cascade specifically.
        // Let's use deleteByPodcastId for ep1's podcast, but both eps belong to same podcast.
        // Instead, let's just delete all episodes and check queue is empty.

        // Alternative: create a second podcast, put ep2 there, delete first podcast's episodes
        // Simplest: verify the cascade from podcast -> episode -> queue
        val podcast = podcastDao.getByFeedUrl("https://example.com/feed.xml")!!
        podcastDao.delete(podcast)

        // All episodes deleted via cascade, and their queue items too
        val queue = queueDao.getQueueOnce()
        assertEquals(0, queue.size)
    }
}
