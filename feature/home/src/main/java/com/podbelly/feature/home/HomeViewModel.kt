package com.podbelly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeEpisodeItem(
    val episodeId: Long,
    val title: String,
    val podcastTitle: String,
    val artworkUrl: String,
    val publicationDate: Long,
    val durationSeconds: Int,
    val played: Boolean,
    val downloadPath: String,
    val playbackPosition: Long = 0L,
)

data class HomeUiState(
    val recentEpisodes: List<HomeEpisodeItem> = emptyList(),
    val inProgressEpisodes: List<HomeEpisodeItem> = emptyList(),
    val isEmpty: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress

    private val _showMobileDataWarning = MutableStateFlow(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        episodeDao.getRecentEpisodes(50),
        podcastDao.getAll(),
    ) { episodes, podcasts ->
        val podcastMap: Map<Long, PodcastEntity> = podcasts.associateBy { it.id }

        val items = episodes.mapNotNull { episode ->
            val podcast = podcastMap[episode.podcastId] ?: return@mapNotNull null
            val artwork = episode.artworkUrl.ifBlank { podcast.artworkUrl }
            HomeEpisodeItem(
                episodeId = episode.id,
                title = episode.title,
                podcastTitle = podcast.title,
                artworkUrl = artwork,
                publicationDate = episode.publicationDate,
                durationSeconds = episode.durationSeconds,
                played = episode.played,
                downloadPath = episode.downloadPath,
                playbackPosition = episode.playbackPosition,
            )
        }

        val inProgress = items.filter { it.playbackPosition > 0L && !it.played }

        HomeUiState(
            recentEpisodes = items,
            inProgressEpisodes = inProgress,
            isEmpty = podcasts.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun downloadEpisode(episodeId: Long) {
        viewModelScope.launch {
            if (downloadManager.isDownloadBlockedByWifiSetting()) {
                _showMobileDataWarning.value = true
                return@launch
            }
            val job = launch {
                downloadManager.downloadEpisode(episodeId)
            }
            downloadManager.registerDownloadJob(episodeId, job)
        }
    }

    fun dismissMobileDataWarning() {
        _showMobileDataWarning.value = false
    }

    fun deleteDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
        }
    }

    fun playEpisode(episodeId: Long) {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            val podcast = podcastDao.getByIdOnce(episode.podcastId)

            val artworkUrl = episode.artworkUrl.ifBlank { podcast?.artworkUrl ?: "" }

            playbackController.play(
                episodeId = episode.id,
                audioUrl = episode.downloadPath.ifBlank { episode.audioUrl },
                title = episode.title,
                podcastTitle = podcast?.title ?: "",
                artworkUrl = artworkUrl,
                startPosition = episode.playbackPosition,
                podcastId = episode.podcastId,
            )
        }
    }
}
