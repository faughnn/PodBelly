package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One subscription on the auto-download manager screen. */
data class AutoDownloadShow(
    val podcastId: Long,
    val title: String,
    val artworkUrl: String,
    val autoDownloadMode: Int,
    val totalListenedMs: Long,
    val lastListenedAt: Long,
    /** Listened to within the smart window — what Smart mode keys off. */
    val engaged: Boolean,
)

data class AutoDownloadUiState(
    val smartEnabled: Boolean = false,
    val windowDays: Int = 30,
    val shows: List<AutoDownloadShow> = emptyList(),
)

/**
 * Backs the auto-download manager screen: every subscription joined with its
 * listening stats, so choosing which shows to Always/Never download is an
 * informed decision rather than guesswork.
 */
@HiltViewModel
class AutoDownloadViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    listeningSessionDao: ListeningSessionDao,
    preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<AutoDownloadUiState> = combine(
        podcastDao.getAll(),
        listeningSessionDao.getPodcastEngagementStats(),
        preferencesManager.smartAutoDownload,
        preferencesManager.smartAutoDownloadWindowDays,
    ) { podcasts, stats, smartEnabled, windowDays ->
        val statsById = stats.associateBy { it.podcastId }
        val cutoff = System.currentTimeMillis() - windowDays * 86_400_000L
        AutoDownloadUiState(
            smartEnabled = smartEnabled,
            windowDays = windowDays,
            shows = podcasts.map { podcast ->
                val stat = statsById[podcast.id]
                val lastListenedAt = stat?.lastListenedAt ?: 0L
                AutoDownloadShow(
                    podcastId = podcast.id,
                    title = podcast.title,
                    artworkUrl = podcast.artworkUrl,
                    autoDownloadMode = podcast.autoDownloadMode,
                    totalListenedMs = stat?.totalListenedMs ?: 0L,
                    lastListenedAt = lastListenedAt,
                    engaged = lastListenedAt >= cutoff,
                )
                // Most recently listened first: the shows worth pinning to Always
                // lead, the never-listened tail is where Never candidates live.
            }.sortedByDescending { it.lastListenedAt },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoDownloadUiState(),
    )

    fun setMode(podcastId: Long, mode: Int) {
        viewModelScope.launch {
            podcastDao.setAutoDownloadMode(podcastId, mode)
        }
    }
}
