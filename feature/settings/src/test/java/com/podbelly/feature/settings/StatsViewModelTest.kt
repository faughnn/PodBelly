package com.podbelly.feature.settings

import app.cash.turbine.test
import com.podbelly.core.database.dao.DayOfWeekStat
import com.podbelly.core.database.dao.EpisodeCompletionStat
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.HourOfDayStat
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDownloadStat
import com.podbelly.core.database.dao.PodcastListeningStat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val listeningSessionDao = mockk<ListeningSessionDao>(relaxed = true)

    private val totalListenedFlow = MutableStateFlow(0L)
    private val timeSavedBySpeedFlow = MutableStateFlow(0L)
    private val silenceTrimmedFlow = MutableStateFlow(0L)
    private val mostListenedPodcastsFlow = MutableStateFlow<List<PodcastListeningStat>>(emptyList())
    private val mostListenedEpisodesFlow = MutableStateFlow<List<EpisodeListeningStat>>(emptyList())
    private val mostDownloadedPodcastsFlow = MutableStateFlow<List<PodcastDownloadStat>>(emptyList())
    private val listenedSinceFlow = MutableStateFlow(0L)
    private val listeningDaysFlow = MutableStateFlow<List<Long>>(emptyList())
    private val averageSessionFlow = MutableStateFlow(0L)
    private val dayOfWeekFlow = MutableStateFlow<List<DayOfWeekStat>>(emptyList())
    private val hourOfDayFlow = MutableStateFlow<List<HourOfDayStat>>(emptyList())
    private val completionStatsFlow = MutableStateFlow<List<EpisodeCompletionStat>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { listeningSessionDao.getTotalListenedMs() } returns totalListenedFlow
        every { listeningSessionDao.getTimeSavedBySpeed() } returns timeSavedBySpeedFlow
        every { listeningSessionDao.getTotalSilenceTrimmedMs() } returns silenceTrimmedFlow
        every { listeningSessionDao.getMostListenedPodcasts(10) } returns mostListenedPodcastsFlow
        every { listeningSessionDao.getMostListenedEpisodes(10) } returns mostListenedEpisodesFlow
        every { listeningSessionDao.getMostDownloadedPodcasts(10) } returns mostDownloadedPodcastsFlow
        every { listeningSessionDao.getListenedMsSince(any()) } returns listenedSinceFlow
        every { listeningSessionDao.getListeningDays() } returns listeningDaysFlow
        every { listeningSessionDao.getAverageSessionLengthMs() } returns averageSessionFlow
        every { listeningSessionDao.getListeningMsByDayOfWeek() } returns dayOfWeekFlow
        every { listeningSessionDao.getListeningMsByHourOfDay() } returns hourOfDayFlow
        every { listeningSessionDao.getEpisodeCompletionStats() } returns completionStatsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StatsViewModel {
        return StatsViewModel(
            listeningSessionDao = listeningSessionDao,
        )
    }

    @Test
    fun `initial state has zero values and empty lists`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0L, state.totalListenedMs)
            assertEquals(0L, state.timeSavedBySpeedMs)
            assertEquals(0L, state.silenceTrimmedMs)
            assertTrue(state.mostListenedPodcasts.isEmpty())
            assertTrue(state.mostListenedEpisodes.isEmpty())
            assertTrue(state.mostDownloadedPodcasts.isEmpty())
        }
    }

    @Test
    fun `emitting listening stats populates total listened time`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            totalListenedFlow.value = 3600000L // 1 hour

            val state = awaitItem()
            assertEquals(3600000L, state.totalListenedMs)
        }
    }

    @Test
    fun `emitting time saved by speed populates UI state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            timeSavedBySpeedFlow.value = 1800000L // 30 minutes

            val state = awaitItem()
            assertEquals(1800000L, state.timeSavedBySpeedMs)
        }
    }

    @Test
    fun `emitting silence trimmed populates UI state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            silenceTrimmedFlow.value = 600000L // 10 minutes

            val state = awaitItem()
            assertEquals(600000L, state.silenceTrimmedMs)
        }
    }

    @Test
    fun `emitting most listened podcasts populates UI state`() = runTest {
        val stats = listOf(
            PodcastListeningStat(
                podcastId = 1L,
                podcastTitle = "Popular Podcast",
                artworkUrl = "https://example.com/art.jpg",
                totalListenedMs = 7200000L,
                episodeCount = 5L,
            ),
            PodcastListeningStat(
                podcastId = 2L,
                podcastTitle = "Less Popular",
                artworkUrl = "https://example.com/art2.jpg",
                totalListenedMs = 3600000L,
                episodeCount = 2L,
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            mostListenedPodcastsFlow.value = stats

            val state = awaitItem()
            assertEquals(2, state.mostListenedPodcasts.size)
            assertEquals("Popular Podcast", state.mostListenedPodcasts[0].podcastTitle)
            assertEquals(7200000L, state.mostListenedPodcasts[0].totalListenedMs)
            assertEquals("Less Popular", state.mostListenedPodcasts[1].podcastTitle)
        }
    }

    @Test
    fun `emitting most listened episodes populates UI state`() = runTest {
        val stats = listOf(
            EpisodeListeningStat(
                episodeId = 10L,
                episodeTitle = "Great Episode",
                podcastTitle = "Podcast A",
                totalListenedMs = 5400000L,
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            mostListenedEpisodesFlow.value = stats

            val state = awaitItem()
            assertEquals(1, state.mostListenedEpisodes.size)
            assertEquals("Great Episode", state.mostListenedEpisodes[0].episodeTitle)
            assertEquals("Podcast A", state.mostListenedEpisodes[0].podcastTitle)
            assertEquals(5400000L, state.mostListenedEpisodes[0].totalListenedMs)
        }
    }

    @Test
    fun `emitting most downloaded podcasts populates UI state`() = runTest {
        val stats = listOf(
            PodcastDownloadStat(
                podcastId = 1L,
                podcastTitle = "Downloaded Show",
                artworkUrl = "https://example.com/art.jpg",
                downloadCount = 15L,
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            mostDownloadedPodcastsFlow.value = stats

            val state = awaitItem()
            assertEquals(1, state.mostDownloadedPodcasts.size)
            assertEquals("Downloaded Show", state.mostDownloadedPodcasts[0].podcastTitle)
            assertEquals(15L, state.mostDownloadedPodcasts[0].downloadCount)
        }
    }

    @Test
    fun `all stats update together`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            totalListenedFlow.value = 10000000L
            timeSavedBySpeedFlow.value = 2000000L
            silenceTrimmedFlow.value = 500000L
            mostListenedPodcastsFlow.value = listOf(
                PodcastListeningStat(1L, "P1", "", 5000000L, 3L),
            )
            mostListenedEpisodesFlow.value = listOf(
                EpisodeListeningStat(10L, "E1", "P1", 3000000L),
            )
            mostDownloadedPodcastsFlow.value = listOf(
                PodcastDownloadStat(1L, "P1", "", 5L),
            )

            // May get multiple intermediate emissions; collect until we have everything
            var state = awaitItem()
            // Keep consuming until all values are populated
            while (state.mostDownloadedPodcasts.isEmpty() || state.mostListenedEpisodes.isEmpty()) {
                state = awaitItem()
            }

            assertEquals(10000000L, state.totalListenedMs)
            assertEquals(2000000L, state.timeSavedBySpeedMs)
            assertEquals(500000L, state.silenceTrimmedMs)
            assertEquals(1, state.mostListenedPodcasts.size)
            assertEquals(1, state.mostListenedEpisodes.size)
            assertEquals(1, state.mostDownloadedPodcasts.size)
        }
    }

    @Test
    fun `streak calculation from listening days`() = runTest {
        val today = System.currentTimeMillis() / 86400000L
        listeningDaysFlow.value = listOf(today - 5, today - 4, today - 3, today - 1, today)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.currentStreak == 0) state = awaitItem()

            assertEquals(2, state.currentStreak)
            assertEquals(3, state.longestStreak)
        }
    }

    @Test
    fun `completion rates calculated from episode stats`() = runTest {
        completionStatsFlow.value = listOf(
            EpisodeCompletionStat(1L, 900000L, 1000000L),  // 90%
            EpisodeCompletionStat(2L, 500000L, 1000000L),  // 50%
            EpisodeCompletionStat(3L, 100000L, 1000000L),  // 10%
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.averageCompletionPercent == 0) state = awaitItem()

            assertEquals(50, state.averageCompletionPercent)
            assertEquals(1, state.finishedEpisodes)
            assertEquals(1, state.abandonedEpisodes)
        }
    }

    @Test
    fun `most active day derived from day of week stats`() = runTest {
        dayOfWeekFlow.value = listOf(
            DayOfWeekStat(dayOfWeek = 0, totalListenedMs = 500000L),
            DayOfWeekStat(dayOfWeek = 4, totalListenedMs = 300000L),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.mostActiveDay.isEmpty()) state = awaitItem()

            assertEquals("Monday", state.mostActiveDay)
        }
    }

    @Test
    fun `most active hour derived from hour of day stats`() = runTest {
        hourOfDayFlow.value = listOf(
            HourOfDayStat(hour = 8, totalListenedMs = 500000L),
            HourOfDayStat(hour = 20, totalListenedMs = 300000L),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.mostActiveHour.isEmpty()) state = awaitItem()

            assertEquals("8 AM", state.mostActiveHour)
        }
    }
}
