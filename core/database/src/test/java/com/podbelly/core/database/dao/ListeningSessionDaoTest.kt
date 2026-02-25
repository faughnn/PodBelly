package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.ListeningSessionEntity
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
class ListeningSessionDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var listeningSessionDao: ListeningSessionDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao

    private var podcastId1 = 0L
    private var podcastId2 = 0L
    private var episodeId1 = 0L
    private var episodeId2 = 0L
    private var episodeId3 = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listeningSessionDao = database.listeningSessionDao()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()

        // Seed test data
        podcastId1 = podcastDao.insert(createPodcast(feedUrl = "https://p1.com/feed", title = "Podcast One"))
        podcastId2 = podcastDao.insert(createPodcast(feedUrl = "https://p2.com/feed", title = "Podcast Two"))

        val eps = episodeDao.insertAll(listOf(
            createEpisode(podcastId = podcastId1, guid = "ep1", title = "Episode 1"),
            createEpisode(podcastId = podcastId1, guid = "ep2", title = "Episode 2"),
            createEpisode(podcastId = podcastId2, guid = "ep3", title = "Episode 3"),
        ))
        episodeId1 = eps[0]
        episodeId2 = eps[1]
        episodeId3 = eps[2]
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPodcast(
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
    ) = PodcastEntity(
        feedUrl = feedUrl, title = title, author = "Author",
        description = "Desc", artworkUrl = "", link = "",
        language = "en", lastBuildDate = 0L, subscribedAt = 1000L,
    )

    private fun createEpisode(
        podcastId: Long,
        guid: String,
        title: String = "Episode",
    ) = EpisodeEntity(
        podcastId = podcastId, guid = guid, title = title,
        description = "Desc", audioUrl = "https://example.com/$guid.mp3",
        publicationDate = 1700000000000L,
    )

    @Test
    fun `insert returns generated id`() = runTest {
        val session = ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = 1000L, listenedMs = 60000L, playbackSpeed = 1.0f,
        )
        val id = listeningSessionDao.insert(session)
        assertTrue(id > 0)
    }

    @Test
    fun `updateSession sets endedAt and listenedMs`() = runTest {
        val id = listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L,
            )
        )

        listeningSessionDao.updateSession(id, endedAt = 2000L, listenedMs = 60000L)

        // Verify via total listened
        listeningSessionDao.getTotalListenedMs().test {
            assertEquals(60000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTotalListenedMs sums all sessions`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId2, podcastId = podcastId1,
                startedAt = 2000L, listenedMs = 30000L,
            )
        )

        listeningSessionDao.getTotalListenedMs().test {
            assertEquals(90000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTotalListenedMs returns zero when no sessions`() = runTest {
        listeningSessionDao.getTotalListenedMs().test {
            assertEquals(0L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTimeSavedBySpeed calculates savings for speeds above 1x`() = runTest {
        // At 2.0x for 60,000ms listened, time saved = 60000 * (2.0 - 1.0) / 2.0 = 30,000ms
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L, playbackSpeed = 2.0f,
            )
        )

        listeningSessionDao.getTimeSavedBySpeed().test {
            assertEquals(30000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTimeSavedBySpeed returns zero for 1x speed`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L, playbackSpeed = 1.0f,
            )
        )

        listeningSessionDao.getTimeSavedBySpeed().test {
            assertEquals(0L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTimeSavedBySpeed returns zero for speeds below 1x`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L, playbackSpeed = 0.5f,
            )
        )

        listeningSessionDao.getTimeSavedBySpeed().test {
            assertEquals(0L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTotalSilenceTrimmedMs sums all sessions`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L, silenceTrimmedMs = 5000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId2, podcastId = podcastId1,
                startedAt = 2000L, listenedMs = 30000L, silenceTrimmedMs = 3000L,
            )
        )

        listeningSessionDao.getTotalSilenceTrimmedMs().test {
            assertEquals(8000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getMostListenedPodcasts groups by podcast and orders by total`() = runTest {
        // Podcast 1: two sessions, 60k + 30k = 90k total
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId2, podcastId = podcastId1,
                startedAt = 2000L, listenedMs = 30000L,
            )
        )
        // Podcast 2: one session, 120k total
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId3, podcastId = podcastId2,
                startedAt = 3000L, listenedMs = 120000L,
            )
        )

        listeningSessionDao.getMostListenedPodcasts(10).test {
            val stats = awaitItem()
            assertEquals(2, stats.size)
            // Podcast Two should be first (120k > 90k)
            assertEquals("Podcast Two", stats[0].podcastTitle)
            assertEquals(120000L, stats[0].totalListenedMs)
            assertEquals("Podcast One", stats[1].podcastTitle)
            assertEquals(90000L, stats[1].totalListenedMs)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getMostListenedPodcasts respects limit`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId1, podcastId = podcastId1, startedAt = 1000L, listenedMs = 60000L)
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId3, podcastId = podcastId2, startedAt = 2000L, listenedMs = 120000L)
        )

        listeningSessionDao.getMostListenedPodcasts(1).test {
            val stats = awaitItem()
            assertEquals(1, stats.size)
            assertEquals("Podcast Two", stats[0].podcastTitle)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getMostListenedEpisodes groups by episode and orders by total`() = runTest {
        // Episode 1: two sessions = 100k
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId1, podcastId = podcastId1, startedAt = 1000L, listenedMs = 60000L)
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId1, podcastId = podcastId1, startedAt = 2000L, listenedMs = 40000L)
        )
        // Episode 3: one session = 200k
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId3, podcastId = podcastId2, startedAt = 3000L, listenedMs = 200000L)
        )

        listeningSessionDao.getMostListenedEpisodes(10).test {
            val stats = awaitItem()
            assertEquals(2, stats.size)
            assertEquals("Episode 3", stats[0].episodeTitle)
            assertEquals(200000L, stats[0].totalListenedMs)
            assertEquals("Episode 1", stats[1].episodeTitle)
            assertEquals(100000L, stats[1].totalListenedMs)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getMostDownloadedPodcasts counts downloaded episodes per podcast`() = runTest {
        // Mark episodes as downloaded
        episodeDao.setDownloadPath(episodeId1, "/path/ep1.mp3", 1000L, System.currentTimeMillis())
        episodeDao.setDownloadPath(episodeId2, "/path/ep2.mp3", 2000L, System.currentTimeMillis())
        episodeDao.setDownloadPath(episodeId3, "/path/ep3.mp3", 3000L, System.currentTimeMillis())

        listeningSessionDao.getMostDownloadedPodcasts(10).test {
            val stats = awaitItem()
            assertEquals(2, stats.size)
            // Podcast One has 2 downloaded episodes, Podcast Two has 1
            assertEquals("Podcast One", stats[0].podcastTitle)
            assertEquals(2L, stats[0].downloadCount)
            assertEquals("Podcast Two", stats[1].podcastTitle)
            assertEquals(1L, stats[1].downloadCount)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getListenedMsSince returns total for sessions after threshold`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId2, podcastId = podcastId1,
                startedAt = 5000L, listenedMs = 30000L,
            )
        )

        listeningSessionDao.getListenedMsSince(3000L).test {
            assertEquals(30000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getListenedMsSince returns zero when no sessions after threshold`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = 1000L, listenedMs = 60000L,
            )
        )

        listeningSessionDao.getListenedMsSince(5000L).test {
            assertEquals(0L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getListeningDays returns distinct epoch days with activity`() = runTest {
        val day1Start = 86400000L * 100  // day 100
        val day2Start = 86400000L * 102  // day 102
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId1, podcastId = podcastId1,
                startedAt = day1Start, listenedMs = 60000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId2, podcastId = podcastId1,
                startedAt = day1Start + 3600000L, listenedMs = 30000L,
            )
        )
        listeningSessionDao.insert(
            ListeningSessionEntity(
                episodeId = episodeId3, podcastId = podcastId2,
                startedAt = day2Start, listenedMs = 45000L,
            )
        )

        listeningSessionDao.getListeningDays().test {
            val days = awaitItem()
            assertEquals(2, days.size)
            assertEquals(100L, days[0])
            assertEquals(102L, days[1])
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getListeningDays returns empty when no sessions`() = runTest {
        listeningSessionDao.getListeningDays().test {
            assertEquals(emptyList<Long>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `CASCADE deleting a podcast removes its listening sessions`() = runTest {
        listeningSessionDao.insert(
            ListeningSessionEntity(episodeId = episodeId1, podcastId = podcastId1, startedAt = 1000L, listenedMs = 60000L)
        )

        val podcast = podcastDao.getByFeedUrl("https://p1.com/feed")!!
        podcastDao.delete(podcast)

        listeningSessionDao.getTotalListenedMs().test {
            assertEquals(0L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
