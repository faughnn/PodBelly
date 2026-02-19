package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.PodcastEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
class PodcastDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var podcastDao: PodcastDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        podcastDao = database.podcastDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPodcast(
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
        author: String = "Test Author",
        description: String = "A test podcast",
        artworkUrl: String = "https://example.com/artwork.png",
        link: String = "https://example.com",
        language: String = "en",
        lastBuildDate: Long = 1000L,
        subscribed: Boolean = true,
        subscribedAt: Long = 1000L,
        lastRefreshedAt: Long = 0L,
        episodeCount: Int = 0,
    ) = PodcastEntity(
        feedUrl = feedUrl,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
        link = link,
        language = language,
        lastBuildDate = lastBuildDate,
        subscribed = subscribed,
        subscribedAt = subscribedAt,
        lastRefreshedAt = lastRefreshedAt,
        episodeCount = episodeCount,
    )

    @Test
    fun `insert returns generated id`() = runTest {
        val podcast = createPodcast()
        val id = podcastDao.insert(podcast)
        assertTrue(id > 0)
    }

    @Test
    fun `getAll returns only subscribed podcasts ordered by title`() = runTest {
        podcastDao.insert(createPodcast(feedUrl = "https://c.com/feed", title = "Charlie", subscribed = true))
        podcastDao.insert(createPodcast(feedUrl = "https://a.com/feed", title = "Alpha", subscribed = true))
        podcastDao.insert(createPodcast(feedUrl = "https://b.com/feed", title = "Bravo", subscribed = false))
        podcastDao.insert(createPodcast(feedUrl = "https://d.com/feed", title = "Delta", subscribed = true))

        podcastDao.getAll().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("Alpha", items[0].title)
            assertEquals("Charlie", items[1].title)
            assertEquals("Delta", items[2].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getById returns correct podcast`() = runTest {
        val id = podcastDao.insert(createPodcast(title = "Target"))
        podcastDao.insert(createPodcast(feedUrl = "https://other.com/feed", title = "Other"))

        podcastDao.getById(id).test {
            val podcast = awaitItem()
            assertNotNull(podcast)
            assertEquals("Target", podcast!!.title)
            assertEquals(id, podcast.id)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getById returns null for nonexistent id`() = runTest {
        podcastDao.getById(999L).test {
            val podcast = awaitItem()
            assertNull(podcast)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getByFeedUrl returns correct podcast`() = runTest {
        val feedUrl = "https://example.com/unique-feed.xml"
        podcastDao.insert(createPodcast(feedUrl = feedUrl, title = "My Podcast"))
        podcastDao.insert(createPodcast(feedUrl = "https://other.com/feed.xml", title = "Other"))

        val podcast = podcastDao.getByFeedUrl(feedUrl)
        assertNotNull(podcast)
        assertEquals("My Podcast", podcast!!.title)
        assertEquals(feedUrl, podcast.feedUrl)
    }

    @Test
    fun `getByFeedUrl returns null for nonexistent url`() = runTest {
        val podcast = podcastDao.getByFeedUrl("https://nonexistent.com/feed.xml")
        assertNull(podcast)
    }

    @Test
    fun `update modifies existing podcast`() = runTest {
        val id = podcastDao.insert(createPodcast(title = "Original Title"))

        val original = podcastDao.getByFeedUrl("https://example.com/feed.xml")!!
        val updated = original.copy(title = "Updated Title", episodeCount = 42)
        podcastDao.update(updated)

        podcastDao.getById(id).test {
            val podcast = awaitItem()
            assertNotNull(podcast)
            assertEquals("Updated Title", podcast!!.title)
            assertEquals(42, podcast.episodeCount)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `delete removes podcast`() = runTest {
        val id = podcastDao.insert(createPodcast())

        val podcast = podcastDao.getByFeedUrl("https://example.com/feed.xml")!!
        podcastDao.delete(podcast)

        podcastDao.getById(id).test {
            val result = awaitItem()
            assertNull(result)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `unsubscribe sets subscribed to false and podcast disappears from getAll`() = runTest {
        val id = podcastDao.insert(createPodcast(title = "Will Unsubscribe"))

        podcastDao.unsubscribe(id)

        // Should not appear in getAll (which only returns subscribed)
        podcastDao.getAll().test {
            val items = awaitItem()
            assertTrue(items.none { it.id == id })
            cancelAndConsumeRemainingEvents()
        }

        // But should still exist in the database with subscribed=false
        podcastDao.getById(id).test {
            val podcast = awaitItem()
            assertNotNull(podcast)
            assertEquals(false, podcast!!.subscribed)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `insert with same feedUrl replaces existing due to REPLACE strategy`() = runTest {
        val feedUrl = "https://example.com/feed.xml"
        val id1 = podcastDao.insert(createPodcast(feedUrl = feedUrl, title = "Original", episodeCount = 5))
        val id2 = podcastDao.insert(createPodcast(feedUrl = feedUrl, title = "Replacement", episodeCount = 10))

        // REPLACE should have removed the old row and inserted a new one
        val podcast = podcastDao.getByFeedUrl(feedUrl)
        assertNotNull(podcast)
        assertEquals("Replacement", podcast!!.title)
        assertEquals(10, podcast.episodeCount)

        // With REPLACE on a unique index, the old row is deleted and a new one inserted
        // so the id may differ (autoGenerate gives a new id)
        assertNotEquals(0L, id2)
    }
}
