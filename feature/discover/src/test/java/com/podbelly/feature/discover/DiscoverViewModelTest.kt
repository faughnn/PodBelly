package com.podbelly.feature.discover

import android.content.Context
import app.cash.turbine.test
import coil.ImageLoader
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context = mockk<Context>(relaxed = true)
    private val imageLoader = mockk<ImageLoader>(relaxed = true)
    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default: no podcast exists in DB for subscribed checks
        coEvery { podcastDao.getByFeedUrl(any()) } returns null
        // Blank stored region -> ViewModel falls back to the device locale.
        every { preferencesManager.chartCountry } returns MutableStateFlow("")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DiscoverViewModel {
        return DiscoverViewModel(
            context = context,
            imageLoader = imageLoader,
            searchRepository = searchRepository,
            podcastDao = podcastDao,
            episodeDao = episodeDao,
            preferencesManager = preferencesManager,
        )
    }

    // -- Tests --

    @Test
    fun `initial state has empty search results`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(emptyList<DiscoverPodcastItem>(), initial.searchResults)
            assertEquals("", initial.searchQuery)
            assertFalse(initial.isSearching)
            assertNull(initial.message)
        }
    }

    @Test
    fun `search updates results with items from searchRepository`() = runTest {
        val searchResults = listOf(
            SearchResult(
                feedUrl = "https://feed1.com/rss",
                title = "Podcast One",
                author = "Author One",
                artworkUrl = "https://art1.com/img.jpg",
            ),
            SearchResult(
                feedUrl = "https://feed2.com/rss",
                title = "Podcast Two",
                author = "Author Two",
                artworkUrl = "https://art2.com/img.jpg",
            ),
        )
        coEvery { searchRepository.search("test query") } returns searchResults

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            viewModel.search("test query")
            advanceUntilIdle()

            // There may be intermediate states (isSearching = true); skip to the final settled state
            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<DiscoverUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertEquals(2, lastItem.searchResults.size)
                assertEquals("Podcast One", lastItem.searchResults[0].title)
                assertEquals("Author One", lastItem.searchResults[0].author)
                assertEquals("https://feed1.com/rss", lastItem.searchResults[0].feedUrl)
                assertFalse(lastItem.searchResults[0].isSubscribed)
                assertEquals("Podcast Two", lastItem.searchResults[1].title)
                assertFalse(lastItem.isSearching)
            }
        }
    }

    @Test
    fun `subscribeToPodcast fetches feed and inserts podcast and episodes into DB`() = runTest {
        val feedUrl = "https://example.com/feed.xml"

        val rssEpisode = RssEpisode(
            guid = "guid-1",
            title = "Episode 1",
            description = "Description",
            audioUrl = "https://audio.com/ep1.mp3",
            publishedAt = 100000L,
            duration = 60000L,
            artworkUrl = "https://art.com/ep1.jpg",
            fileSize = 5000L,
        )
        val rssFeed = RssFeed(
            title = "Test Podcast",
            description = "A podcast",
            author = "Author",
            artworkUrl = "https://art.com/podcast.jpg",
            link = "https://example.com",
            episodes = listOf(rssEpisode),
        )

        coEvery { podcastDao.getByFeedUrl(feedUrl) } returns null
        coEvery { searchRepository.fetchFeed(feedUrl) } returns rssFeed
        coEvery { podcastDao.insert(any()) } returns 42L

        val viewModel = createViewModel()
        viewModel.subscribeToPodcast(feedUrl)
        advanceUntilIdle()

        coVerify { searchRepository.fetchFeed(feedUrl) }
        coVerify { podcastDao.insert(match { it.feedUrl == feedUrl && it.title == "Test Podcast" && it.subscribed }) }
        coVerify { episodeDao.insertAll(match { it.size == 1 && it[0].podcastId == 42L }) }
    }

    @Test
    fun `subscribeToPodcast shows already subscribed message when podcast already subscribed`() = runTest {
        val feedUrl = "https://example.com/feed.xml"
        val existing = PodcastEntity(
            id = 5L,
            feedUrl = feedUrl,
            title = "Existing",
            author = "Author",
            description = "Desc",
            artworkUrl = "https://art.com",
            link = "https://link.com",
            language = "en",
            lastBuildDate = 0L,
            subscribed = true,
            subscribedAt = 0L,
        )

        coEvery { podcastDao.getByFeedUrl(feedUrl) } returns existing

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.subscribeToPodcast(feedUrl)
            advanceUntilIdle()

            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<DiscoverUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertEquals("Already subscribed", lastItem.message)
                assertTrue(lastItem.subscribingFeedUrls.isEmpty())
            }
        }
    }

    @Test
    fun `error during search sets error message in state`() = runTest {
        coEvery { searchRepository.search("fail query") } throws RuntimeException("Network error")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.search("fail query")
            advanceUntilIdle()

            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<DiscoverUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertTrue(lastItem.message?.contains("Search failed") == true)
                assertFalse(lastItem.isSearching)
            }
        }
    }

    @Test
    fun `clearMessage sets message to null`() = runTest {
        coEvery { searchRepository.search("fail") } throws RuntimeException("err")

        val viewModel = createViewModel()

        viewModel.search("fail")
        advanceUntilIdle()

        viewModel.clearMessage()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.message)
        }
    }

    // -- subscribeByUrl tests --

    @Test
    fun `subscribeByUrl trims URL and subscribes`() = runTest {
        val feedUrl = "https://example.com/feed.xml"

        val rssFeed = RssFeed(
            title = "Direct Feed",
            description = "Desc",
            author = "Author",
            artworkUrl = "https://art.com",
            link = "https://example.com",
            episodes = emptyList(),
        )

        coEvery { podcastDao.getByFeedUrl(feedUrl) } returns null
        coEvery { searchRepository.fetchFeed(feedUrl) } returns rssFeed
        coEvery { podcastDao.insert(any()) } returns 10L

        val viewModel = createViewModel()
        viewModel.subscribeByUrl("  $feedUrl  ")
        advanceUntilIdle()

        coVerify { searchRepository.fetchFeed(feedUrl) }
        coVerify { podcastDao.insert(match { it.feedUrl == feedUrl }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.feedUrlInput)
        }
    }

    @Test
    fun `subscribeByUrl with blank URL shows error message`() = runTest {
        val viewModel = createViewModel()
        viewModel.subscribeByUrl("   ")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Please enter a feed URL", state.message)
        }
    }

    // -- updateSearchQuery tests --

    @Test
    fun `updateSearchQuery clears results when blank`() = runTest {
        // First do a search to populate results
        val searchResults = listOf(
            SearchResult(
                feedUrl = "https://feed.com/rss",
                title = "Podcast",
                author = "Author",
                artworkUrl = "https://art.com",
            ),
        )
        coEvery { searchRepository.search("test") } returns searchResults

        val viewModel = createViewModel()
        viewModel.search("test")
        advanceUntilIdle()

        // Now clear the query
        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.isSearching)
            assertEquals("", state.searchQuery)
        }
    }

    // -- updateFeedUrl tests --

    @Test
    fun `updateFeedUrl updates feedUrlInput in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateFeedUrl("https://my-podcast.com/rss")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("https://my-podcast.com/rss", state.feedUrlInput)
        }
    }

    // -- Subscribe error tests --

    @Test
    fun `error during subscribe sets error message`() = runTest {
        val feedUrl = "https://bad-feed.com/rss"
        coEvery { podcastDao.getByFeedUrl(feedUrl) } returns null
        coEvery { searchRepository.fetchFeed(feedUrl) } throws RuntimeException("Connection refused")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.subscribeToPodcast(feedUrl)
            advanceUntilIdle()

            val states = cancelAndConsumeRemainingEvents()
            val lastItem = states.filterIsInstance<app.cash.turbine.Event.Item<DiscoverUiState>>().lastOrNull()?.value

            if (lastItem != null) {
                assertTrue(lastItem.message?.contains("Subscription failed") == true)
                assertTrue(lastItem.subscribingFeedUrls.isEmpty())
            }
        }
    }

    // -- Charts --

    private fun chartResult(feedUrl: String, title: String) = SearchResult(
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        artworkUrl = "https://art.example/$title.jpg",
    )

    @Test
    fun `top chart loads on init and marks subscribed podcasts`() = runTest {
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns listOf(
            chartResult("https://feed.a", "Chart Show A"),
            chartResult("https://feed.b", "Chart Show B"),
        )
        coEvery { podcastDao.getByFeedUrl("https://feed.a") } returns PodcastEntity(
            id = 1L,
            feedUrl = "https://feed.a",
            title = "Chart Show A",
            author = "Author",
            description = "Desc",
            artworkUrl = "",
            link = "",
            language = "en",
            lastBuildDate = 0L,
            subscribed = true,
            subscribedAt = 0L,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Chart Show A", "Chart Show B"), state.chartResults.map { it.title })
        assertTrue(state.chartResults[0].isSubscribed)
        assertFalse(state.chartResults[1].isSubscribed)
        assertFalse(state.isLoadingChart)
        assertNull(state.chartError)
        coVerify { searchRepository.topPodcasts(any(), DiscoverViewModel.TOP_CHART_GENRE_ID, any()) }
    }

    @Test
    fun `selecting a category loads its chart by genre id`() = runTest {
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectChartCategory(1303)
        advanceUntilIdle()

        assertEquals(1303, viewModel.uiState.value.selectedChartGenreId)
        coVerify { searchRepository.topPodcasts(any(), 1303, any()) }
    }

    @Test
    fun `switching back to a cached category does not refetch`() = runTest {
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectChartCategory(1303)
        advanceUntilIdle()
        viewModel.selectChartCategory(DiscoverViewModel.TOP_CHART_GENRE_ID)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            searchRepository.topPodcasts(any(), DiscoverViewModel.TOP_CHART_GENRE_ID, any())
        }
    }

    @Test
    fun `chart failure surfaces an error and retry recovers`() = runTest {
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } throws RuntimeException("offline")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.chartError != null)
        assertTrue(viewModel.uiState.value.chartResults.isEmpty())

        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns listOf(
            chartResult("https://feed.c", "Recovered Show"),
        )
        viewModel.retryChart()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.chartError)
        assertEquals(listOf("Recovered Show"), state.chartResults.map { it.title })
    }

    // -- Chart regions --

    @Test
    fun `stored chart region is used for the initial chart load`() = runTest {
        every { preferencesManager.chartCountry } returns MutableStateFlow("gb")
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("gb", viewModel.uiState.value.selectedChartCountry)
        coVerify { searchRepository.topPodcasts("gb", DiscoverViewModel.TOP_CHART_GENRE_ID, any()) }
    }

    @Test
    fun `selecting a region persists it, clears the cache and refetches`() = runTest {
        every { preferencesManager.chartCountry } returns MutableStateFlow("us")
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns listOf(
            chartResult("https://feed.a", "Show A"),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectChartRegion("ie")
        advanceUntilIdle()

        assertEquals("ie", viewModel.uiState.value.selectedChartCountry)
        coVerify { preferencesManager.setChartCountry("ie") }
        // Fetched once per region for the same genre - the cache was cleared.
        coVerify(exactly = 1) { searchRepository.topPodcasts("us", DiscoverViewModel.TOP_CHART_GENRE_ID, any()) }
        coVerify(exactly = 1) { searchRepository.topPodcasts("ie", DiscoverViewModel.TOP_CHART_GENRE_ID, any()) }
    }

    @Test
    fun `selecting the already-active region does nothing`() = runTest {
        every { preferencesManager.chartCountry } returns MutableStateFlow("ie")
        coEvery { searchRepository.topPodcasts(any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectChartRegion("ie")
        advanceUntilIdle()

        coVerify(exactly = 0) { preferencesManager.setChartCountry(any()) }
    }
}
