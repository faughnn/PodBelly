package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.DownloadErrorEntity
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DownloadErrorDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var downloadErrorDao: DownloadErrorDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao

    private var podcastId = 0L
    private var episodeId1 = 0L
    private var episodeId2 = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        downloadErrorDao = database.downloadErrorDao()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()

        podcastId = podcastDao.insert(
            PodcastEntity(
                feedUrl = "https://example.com/feed.xml", title = "Test Podcast",
                author = "Author", description = "Desc", artworkUrl = "",
                link = "", language = "en", lastBuildDate = 0L, subscribedAt = 1000L,
            )
        )

        val ids = episodeDao.insertAll(listOf(
            EpisodeEntity(
                podcastId = podcastId, guid = "ep1", title = "Episode One",
                description = "Desc", audioUrl = "https://example.com/ep1.mp3",
                publicationDate = 1700000000000L,
            ),
            EpisodeEntity(
                podcastId = podcastId, guid = "ep2", title = "Episode Two",
                description = "Desc", audioUrl = "https://example.com/ep2.mp3",
                publicationDate = 1700000100000L,
            ),
        ))
        episodeId1 = ids[0]
        episodeId2 = ids[1]
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert adds error record`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(
                episodeId = episodeId1,
                errorMessage = "HTTP 500",
                errorCode = 500,
                timestamp = 1000L,
            )
        )

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertEquals(1, errors.size)
            assertEquals("HTTP 500", errors[0].errorMessage)
            assertEquals(500, errors[0].errorCode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getAll returns errors with episode titles joined`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error 1", timestamp = 2000L)
        )
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId2, errorMessage = "Error 2", timestamp = 1000L)
        )

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertEquals(2, errors.size)
            // Ordered by timestamp DESC
            assertEquals("Episode One", errors[0].episodeTitle)
            assertEquals("Error 1", errors[0].errorMessage)
            assertEquals("Episode Two", errors[1].episodeTitle)
            assertEquals("Error 2", errors[1].errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteByEpisodeId removes errors for specific episode`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error 1", timestamp = 1000L)
        )
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId2, errorMessage = "Error 2", timestamp = 2000L)
        )

        downloadErrorDao.deleteByEpisodeId(episodeId1)

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertEquals(1, errors.size)
            assertEquals(episodeId2, errors[0].episodeId)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteAll removes all error records`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error 1", timestamp = 1000L)
        )
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId2, errorMessage = "Error 2", timestamp = 2000L)
        )

        downloadErrorDao.deleteAll()

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertTrue(errors.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `incrementRetryCount increases retry count by one`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error", timestamp = 1000L, retryCount = 0)
        )

        downloadErrorDao.incrementRetryCount(episodeId1)

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertEquals(1, errors[0].retryCount)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `incrementRetryCount twice increases to two`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error", timestamp = 1000L, retryCount = 0)
        )

        downloadErrorDao.incrementRetryCount(episodeId1)
        downloadErrorDao.incrementRetryCount(episodeId1)

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertEquals(2, errors[0].retryCount)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `CASCADE deleting an episode removes its download errors`() = runTest {
        downloadErrorDao.insert(
            DownloadErrorEntity(episodeId = episodeId1, errorMessage = "Error", timestamp = 1000L)
        )

        // Delete the parent podcast (cascades to episodes, then to download errors)
        val podcast = podcastDao.getByFeedUrl("https://example.com/feed.xml")!!
        podcastDao.delete(podcast)

        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertTrue(errors.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getAll returns empty list when no errors exist`() = runTest {
        downloadErrorDao.getAll().test {
            val errors = awaitItem()
            assertTrue(errors.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }
}
