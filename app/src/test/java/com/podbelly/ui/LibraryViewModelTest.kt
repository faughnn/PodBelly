package com.podbelly.ui

import app.cash.turbine.test
import com.podbelly.core.common.LibrarySortOrder
import com.podbelly.core.common.LibraryViewMode
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.PodcastLatestEpisode
import com.podbelly.core.database.dao.PodcastListeningStat
import com.podbelly.core.database.entity.PodcastEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val listeningSessionDao = mockk<ListeningSessionDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())
    private val sortOrderFlow = MutableStateFlow(LibrarySortOrder.NAME_A_TO_Z)
    private val viewModeFlow = MutableStateFlow(LibraryViewMode.GRID)
    private val latestEpisodesFlow = MutableStateFlow<List<PodcastLatestEpisode>>(emptyList())
    private val listenedStatsFlow = MutableStateFlow<List<PodcastListeningStat>>(emptyList())

    private fun makePodcast(
        id: Long = 1L,
        feedUrl: String = "https://example.com/feed$id.xml",
        title: String = "Podcast $id",
        subscribedAt: Long = 1000L * id,
        episodeCount: Int = 10,
    ) = PodcastEntity(
        id = id,
        feedUrl = feedUrl,
        title = title,
        author = "Author",
        description = "Desc",
        artworkUrl = "https://example.com/art$id.jpg",
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = subscribedAt,
        episodeCount = episodeCount,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { podcastDao.getAll() } returns podcastsFlow
        every { preferencesManager.librarySortOrder } returns sortOrderFlow
        every { preferencesManager.libraryViewMode } returns viewModeFlow
        every { episodeDao.getLatestEpisodeDateByPodcast() } returns latestEpisodesFlow
        every { listeningSessionDao.getMostListenedPodcasts(limit = Int.MAX_VALUE) } returns listenedStatsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            podcastDao = podcastDao,
            episodeDao = episodeDao,
            listeningSessionDao = listeningSessionDao,
            preferencesManager = preferencesManager,
        )
    }

    // -- UI State tests --

    @Test
    fun `initial state has empty podcasts and default sort and view mode`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.podcasts.isEmpty())
            assertEquals(LibrarySortOrder.NAME_A_TO_Z, state.sortOrder)
            assertEquals(LibraryViewMode.GRID, state.viewMode)
        }
    }

    @Test
    fun `emitting podcasts populates UI state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Alpha"),
                makePodcast(id = 2L, title = "Bravo"),
            )

            val state = awaitItem()
            assertEquals(2, state.podcasts.size)
            assertEquals("Alpha", state.podcasts[0].title)
            assertEquals("Bravo", state.podcasts[1].title)
        }
    }

    // -- Sort order tests --

    @Test
    fun `NAME_A_TO_Z sorts podcasts alphabetically`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Zebra"),
                makePodcast(id = 2L, title = "Apple"),
            )

            val state = awaitItem()
            assertEquals("Apple", state.podcasts[0].title)
            assertEquals("Zebra", state.podcasts[1].title)
        }
    }

    @Test
    fun `RECENTLY_ADDED sorts by subscribedAt descending`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = LibrarySortOrder.RECENTLY_ADDED
            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Old", subscribedAt = 1000L),
                makePodcast(id = 2L, title = "New", subscribedAt = 2000L),
            )

            val state = awaitItem()
            assertEquals("New", state.podcasts[0].title)
            assertEquals("Old", state.podcasts[1].title)
        }
    }

    @Test
    fun `EPISODE_COUNT sorts by episode count descending`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = LibrarySortOrder.EPISODE_COUNT
            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Few", episodeCount = 5),
                makePodcast(id = 2L, title = "Many", episodeCount = 100),
            )

            val state = awaitItem()
            assertEquals("Many", state.podcasts[0].title)
            assertEquals("Few", state.podcasts[1].title)
        }
    }

    @Test
    fun `MOST_RECENT_EPISODE sorts by latest episode date descending`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = LibrarySortOrder.MOST_RECENT_EPISODE
            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Stale"),
                makePodcast(id = 2L, title = "Fresh"),
            )
            latestEpisodesFlow.value = listOf(
                PodcastLatestEpisode(podcastId = 1L, latestPublicationDate = 1000L),
                PodcastLatestEpisode(podcastId = 2L, latestPublicationDate = 2000L),
            )

            val state = awaitItem()
            assertEquals("Fresh", state.podcasts[0].title)
            assertEquals("Stale", state.podcasts[1].title)
        }
    }

    @Test
    fun `MOST_LISTENED sorts by total listened time descending`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sortOrderFlow.value = LibrarySortOrder.MOST_LISTENED
            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Rarely Listened"),
                makePodcast(id = 2L, title = "Most Listened"),
            )
            listenedStatsFlow.value = listOf(
                PodcastListeningStat(podcastId = 1L, podcastTitle = "", artworkUrl = "", totalListenedMs = 1000L, episodeCount = 1L),
                PodcastListeningStat(podcastId = 2L, podcastTitle = "", artworkUrl = "", totalListenedMs = 50000L, episodeCount = 3L),
            )

            val state = awaitItem()
            assertEquals("Most Listened", state.podcasts[0].title)
            assertEquals("Rarely Listened", state.podcasts[1].title)
        }
    }

    // -- View mode tests --

    @Test
    fun `view mode reflects preferences`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModeFlow.value = LibraryViewMode.LIST

            val state = awaitItem()
            assertEquals(LibraryViewMode.LIST, state.viewMode)
        }
    }

    // -- Search tests --

    @Test
    fun `search filters podcasts by title`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Tech Talk"),
                makePodcast(id = 2L, title = "History Hour"),
                makePodcast(id = 3L, title = "Tech News Daily"),
            )
            awaitItem() // podcasts loaded

            viewModel.setSearchActive(true)
            awaitItem() // search active

            viewModel.setSearchQuery("tech")
            val state = awaitItem()
            assertEquals(2, state.podcasts.size)
            assertTrue(state.podcasts.all { it.title.lowercase().contains("tech") })
            assertEquals("tech", state.searchQuery)
            assertTrue(state.isSearchActive)
        }
    }

    @Test
    fun `search filters podcasts by author`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Show A"),
                makePodcast(id = 2L, title = "Show B"),
            )
            awaitItem()

            viewModel.setSearchQuery("Author")
            val state = awaitItem()
            assertEquals(2, state.podcasts.size)
        }
    }

    @Test
    fun `empty search query shows all podcasts`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Alpha"),
                makePodcast(id = 2L, title = "Bravo"),
            )
            awaitItem()

            viewModel.setSearchQuery("Alpha")
            awaitItem()

            viewModel.setSearchQuery("")
            val state = awaitItem()
            assertEquals(2, state.podcasts.size)
        }
    }

    @Test
    fun `closing search clears query`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Alpha"),
                makePodcast(id = 2L, title = "Bravo"),
            )
            awaitItem()

            viewModel.setSearchActive(true)
            awaitItem()

            viewModel.setSearchQuery("Alpha")
            awaitItem()

            viewModel.setSearchActive(false)
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertFalse(state.isSearchActive)
            assertEquals(2, state.podcasts.size)
        }
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.value = listOf(
                makePodcast(id = 1L, title = "Tech Talk"),
                makePodcast(id = 2L, title = "History Hour"),
            )
            awaitItem()

            viewModel.setSearchQuery("TECH")
            val state = awaitItem()
            assertEquals(1, state.podcasts.size)
            assertEquals("Tech Talk", state.podcasts[0].title)
        }
    }

    // -- Action tests --

    @Test
    fun `setSortOrder delegates to preferencesManager`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSortOrder(LibrarySortOrder.EPISODE_COUNT)
        advanceUntilIdle()

        coVerify { preferencesManager.setLibrarySortOrder(LibrarySortOrder.EPISODE_COUNT) }
    }

    @Test
    fun `toggleViewMode switches from GRID to LIST`() = runTest {
        viewModeFlow.value = LibraryViewMode.GRID

        val viewModel = createViewModel()
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleViewMode()
        advanceUntilIdle()

        coVerify { preferencesManager.setLibraryViewMode(LibraryViewMode.LIST) }
        job.cancel()
    }

    @Test
    fun `toggleViewMode switches from LIST to GRID`() = runTest {
        viewModeFlow.value = LibraryViewMode.LIST

        val viewModel = createViewModel()
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleViewMode()
        advanceUntilIdle()

        coVerify { preferencesManager.setLibraryViewMode(LibraryViewMode.GRID) }
        job.cancel()
    }
}
