package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.DailyListeningStat
import com.podbelly.core.database.dao.DayOfWeekStat
import com.podbelly.core.database.dao.EpisodeCompletionStat
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.HourOfDayStat
import com.podbelly.core.database.dao.KeepUpStat
import com.podbelly.core.database.dao.LibraryStat
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.PodcastDownloadStat
import com.podbelly.core.database.dao.PodcastEngagementStat
import com.podbelly.core.database.dao.PodcastListeningStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/** Date range every session-derived stat on the Overview tab is filtered to. */
enum class StatsPeriod(val label: String) {
    ALL_TIME("All time"),
    THIS_YEAR("This year"),
    LAST_30_DAYS("Last 30 days");

    /** Earliest session start (epoch ms) included in this period; 0 = everything. */
    fun cutoff(now: Long = System.currentTimeMillis()): Long = when (this) {
        ALL_TIME -> 0L
        THIS_YEAR -> Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        LAST_30_DAYS -> now - 30L * DAY_MS
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}

data class StatsUiState(
    // Headline + rolling windows (windows are always rolling, not period-filtered)
    val totalListenedMs: Long = 0L,
    val listenedTodayMs: Long = 0L,
    val listenedThisWeekMs: Long = 0L,
    val listenedThisMonthMs: Long = 0L,
    // Time saved breakdown
    val timeSavedBySpeedMs: Long = 0L,
    val silenceTrimmedMs: Long = 0L,
    val skipSavedMs: Long = 0L,
    // Streaks are lifetime by definition
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    // Habits
    val averageSessionLengthMs: Long = 0L,
    val averageSpeed: Float = 0f,
    val mostActiveDay: String = "",
    val mostActiveHour: String = "",
    val dayOfWeekStats: List<DayOfWeekStat> = emptyList(),
    val hourOfDayStats: List<HourOfDayStat> = emptyList(),
    // Sessions & totals
    val sessionCount: Int = 0,
    val longestSessionMs: Long = 0L,
    val daysListened: Int = 0,
    val averagePerActiveDayMs: Long = 0L,
    // Last-30-days chart (always the trailing month regardless of period)
    val dailyListening: List<DailyListeningStat> = emptyList(),
    // Keep-up (episodes that arrived in the last 30 days)
    val keepUpPlayed: Int = 0,
    val keepUpTotal: Int = 0,
    // Library summary (always whole-library)
    val subscriptionCount: Int = 0,
    val libraryEpisodeCount: Int = 0,
    val libraryPlayedCount: Int = 0,
    val downloadedBytes: Long = 0L,
    // Completion
    val averageCompletionPercent: Int = 0,
    val finishedEpisodes: Int = 0,
    val abandonedEpisodes: Int = 0,
    // Top lists
    val mostListenedPodcasts: List<PodcastListeningStat> = emptyList(),
    val mostListenedEpisodes: List<EpisodeListeningStat> = emptyList(),
    val mostDownloadedPodcasts: List<PodcastDownloadStat> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val listeningSessionDao: ListeningSessionDao,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) : ViewModel() {

    // Bucket day/hour/streak stats in the device's local time, not UTC.
    private val tzOffsetMs = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()

    private val _period = MutableStateFlow(StatsPeriod.ALL_TIME)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun setPeriod(period: StatsPeriod) {
        _period.value = period
    }

    val uiState: StateFlow<StatsUiState> = _period
        .flatMapLatest { period ->
            // Capture cutoffs when collection (re)starts, not at construction, so
            // rolling windows don't go stale while the screen stays open.
            flow { emitAll(statsFlow(period.cutoff())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    /**
     * Per-podcast engagement for the "Podcasts" tab, least listened first, so the
     * subscriptions gathering dust are the first thing on screen.
     */
    val engagementStats: StateFlow<List<PodcastEngagementStat>> =
        listeningSessionDao.getPodcastEngagementStats()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun unsubscribe(podcastId: Long) {
        viewModelScope.launch {
            podcastDao.unsubscribe(podcastId)
        }
    }

    /** Undo for [unsubscribe] (the snackbar's Undo action). */
    fun undoUnsubscribe(podcastId: Long) {
        viewModelScope.launch {
            podcastDao.resubscribe(podcastId)
        }
    }

    // -----------------------------------------------------------------------
    // Flow assembly
    // -----------------------------------------------------------------------

    private data class Totals(
        val totalListenedMs: Long,
        val speedSavedMs: Long,
        val silenceSavedMs: Long,
        val skipSavedMs: Long,
        val averageSpeed: Double,
    )

    private data class Windows(
        val todayMs: Long,
        val weekMs: Long,
        val monthMs: Long,
    )

    private data class Sessions(
        val count: Int,
        val longestMs: Long,
        val averageLengthMs: Long,
        val listeningDays: List<Long>,
    )

    private data class Distributions(
        val daily: List<DailyListeningStat>,
        val dayOfWeek: List<DayOfWeekStat>,
        val hourOfDay: List<HourOfDayStat>,
    )

    private data class Library(
        val subscriptions: Int,
        val library: LibraryStat,
        val downloadedBytes: Long,
        val keepUp: KeepUpStat,
    )

    private data class Aux(
        val library: Library,
        val topPodcasts: List<PodcastListeningStat>,
        val topEpisodes: List<EpisodeListeningStat>,
        val topDownloads: List<PodcastDownloadStat>,
        val completion: List<EpisodeCompletionStat>,
        val allTimeDays: List<Long>,
    )

    private fun statsFlow(since: Long): Flow<StatsUiState> {
        val now = System.currentTimeMillis()

        val totals = combine(
            listeningSessionDao.getTotalListenedMs(since),
            listeningSessionDao.getTimeSavedBySpeed(since),
            listeningSessionDao.getTotalSilenceTrimmedMs(since),
            listeningSessionDao.getTotalSkipSavedMs(since),
            listeningSessionDao.getWeightedAverageSpeed(since),
            ::Totals,
        )

        val windows = combine(
            listeningSessionDao.getListenedMsSince(now - DAY_MS),
            listeningSessionDao.getListenedMsSince(now - 7 * DAY_MS),
            listeningSessionDao.getListenedMsSince(now - 30 * DAY_MS),
            ::Windows,
        )

        val sessions = combine(
            listeningSessionDao.getSessionCount(since),
            listeningSessionDao.getLongestSessionMs(since),
            listeningSessionDao.getAverageSessionLengthMs(since),
            listeningSessionDao.getListeningDays(tzOffsetMs, since),
            ::Sessions,
        )

        val distributions = combine(
            listeningSessionDao.getListenedMsPerDay(tzOffsetMs, now - 30 * DAY_MS),
            listeningSessionDao.getListeningMsByDayOfWeek(tzOffsetMs, since),
            listeningSessionDao.getListeningMsByHourOfDay(tzOffsetMs, since),
            ::Distributions,
        )

        val library = combine(
            podcastDao.getSubscribedCount(),
            episodeDao.getLibraryStats(),
            episodeDao.getTotalDownloadedBytes(),
            episodeDao.getKeepUpStats(now - 30 * DAY_MS),
            ::Library,
        )

        val aux = combine(
            library,
            combine(
                listeningSessionDao.getMostListenedPodcasts(10, since),
                listeningSessionDao.getMostListenedEpisodes(10, since),
                listeningSessionDao.getMostDownloadedPodcasts(10),
            ) { p, e, d -> Triple(p, e, d) },
            listeningSessionDao.getEpisodeCompletionStats(since),
            listeningSessionDao.getListeningDays(tzOffsetMs),
        ) { lib, (topPodcasts, topEpisodes, topDownloads), completion, allDays ->
            Aux(lib, topPodcasts, topEpisodes, topDownloads, completion, allDays)
        }

        return combine(totals, windows, sessions, distributions, aux) {
                t, w, s, dist, extra ->
            val (currentStreak, longestStreak) = calculateStreaks(
                extra.allTimeDays,
                (System.currentTimeMillis() + tzOffsetMs) / DAY_MS,
            )
            val (avgCompletion, finished, abandoned) = calculateCompletion(extra.completion)

            StatsUiState(
                totalListenedMs = t.totalListenedMs,
                listenedTodayMs = w.todayMs,
                listenedThisWeekMs = w.weekMs,
                listenedThisMonthMs = w.monthMs,
                timeSavedBySpeedMs = t.speedSavedMs,
                silenceTrimmedMs = t.silenceSavedMs,
                skipSavedMs = t.skipSavedMs,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                averageSessionLengthMs = s.averageLengthMs,
                averageSpeed = t.averageSpeed.toFloat(),
                mostActiveDay = dist.dayOfWeek.firstOrNull()?.let { dayName(it.dayOfWeek) } ?: "",
                mostActiveHour = dist.hourOfDay.firstOrNull()?.let { hourName(it.hour) } ?: "",
                dayOfWeekStats = dist.dayOfWeek,
                hourOfDayStats = dist.hourOfDay,
                sessionCount = s.count,
                longestSessionMs = s.longestMs,
                daysListened = s.listeningDays.size,
                averagePerActiveDayMs = if (s.listeningDays.isNotEmpty()) {
                    t.totalListenedMs / s.listeningDays.size
                } else 0L,
                dailyListening = dist.daily,
                keepUpPlayed = extra.library.keepUp.playedCount,
                keepUpTotal = extra.library.keepUp.totalCount,
                subscriptionCount = extra.library.subscriptions,
                libraryEpisodeCount = extra.library.library.episodeCount,
                libraryPlayedCount = extra.library.library.playedCount,
                downloadedBytes = extra.library.downloadedBytes,
                averageCompletionPercent = avgCompletion,
                finishedEpisodes = finished,
                abandonedEpisodes = abandoned,
                mostListenedPodcasts = extra.topPodcasts,
                mostListenedEpisodes = extra.topEpisodes,
                mostDownloadedPodcasts = extra.topDownloads,
            )
        }
    }

    companion object {
        private const val DAY_MS = 86_400_000L

        fun calculateStreaks(
            sortedDays: List<Long>,
            today: Long = System.currentTimeMillis() / 86400000L,
        ): Pair<Int, Int> {
            if (sortedDays.isEmpty()) return 0 to 0

            var longestStreak = 1
            var streak = 1

            for (i in 1..sortedDays.lastIndex) {
                if (sortedDays[i] - sortedDays[i - 1] == 1L) {
                    streak++
                } else {
                    longestStreak = maxOf(longestStreak, streak)
                    streak = 1
                }
            }
            longestStreak = maxOf(longestStreak, streak)

            // Current streak: count consecutive days ending at today or yesterday
            var currentStreak = 0
            val lastDay = sortedDays.last()
            if (lastDay == today || lastDay == today - 1) {
                currentStreak = 1
                for (i in sortedDays.lastIndex downTo 1) {
                    if (sortedDays[i] - sortedDays[i - 1] == 1L) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }

            return currentStreak to longestStreak
        }

        fun calculateCompletion(stats: List<EpisodeCompletionStat>): Triple<Int, Int, Int> {
            if (stats.isEmpty()) return Triple(0, 0, 0)

            val percentages = stats.map { stat ->
                ((stat.totalListenedMs.toDouble() / stat.durationMs) * 100).coerceAtMost(100.0)
            }
            val avg = percentages.average().toInt()
            val finished = percentages.count { it >= 90.0 }
            val abandoned = percentages.count { it < 25.0 }

            return Triple(avg, finished, abandoned)
        }

        fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            5 -> "Saturday"
            6 -> "Sunday"
            else -> ""
        }

        fun hourName(hour: Int): String = when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
