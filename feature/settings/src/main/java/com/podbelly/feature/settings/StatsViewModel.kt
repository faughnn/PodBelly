package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeListeningStat
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
    val mostListenedPodcasts: List<PodcastListeningStat> = emptyList(),
    val mostListenedEpisodes: List<EpisodeListeningStat> = emptyList(),
    val mostDownloadedPodcasts: List<PodcastDownloadStat> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    listeningSessionDao: ListeningSessionDao,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        listeningSessionDao.getTotalListenedMs(),
        listeningSessionDao.getTimeSavedBySpeed(),
        listeningSessionDao.getTotalSilenceTrimmedMs(),
        listeningSessionDao.getMostListenedPodcasts(10),
        listeningSessionDao.getMostListenedEpisodes(10),
    ) { totalListened, timeSavedBySpeed, silenceTrimmed, mostListenedPodcasts, mostListenedEpisodes ->
        PartialStats(
            totalListenedMs = totalListened,
            timeSavedBySpeedMs = timeSavedBySpeed,
            silenceTrimmedMs = silenceTrimmed,
            mostListenedPodcasts = mostListenedPodcasts,
            mostListenedEpisodes = mostListenedEpisodes,
        )
    }.combine(
        listeningSessionDao.getMostDownloadedPodcasts(10),
    ) { partial, mostDownloaded ->
        StatsUiState(
            totalListenedMs = partial.totalListenedMs,
            timeSavedBySpeedMs = partial.timeSavedBySpeedMs,
            silenceTrimmedMs = partial.silenceTrimmedMs,
            mostListenedPodcasts = partial.mostListenedPodcasts,
            mostListenedEpisodes = partial.mostListenedEpisodes,
            mostDownloadedPodcasts = mostDownloaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    private data class PartialStats(
        val totalListenedMs: Long,
        val timeSavedBySpeedMs: Long,
        val silenceTrimmedMs: Long,
        val mostListenedPodcasts: List<PodcastListeningStat>,
        val mostListenedEpisodes: List<EpisodeListeningStat>,
    )
}
