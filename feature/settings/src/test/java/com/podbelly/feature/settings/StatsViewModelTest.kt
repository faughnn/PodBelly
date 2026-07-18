package com.podbelly.feature.settings

import app.cash.turbine.test
import com.podbelly.core.database.dao.DayOfWeekStat
import com.podbelly.core.database.dao.EpisodeCompletionStat
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.HourOfDayStat
import com.podbelly.core.database.dao.DailyListeningStat
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.KeepUpStat
import com.podbelly.core.database.dao.LibraryStat
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.PodcastDownloadStat
import com.podbelly.core.database.dao.PodcastEngagementStat
import com.podbelly.core.database.dao.PodcastListeningStat
import io.mockk.coVerify
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
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)

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
    private val engagementStatsFlow = MutableStateFlow<List<PodcastEngagementStat>>(emptyList())
    private val skipSavedFlow = MutableStateFlow(0L)
    private val weightedSpeedFlow = MutableStateFlow(0.0)
    private val sessionCountFlow = MutableStateFlow(0)
    private val longestSessionFlow = MutableStateFlow(0L)
    private val dailyListeningFlow = MutableStateFlow<List<DailyListeningStat>>(emptyList())
    private val subscribedCountFlow = MutableStateFlow(0)
    private val libraryStatsFlow = MutableStateFlow(LibraryStat(episodeCount = 0, playedCount = 0))
    private val downloadedBytesFlow = MutableStateFlow(0L)
    private val keepUpFlow = MutableStateFlow(KeepUpStat(totalCount = 0, playedCount = 0))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { listeningSessionDao.getTotalListenedMs(any()) } returns totalListenedFlow
        every { listeningSessionDao.getTimeSavedBySpeed(any()) } returns timeSavedBySpeedFlow
        every { listeningSessionDao.getTotalSilenceTrimmedMs(any()) } returns silenceTrimmedFlow
        every { listeningSessionDao.getTotalSkipSavedMs(any()) } returns skipSavedFlow
        every { listeningSessionDao.getWeightedAverageSpeed(any()) } returns weightedSpeedFlow
        every { listeningSessionDao.getSessionCount(any()) } returns sessionCountFlow
        every { listeningSessionDao.getLongestSessionMs(any()) } returns longestSessionFlow
        every { listeningSessionDao.getListenedMsPerDay(any(), any()) } returns dailyListeningFlow
        every { listeningSessionDao.getMostListenedPodcasts(10, any()) } returns mostListenedPodcastsFlow
        every { listeningSessionDao.getMostListenedEpisodes(10, any()) } returns mostListenedEpisodesFlow
        every { listeningSessionDao.getMostDownloadedPodcasts(10) } returns mostDownloadedPodcastsFlow
        every { listeningSessionDao.getListenedMsSince(any()) } returns listenedSinceFlow
        every { listeningSessionDao.getListeningDays(any(), any()) } returns listeningDaysFlow
        every { listeningSessionDao.getAverageSessionLengthMs(any()) } returns averageSessionFlow
        every { listeningSessionDao.getListeningMsByDayOfWeek(any(), any()) } returns dayOfWeekFlow
        every { listeningSessionDao.getListeningMsByHourOfDay(any(), any()) } returns hourOfDayFlow
        every { listeningSessionDao.getEpisodeCompletionStats(any()) } returns completionStatsFlow
        every { listeningSessionDao.getPodcastEngagementStats() } returns engagementStatsFlow
        every { podcastDao.getSubscribedCount() } returns subscribedCountFlow
        every { episodeDao.getLibraryStats() } returns libraryStatsFlow
        every { episodeDao.getTotalDownloadedBytes() } returns downloadedBytesFlow
        every { episodeDao.getKeepUpStats(any()) } returns keepUpFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StatsViewModel {
        return StatsViewModel(
            listeningSessionDao = listeningSessionDao,
            podcastDao = podcastDao,
            episodeDao = episodeDao,
        )
    }

    private fun makeEngagementStat(
        podcastId: Long = 1L,
        totalListenedMs: Long = 0L,
    ) = PodcastEngagementStat(
        podcastId = podcastId,
        podcastTitle = "Show $podcastId",
        artworkUrl = "",
        subscribedAt = 1_000L,
        totalListenedMs = totalListenedMs,
        lastListenedAt = 0L,
        episodeCount = 10L,
        playedCount = 0L,
        inProgressCount = 0L,
        downloadedCount = 0L,
        downloadedBytes = 0L,
        latestEpisodeAt = 0L,
    )

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

    // -- New overview stats --

    @Test
    fun `skip savings and average speed flow into the UI state`() = runTest {
        skipSavedFlow.value = 240_000L
        weightedSpeedFlow.value = 1.42

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.skipSavedMs == 0L) state = awaitItem()

            assertEquals(240_000L, state.skipSavedMs)
            assertEquals(1.42f, state.averageSpeed, 0.001f)
        }
    }

    @Test
    fun `session, keep-up and library stats flow into the UI state`() = runTest {
        sessionCountFlow.value = 42
        longestSessionFlow.value = 5_400_000L
        subscribedCountFlow.value = 106
        libraryStatsFlow.value = LibraryStat(episodeCount = 2000, playedCount = 500)
        downloadedBytesFlow.value = 1_234L
        keepUpFlow.value = KeepUpStat(totalCount = 38, playedCount = 12)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.sessionCount == 0) state = awaitItem()

            assertEquals(42, state.sessionCount)
            assertEquals(5_400_000L, state.longestSessionMs)
            assertEquals(106, state.subscriptionCount)
            assertEquals(2000, state.libraryEpisodeCount)
            assertEquals(500, state.libraryPlayedCount)
            assertEquals(1_234L, state.downloadedBytes)
            assertEquals(38, state.keepUpTotal)
            assertEquals(12, state.keepUpPlayed)
        }
    }

    @Test
    fun `average per active day divides total by listening days`() = runTest {
        val today = System.currentTimeMillis() / 86400000L
        totalListenedFlow.value = 3_000_000L
        listeningDaysFlow.value = listOf(today - 2, today - 1, today)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.daysListened == 0) state = awaitItem()

            assertEquals(3, state.daysListened)
            assertEquals(1_000_000L, state.averagePerActiveDayMs)
        }
    }

    @Test
    fun `selecting a period re-queries with a non-zero cutoff`() = runTest {
        val cutoffs = mutableListOf<Long>()
        every { listeningSessionDao.getTotalListenedMs(capture(cutoffs)) } returns totalListenedFlow

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // ALL_TIME collection started
            viewModel.setPeriod(StatsPeriod.LAST_30_DAYS)
            // The re-collected state can be identical (all stubs unchanged), so
            // drive the scheduler instead of awaiting a (deduped) emission.
            testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0L, cutoffs.first())
        assertTrue(cutoffs.last() > 0L)
    }

    @Test
    fun `period cutoffs are ordered sensibly`() {
        val now = System.currentTimeMillis()
        assertEquals(0L, StatsPeriod.ALL_TIME.cutoff(now))
        assertTrue(StatsPeriod.LAST_30_DAYS.cutoff(now) == now - 30L * 86_400_000L)
        val yearStart = StatsPeriod.THIS_YEAR.cutoff(now)
        assertTrue(yearStart in 1..now)
    }

    // -- Podcast engagement tab --

    @Test
    fun `engagementStats exposes the least-listened list from the dao`() = runTest {
        val stats = listOf(
            makeEngagementStat(podcastId = 1L, totalListenedMs = 0L),
            makeEngagementStat(podcastId = 2L, totalListenedMs = 5_000L),
        )
        engagementStatsFlow.value = stats

        val viewModel = createViewModel()

        viewModel.engagementStats.test {
            var value = awaitItem()
            while (value.isEmpty()) value = awaitItem()
            assertEquals(stats, value)
        }
    }

    @Test
    fun `unsubscribe delegates to the dao`() = runTest {
        val viewModel = createViewModel()

        viewModel.unsubscribe(7L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { podcastDao.unsubscribe(7L) }
    }

    @Test
    fun `undoUnsubscribe resubscribes via the dao`() = runTest {
        val viewModel = createViewModel()

        viewModel.undoUnsubscribe(7L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { podcastDao.resubscribe(7L) }
    }
}
