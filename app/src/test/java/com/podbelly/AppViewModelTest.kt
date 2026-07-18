package com.podbelly

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import app.cash.turbine.test
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val application = mockk<Application>()
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { podcastDao.getAll() } returns podcastsFlow

        // Staleness gate for the launch refresh: default to "never refreshed" with
        // a 60-minute interval, so init's refreshIfStale() fires like before.
        every { preferencesManager.feedRefreshIntervalMinutes } returns MutableStateFlow(60)
        every { preferencesManager.lastFeedRefreshAt } returns MutableStateFlow(0L)

        val packageInfo = PackageInfo().apply {
            @Suppress("DEPRECATION")
            versionCode = 9
            versionName = "1.0.8"
        }
        val packageManager = mockk<PackageManager>()
        every { packageManager.getPackageInfo("com.podbelly", 0) } returns packageInfo
        every { application.packageManager } returns packageManager
        every { application.packageName } returns "com.podbelly"

        // Default: fresh install (versionCode = 0 means first time)
        coEvery { preferencesManager.getLastSeenVersionCode() } returns 0

        // Default: no episode exists yet, so feed episodes are treated as new.
        // (A relaxed mock would otherwise fabricate a non-null "existing" episode.)
        coEvery { episodeDao.getByPodcastAndGuid(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makePodcast(
        id: Long = 1L,
        feedUrl: String = "https://example.com/feed.xml",
        title: String = "Test Podcast",
    ) = PodcastEntity(
        id = id,
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        description = "Description",
        artworkUrl = "https://example.com/art.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
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
                duration = 60000L,
                artworkUrl = null,
                fileSize = 5000L,
            )
        },
    )

    private fun createViewModel(): AppViewModel {
        return AppViewModel(
            application = application,
            episodeDao = episodeDao,
            podcastDao = podcastDao,
            searchRepository = searchRepository,
            preferencesManager = preferencesManager,
        )
    }

    @Test
    fun `init triggers refresh automatically`() = runTest {
        val podcast = makePodcast(id = 1L, feedUrl = "https://feed1.com/rss")
        podcastsFlow.value = listOf(podcast)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed()

        createViewModel()
        advanceUntilIdle()

        coVerify { searchRepository.fetchFeed("https://feed1.com/rss") }
        coVerify { episodeDao.insertAll(any()) }
    }

    @Test
    fun `init skips the refresh when the last refresh is within the interval`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        every { preferencesManager.lastFeedRefreshAt } returns
            MutableStateFlow(System.currentTimeMillis())

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.fetchFeed(any()) }
    }

    @Test
    fun `init skips the refresh when the interval is manual-only`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        every { preferencesManager.feedRefreshIntervalMinutes } returns MutableStateFlow(0)

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.fetchFeed(any()) }
    }

    @Test
    fun `manual refreshFeeds runs even when the last refresh is recent`() = runTest {
        podcastsFlow.value = listOf(makePodcast())
        every { preferencesManager.lastFeedRefreshAt } returns
            MutableStateFlow(System.currentTimeMillis())
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed()

        val viewModel = createViewModel()
        advanceUntilIdle() // init's staleness check completes without refreshing

        viewModel.refreshFeeds()
        advanceUntilIdle()

        coVerify(exactly = 1) { searchRepository.fetchFeed(any()) }
    }

    @Test
    fun `unchanged existing episode is not rewritten`() = runTest {
        val podcast = makePodcast(id = 1L)
        podcastsFlow.value = listOf(podcast)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 1)
        // Mirror exactly what makeRssFeed's episode maps to in the database.
        coEvery { episodeDao.getByPodcastAndGuid(1L, "guid-1") } returns
            com.podbelly.core.database.entity.EpisodeEntity(
                id = 10L,
                podcastId = 1L,
                guid = "guid-1",
                title = "Episode 1",
                description = "Desc",
                audioUrl = "https://audio.com/ep1.mp3",
                publicationDate = 1000L,
                durationSeconds = 60,
                artworkUrl = "",
                fileSize = 5000L,
            )

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) {
            episodeDao.updateFeedFields(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `changed existing episode is rewritten`() = runTest {
        val podcast = makePodcast(id = 1L)
        podcastsFlow.value = listOf(podcast)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 1)
        // Same episode but the publisher fixed the title.
        coEvery { episodeDao.getByPodcastAndGuid(1L, "guid-1") } returns
            com.podbelly.core.database.entity.EpisodeEntity(
                id = 10L,
                podcastId = 1L,
                guid = "guid-1",
                title = "Old Title",
                description = "Desc",
                audioUrl = "https://audio.com/ep1.mp3",
                publicationDate = 1000L,
                durationSeconds = 60,
                artworkUrl = "",
                fileSize = 5000L,
            )

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            episodeDao.updateFeedFields(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `refreshFeeds fetches all podcast feeds and inserts episodes`() = runTest {
        val podcast1 = makePodcast(id = 1L, feedUrl = "https://feed1.com/rss")
        val podcast2 = makePodcast(id = 2L, feedUrl = "https://feed2.com/rss")
        podcastsFlow.value = listOf(podcast1, podcast2)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed()

        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify { searchRepository.fetchFeed("https://feed1.com/rss") }
        coVerify { searchRepository.fetchFeed("https://feed2.com/rss") }
        coVerify(exactly = 2) { episodeDao.insertAll(any()) }
        coVerify(exactly = 2) { podcastDao.update(any()) }
    }

    @Test
    fun `refreshResult emits count of new episodes`() = runTest {
        val podcast = makePodcast(id = 1L, feedUrl = "https://feed.com/rss")
        podcastsFlow.value = listOf(podcast)

        // insertAll returns positive IDs for 2 new episodes
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 2)
        coEvery { episodeDao.insertAll(any()) } returns listOf(1L, 2L)

        val viewModel = createViewModel()

        viewModel.refreshResult.test {
            advanceUntilIdle()
            val count = awaitItem()
            assertEquals(2, count)
        }
    }

    @Test
    fun `refreshResult emits zero when no new episodes`() = runTest {
        val podcast = makePodcast(id = 1L, feedUrl = "https://feed.com/rss")
        podcastsFlow.value = listOf(podcast)

        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed(episodeCount = 1)
        // Episode already exists for this feed -> refreshed in place, not counted as new.
        coEvery { episodeDao.getByPodcastAndGuid(any(), any()) } returns
            mockk<com.podbelly.core.database.entity.EpisodeEntity>(relaxed = true)

        val viewModel = createViewModel()

        viewModel.refreshResult.test {
            advanceUntilIdle()
            val count = awaitItem()
            assertEquals(0, count)
        }
    }

    @Test
    fun `isRefreshing is false after refresh completes`() = runTest {
        podcastsFlow.value = emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.isRefreshing.test {
            val value = awaitItem()
            assertFalse(value)
        }
    }

    @Test
    fun `refreshFeeds skips when already refreshing`() = runTest {
        val podcast = makePodcast(id = 1L, feedUrl = "https://feed.com/rss")
        podcastsFlow.value = listOf(podcast)
        coEvery { searchRepository.fetchFeed(any()) } returns makeRssFeed()

        val viewModel = createViewModel()
        // Call again immediately — should be skipped since init already started a refresh
        viewModel.refreshFeeds()
        advanceUntilIdle()

        // Should only fetch once (from init), not twice
        coVerify(exactly = 1) { searchRepository.fetchFeed(any()) }
    }

    @Test
    fun `refreshFeeds handles feed fetch failure gracefully`() = runTest {
        val podcast1 = makePodcast(id = 1L, feedUrl = "https://feed1.com/rss")
        val podcast2 = makePodcast(id = 2L, feedUrl = "https://feed2.com/rss")
        podcastsFlow.value = listOf(podcast1, podcast2)

        // First feed throws, second succeeds
        coEvery { searchRepository.fetchFeed("https://feed1.com/rss") } throws RuntimeException("Network error")
        coEvery { searchRepository.fetchFeed("https://feed2.com/rss") } returns makeRssFeed()

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Second feed should still be processed
        coVerify { searchRepository.fetchFeed("https://feed2.com/rss") }
        coVerify(exactly = 1) { episodeDao.insertAll(any()) }
    }
}
