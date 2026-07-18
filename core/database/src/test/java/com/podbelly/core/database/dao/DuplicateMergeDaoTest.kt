package com.podbelly.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.ListeningSessionEntity
import com.podbelly.core.database.entity.PodcastEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DuplicateMergeDaoTest {

    private lateinit var database: PodbellDatabase
    private lateinit var mergeDao: DuplicateMergeDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var sessionDao: ListeningSessionDao

    private var keepId = 0L
    private var spareId = 0L
    private var keepEp = 0L
    private var spareEp = 0L
    private var spareOnlyEp = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodbellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mergeDao = database.duplicateMergeDao()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()
        sessionDao = database.listeningSessionDao()

        keepId = podcastDao.insert(podcast("https://keep.com/feed", "Same Show"))
        spareId = podcastDao.insert(podcast("https://spare.com/feed", "Same Show"))

        val eps = episodeDao.insertAll(
            listOf(
                // Same episode on both feeds — titles differ only in punctuation.
                episode(keepId, "k1", "Episode 1: The Start"),
                episode(spareId, "s1", "Episode 1 - The Start").copy(
                    played = true,
                    playbackPosition = 90_000L,
                    lastPlayedAt = 5_000L,
                    downloadPath = "/files/s1.mp3",
                    downloadedAt = 4_000L,
                    fileSize = 1_234L,
                ),
                // Spare-only episode; no counterpart on the kept feed.
                episode(spareId, "s2", "Bonus Episode"),
            )
        )
        keepEp = eps[0]
        spareEp = eps[1]
        spareOnlyEp = eps[2]
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun podcast(feedUrl: String, title: String) = PodcastEntity(
        feedUrl = feedUrl, title = title, author = "A",
        description = "D", artworkUrl = "", link = "",
        language = "en", lastBuildDate = 0L, subscribedAt = 1000L,
    )

    private fun episode(podcastId: Long, guid: String, title: String) = EpisodeEntity(
        podcastId = podcastId, guid = guid, title = title,
        description = "D", audioUrl = "https://x.com/$guid.mp3",
        publicationDate = 1000L,
    )

    @Test
    fun `normalizeTitleForMatch ignores case punctuation and whitespace`() {
        assertEquals(
            normalizeTitleForMatch("The Rest Is Politics: US"),
            normalizeTitleForMatch("the rest is  politics — us"),
        )
        assertTrue(
            normalizeTitleForMatch("Show A") != normalizeTitleForMatch("Show B"),
        )
    }

    @Test
    fun `merge moves play state and download to the kept episode`() = runTest {
        mergeDao.merge(fromPodcastId = spareId, toPodcastId = keepId)

        val kept = episodeDao.getByIdOnce(keepEp)!!
        assertTrue(kept.played)
        assertEquals(90_000L, kept.playbackPosition)
        assertEquals(5_000L, kept.lastPlayedAt)
        assertEquals("/files/s1.mp3", kept.downloadPath)
        assertEquals(1_234L, kept.fileSize)

        // The spare episode released its download so storage isn't double-counted.
        val spare = episodeDao.getByIdOnce(spareEp)!!
        assertEquals("", spare.downloadPath)
        assertEquals(0L, spare.fileSize)
    }

    @Test
    fun `merge does not regress the kept episode's own progress`() = runTest {
        episodeDao.updatePlaybackPosition(keepEp, 200_000L)

        mergeDao.merge(fromPodcastId = spareId, toPodcastId = keepId)

        val kept = episodeDao.getByIdOnce(keepEp)!!
        assertEquals(200_000L, kept.playbackPosition)
    }

    @Test
    fun `merge reassigns listening sessions to the kept copy`() = runTest {
        sessionDao.insert(
            ListeningSessionEntity(
                episodeId = spareEp, podcastId = spareId,
                startedAt = 1000L, listenedMs = 60_000L,
            )
        )
        sessionDao.insert(
            ListeningSessionEntity(
                episodeId = spareOnlyEp, podcastId = spareId,
                startedAt = 2000L, listenedMs = 30_000L,
            )
        )

        mergeDao.merge(fromPodcastId = spareId, toPodcastId = keepId)

        // All the spare's listening now counts toward the kept show.
        sessionDao.getMostListenedPodcasts(5).test {
            val stats = awaitItem()
            assertEquals(listOf(keepId), stats.map { it.podcastId })
            assertEquals(90_000L, stats[0].totalListenedMs)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `merge unsubscribes the spare and keeps the kept copy`() = runTest {
        mergeDao.merge(fromPodcastId = spareId, toPodcastId = keepId)

        val subscribed = podcastDao.getAll().first()
        assertEquals(listOf(keepId), subscribed.map { it.id })
        assertFalse(subscribed.isEmpty())
    }

    @Test
    fun `merge with the same id is a no-op`() = runTest {
        mergeDao.merge(fromPodcastId = keepId, toPodcastId = keepId)

        assertEquals(2, podcastDao.getAll().first().size)
    }
}
