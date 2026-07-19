package com.podbelly.feature.settings

import app.cash.turbine.test
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.PodcastEngagementStat
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_ALWAYS
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_SMART
import com.podbelly.core.database.entity.PodcastEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoDownloadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val listeningSessionDao = mockk<ListeningSessionDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>()

    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())
    private val statsFlow = MutableStateFlow<List<PodcastEngagementStat>>(emptyList())
    private val smartEnabledFlow = MutableStateFlow(true)
    private val windowDaysFlow = MutableStateFlow(30)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { podcastDao.getAll() } returns podcastsFlow
        every { listeningSessionDao.getPodcastEngagementStats() } returns statsFlow
        every { preferencesManager.smartAutoDownload } returns smartEnabledFlow
        every { preferencesManager.smartAutoDownloadWindowDays } returns windowDaysFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makePodcast(id: Long, title: String, mode: Int = AUTO_DOWNLOAD_SMART) =
        PodcastEntity(
            id = id,
            feedUrl = "https://example.com/$id",
            title = title,
            author = "Author",
            description = "Description",
            artworkUrl = "https://art.com/$id.jpg",
            link = "",
            language = "en",
            lastBuildDate = 0L,
            subscribedAt = 0L,
            autoDownloadMode = mode,
        )

    private fun makeStat(
        podcastId: Long,
        totalListenedMs: Long,
        lastListenedAt: Long,
    ) = PodcastEngagementStat(
        podcastId = podcastId,
        podcastTitle = "Show $podcastId",
        artworkUrl = "",
        feedUrl = "https://example.com/$podcastId",
        subscribedAt = 0L,
        totalListenedMs = totalListenedMs,
        lastListenedAt = lastListenedAt,
        episodeCount = 10L,
        playedCount = 3L,
        inProgressCount = 1L,
        downloadedCount = 2L,
        downloadedBytes = 1024L,
        latestEpisodeAt = 0L,
    )

    private fun createViewModel() =
        AutoDownloadViewModel(podcastDao, listeningSessionDao, preferencesManager)

    @Test
    fun `shows join listening stats and sort most recently listened first`() = runTest {
        val now = System.currentTimeMillis()
        podcastsFlow.value = listOf(
            makePodcast(1L, "Dusty Show"),
            makePodcast(2L, "Daily Listen", mode = AUTO_DOWNLOAD_ALWAYS),
        )
        statsFlow.value = listOf(
            makeStat(1L, totalListenedMs = 60_000L, lastListenedAt = now - 90L * 86_400_000L),
            makeStat(2L, totalListenedMs = 3_600_000L, lastListenedAt = now - 86_400_000L),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertTrue(state.smartEnabled)
            assertEquals(30, state.windowDays)
            assertEquals(listOf(2L, 1L), state.shows.map { it.podcastId })

            val daily = state.shows[0]
            assertEquals("Daily Listen", daily.title)
            assertEquals(AUTO_DOWNLOAD_ALWAYS, daily.autoDownloadMode)
            assertEquals(3_600_000L, daily.totalListenedMs)
            // Listened a day ago: inside the 30-day smart window.
            assertTrue(daily.engaged)

            // Listened 90 days ago: outside the window.
            assertFalse(state.shows[1].engaged)
        }
    }

    @Test
    fun `a show with no listening history is never engaged`() = runTest {
        podcastsFlow.value = listOf(makePodcast(1L, "Untouched"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val show = expectMostRecentItem().shows.single()
            assertFalse(show.engaged)
            assertEquals(0L, show.totalListenedMs)
        }
    }

    @Test
    fun `setMode persists the override`() = runTest {
        val viewModel = createViewModel()

        viewModel.setMode(7L, AUTO_DOWNLOAD_NEVER)
        advanceUntilIdle()

        coVerify { podcastDao.setAutoDownloadMode(7L, AUTO_DOWNLOAD_NEVER) }
    }
}
