package com.podbelly.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [FeedRefreshWorker] — the background half of feed refreshing.
 * Its insert/update logic mirrors AppViewModel.refreshFeeds (tested in
 * AppViewModelTest); what's unique here is the worker lifecycle (Result
 * semantics) and [FeedRefreshWorker.cleanUpPlayedDownloads], which only exists
 * on this path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FeedRefreshWorkerTest {

    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val listeningSessionDao = mockk<ListeningSessionDao>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    @Before
    fun setUp() {
        every { podcastDao.getAll() } returns podcastsFlow
        every { preferencesManager.smartAutoDownload } returns flowOf(false)
        every { preferencesManager.smartAutoDownloadWindowDays } returns flowOf(30)
        every { preferencesManager.smartAutoDownloadKeepPerShow } returns flowOf(0)
        every { preferencesManager.autoDeletePlayedAfterDays } returns flowOf(0)
        coEvery { downloadManager.isDownloadBlockedByWifiSetting() } returns false
        coEvery { downloadManager.isAutoDownloadBlockedByChargingSetting() } returns false
        coEvery { listeningSessionDao.getEngagedPodcastIds(any()) } returns emptyList()
        coEvery { episodeDao.getByPodcastAndGuid(any(), any()) } returns null
        coEvery { episodeDao.getPlayedDownloadIdsOlderThan(any()) } returns emptyList()
    }

    private fun makePodcast(id: Long = 1L) = PodcastEntity(
        id = id,
        feedUrl = "https://example.com/feed-$id.xml",
        title = "Podcast $id",
        author = "Author",
        description = "Description",
        artworkUrl = "",
        link = "",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
        // Keeps the notification path out of these tests.
        notifyNewEpisodes = false,
    )

    private fun makeRssFeed(episodeCount: Int = 1) = RssFeed(
        title = "Feed",
        description = "Feed Desc",
        author = "Author",
        artworkUrl = "https://art.com",
        link = "https://link.com",
        episodes = (1..episodeCount).map { i ->
            RssEpisode(
                guid = "guid-$i",
                title = "Episode $i",
                description = "Desc",
                audioUrl = "https://audio.com/ep$i.mp3",
                publishedAt = 1000L * i,
                duration = 60_000L,
                artworkUrl = null,
                fileSize = 5000L,
            )
        },
    )

    private fun createWorker(): FeedRefreshWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<FeedRefreshWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = FeedRefreshWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    podcastDao = podcastDao,
                    episodeDao = episodeDao,
                    searchRepository = searchRepository,
                    preferencesManager = preferencesManager,
                    listeningSessionDao = listeningSessionDao,
                    downloadManager = downloadManager,
                )
            })
            .build() as FeedRefreshWorker
    }

    @Test
    fun `refresh inserts new episodes and records the refresh time`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 2)
        coEvery { episodeDao.insertAll(any()) } returns listOf(11L, 12L)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { episodeDao.insertAll(match { it.size == 2 }) }
        coVerify { preferencesManager.setLastFeedRefreshAt(any()) }
    }

    @Test
    fun `a failing feed is skipped without failing the refresh`() = runTest {
        podcastsFlow.value = listOf(makePodcast(id = 1L), makePodcast(id = 2L))
        coEvery { searchRepository.fetchFeed("https://example.com/feed-1.xml") } throws
            RuntimeException("boom")
        coEvery { searchRepository.fetchFeed("https://example.com/feed-2.xml") } returns
            makeRssFeed(episodeCount = 1)
        coEvery { episodeDao.insertAll(any()) } returns listOf(11L)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        // The healthy feed still counted, so the refresh time is recorded.
        coVerify { preferencesManager.setLastFeedRefreshAt(any()) }
    }

    @Test
    fun `offline refresh does not claim feeds are up to date`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        coEvery { searchRepository.fetchFeed(any()) } throws RuntimeException("offline")

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { preferencesManager.setLastFeedRefreshAt(any()) }
    }

    @Test
    fun `auto-delete removes played downloads older than the configured days`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 0)
        every { preferencesManager.autoDeletePlayedAfterDays } returns flowOf(7)
        coEvery { episodeDao.getPlayedDownloadIdsOlderThan(any()) } returns listOf(4L, 5L)

        createWorker().doWork()

        coVerify { downloadManager.deleteDownload(4L) }
        coVerify { downloadManager.deleteDownload(5L) }
    }

    @Test
    fun `auto-delete is skipped entirely when turned off`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 0)

        createWorker().doWork()

        coVerify(exactly = 0) { episodeDao.getPlayedDownloadIdsOlderThan(any()) }
        coVerify(exactly = 0) { downloadManager.deleteDownload(any()) }
    }

    @Test
    fun `smart auto-download queues new arrivals from engaged shows`() = runTest {
        podcastsFlow.value = listOf(makePodcast(id = 1L))
        every { preferencesManager.smartAutoDownload } returns flowOf(true)
        coEvery { listeningSessionDao.getEngagedPodcastIds(any()) } returns listOf(1L)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 1)
        coEvery { episodeDao.insertAll(any()) } returns listOf(11L)

        createWorker().doWork()

        coVerify {
            downloadManager.autoDownloadNewEpisodes(
                podcastId = 1L,
                inserted = match { list -> list.map { it.episodeId } == listOf(11L) },
                keepPerShow = 0,
            )
        }
    }

    @Test
    fun `per-show Never override blocks the worker's auto-download too`() = runTest {
        podcastsFlow.value = listOf(makePodcast(id = 1L).copy(autoDownloadMode = AUTO_DOWNLOAD_NEVER))
        every { preferencesManager.smartAutoDownload } returns flowOf(true)
        coEvery { listeningSessionDao.getEngagedPodcastIds(any()) } returns listOf(1L)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 1)
        coEvery { episodeDao.insertAll(any()) } returns listOf(11L)

        createWorker().doWork()

        coVerify(exactly = 0) { downloadManager.autoDownloadNewEpisodes(any(), any(), any()) }
    }

    @Test
    fun `no subscriptions short-circuits to success`() = runTest {
        podcastsFlow.value = emptyList()

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { searchRepository.fetchFeed(any()) }
    }
}
