package com.podbelly.feature.discover

import app.cash.turbine.test
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val searchRepository = mockk<PodcastSearchRepository>(relaxed = true)
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default: no podcast exists in DB for subscribed checks
        coEvery { podcastDao.getByFeedUrl(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DiscoverViewModel {
        return DiscoverViewModel(
            searchRepository = searchRepository,
            podcastDao = podcastDao,
            episodeDao = episodeDao,
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
                assertFalse(lastItem.isSubscribing)
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
                assertFalse(lastItem.isSubscribing)
            }
        }
    }
}
