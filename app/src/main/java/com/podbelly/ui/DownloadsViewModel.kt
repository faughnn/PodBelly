package com.podbelly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.DownloadsSortOrder
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadedEpisodeItem(
    val episodeId: Long,
    val title: String,
    val podcastTitle: String,
    val artworkUrl: String,
    val publicationDate: Long,
    val durationSeconds: Int,
    val played: Boolean,
    val downloadPath: String,
    val downloadedAt: Long,
    val fileSize: Long,
)

data class DownloadsUiState(
    val episodes: List<DownloadedEpisodeItem> = emptyList(),
    val sortOrder: DownloadsSortOrder = DownloadsSortOrder.DATE_NEWEST,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress

    val uiState: StateFlow<DownloadsUiState> = combine(
        episodeDao.getDownloadedEpisodes(),
        podcastDao.getAll(),
        preferencesManager.downloadsSortOrder,
    ) { episodes, podcasts, sortOrder ->
        val podcastMap: Map<Long, PodcastEntity> = podcasts.associateBy { it.id }

        val items = episodes.mapNotNull { episode ->
            val podcast = podcastMap[episode.podcastId] ?: return@mapNotNull null
            val artwork = episode.artworkUrl.ifBlank { podcast.artworkUrl }
            DownloadedEpisodeItem(
                episodeId = episode.id,
                title = episode.title,
                podcastTitle = podcast.title,
                artworkUrl = artwork,
                publicationDate = episode.publicationDate,
                durationSeconds = episode.durationSeconds,
                played = episode.played,
                downloadPath = episode.downloadPath,
                downloadedAt = episode.downloadedAt,
                fileSize = episode.fileSize,
            )
        }

        val sorted = when (sortOrder) {
            DownloadsSortOrder.DATE_NEWEST -> items.sortedByDescending { it.downloadedAt }
            DownloadsSortOrder.DATE_OLDEST -> items.sortedBy { it.downloadedAt }
            DownloadsSortOrder.NAME_A_TO_Z -> items.sortedBy { it.title.lowercase() }
            DownloadsSortOrder.PODCAST_NAME -> items.sortedBy { it.podcastTitle.lowercase() }
            DownloadsSortOrder.FILE_SIZE -> items.sortedByDescending { it.fileSize }
        }

        DownloadsUiState(episodes = sorted, sortOrder = sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState(),
    )

    fun setSortOrder(sortOrder: DownloadsSortOrder) {
        viewModelScope.launch {
            preferencesManager.setDownloadsSortOrder(sortOrder)
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
            )
        }
    }

    fun deleteDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
        }
    }
}
