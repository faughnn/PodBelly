package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.DayOfWeekStat
import com.podbelly.core.database.dao.EpisodeCompletionStat
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.HourOfDayStat
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDownloadStat
import com.podbelly.core.database.dao.PodcastListeningStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val totalListenedMs: Long = 0L,
    val timeSavedBySpeedMs: Long = 0L,
    val silenceTrimmedMs: Long = 0L,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val listenedThisWeekMs: Long = 0L,
    val listenedThisMonthMs: Long = 0L,
    val averageSessionLengthMs: Long = 0L,
    val mostActiveDay: String = "",
    val mostActiveHour: String = "",
    val averageCompletionPercent: Int = 0,
    val finishedEpisodes: Int = 0,
    val abandonedEpisodes: Int = 0,
    val mostListenedPodcasts: List<PodcastListeningStat> = emptyList(),
    val mostListenedEpisodes: List<EpisodeListeningStat> = emptyList(),
    val mostDownloadedPodcasts: List<PodcastDownloadStat> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    listeningSessionDao: ListeningSessionDao,
) : ViewModel() {

    private val now = System.currentTimeMillis()

    val uiState: StateFlow<StatsUiState> = combine(
        listeningSessionDao.getTotalListenedMs(),
        listeningSessionDao.getTimeSavedBySpeed(),
        listeningSessionDao.getTotalSilenceTrimmedMs(),
        listeningSessionDao.getMostListenedPodcasts(10),
        listeningSessionDao.getMostListenedEpisodes(10),
    ) { totalListened, timeSavedBySpeed, silenceTrimmed, mostPodcasts, mostEpisodes ->
        PartialBase(totalListened, timeSavedBySpeed, silenceTrimmed, mostPodcasts, mostEpisodes)
    }.combine(
        listeningSessionDao.getMostDownloadedPodcasts(10),
    ) { base, mostDownloaded ->
        base to mostDownloaded
    }.combine(combine(
        listeningSessionDao.getListenedMsSince(now - 7 * 86400000L),
        listeningSessionDao.getListenedMsSince(now - 30 * 86400000L),
        listeningSessionDao.getListeningDays(),
        listeningSessionDao.getAverageSessionLengthMs(),
        listeningSessionDao.getListeningMsByDayOfWeek(),
    ) { week, month, days, avgSession, dayOfWeek ->
        PartialNew(week, month, days, avgSession, dayOfWeek)
    }) { (base, mostDownloaded), newStats ->
        Triple(base, mostDownloaded, newStats)
    }.combine(combine(
        listeningSessionDao.getListeningMsByHourOfDay(),
        listeningSessionDao.getEpisodeCompletionStats(),
    ) { hours, completion -> hours to completion }
    ) { (base, mostDownloaded, newStats), (hours, completion) ->
        val (currentStreak, longestStreak) = calculateStreaks(newStats.listeningDays)
        val (avgCompletion, finished, abandoned) = calculateCompletion(completion)
        val mostActiveDay = newStats.dayOfWeekStats.firstOrNull()?.let { dayName(it.dayOfWeek) } ?: ""
        val mostActiveHour = hours.firstOrNull()?.let { hourName(it.hour) } ?: ""

        StatsUiState(
            totalListenedMs = base.totalListenedMs,
            timeSavedBySpeedMs = base.timeSavedBySpeedMs,
            silenceTrimmedMs = base.silenceTrimmedMs,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            listenedThisWeekMs = newStats.listenedThisWeekMs,
            listenedThisMonthMs = newStats.listenedThisMonthMs,
            averageSessionLengthMs = newStats.averageSessionLengthMs,
            mostActiveDay = mostActiveDay,
            mostActiveHour = mostActiveHour,
            averageCompletionPercent = avgCompletion,
            finishedEpisodes = finished,
            abandonedEpisodes = abandoned,
            mostListenedPodcasts = base.mostListenedPodcasts,
            mostListenedEpisodes = base.mostListenedEpisodes,
            mostDownloadedPodcasts = mostDownloaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    private data class PartialBase(
        val totalListenedMs: Long,
        val timeSavedBySpeedMs: Long,
        val silenceTrimmedMs: Long,
        val mostListenedPodcasts: List<PodcastListeningStat>,
        val mostListenedEpisodes: List<EpisodeListeningStat>,
    )

    private data class PartialNew(
        val listenedThisWeekMs: Long,
        val listenedThisMonthMs: Long,
        val listeningDays: List<Long>,
        val averageSessionLengthMs: Long,
        val dayOfWeekStats: List<DayOfWeekStat>,
    )

    companion object {
        fun calculateStreaks(sortedDays: List<Long>): Pair<Int, Int> {
            if (sortedDays.isEmpty()) return 0 to 0

            val today = System.currentTimeMillis() / 86400000L
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
