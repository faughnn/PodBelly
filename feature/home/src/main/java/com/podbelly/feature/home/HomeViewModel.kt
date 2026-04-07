package com.podbelly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.common.DownloadErrorEvent
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    private val queueDao: QueueDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress
    val downloadErrors: SharedFlow<DownloadErrorEvent> = downloadManager.downloadErrors

    val queueEnabled: StateFlow<Boolean> = preferencesManager.queueEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _showMobileDataWarning = MutableStateFlow(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        episodeDao.getRecentEpisodes(50),
        episodeDao.getInProgressEpisodes(),
        podcastDao.getAll(),
    ) { episodes, inProgressEpisodes, podcasts ->
        val podcastMap: Map<Long, PodcastEntity> = podcasts.associateBy { it.id }

        fun toHomeItem(episode: EpisodeEntity): HomeEpisodeItem? {
            val podcast = podcastMap[episode.podcastId] ?: return null
            val artwork = episode.artworkUrl.ifBlank { podcast.artworkUrl }
            return HomeEpisodeItem(
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

        val items = episodes.mapNotNull { toHomeItem(it) }
        val inProgress = inProgressEpisodes.mapNotNull { toHomeItem(it) }

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
            downloadManager.enqueueDownload(episodeId)
        }
    }

    fun dismissMobileDataWarning() {
        _showMobileDataWarning.value = false
    }

    fun cancelDownload(episodeId: Long) {
        downloadManager.cancelDownload(episodeId)
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

    fun addToQueueNext(episodeId: Long) {
        viewModelScope.launch {
            if (queueDao.isInQueue(episodeId)) return@launch
            val items = queueDao.getQueueOnce()
            val shifted = items.map { it.queueItem.copy(position = it.queueItem.position + 1) }
            queueDao.updatePositions(shifted)
            queueDao.addToQueue(QueueItemEntity(episodeId = episodeId, position = 0, addedAt = System.currentTimeMillis()))
        }
    }

    fun addToQueueLast(episodeId: Long) {
        viewModelScope.launch {
            if (queueDao.isInQueue(episodeId)) return@launch
            val maxPos = queueDao.getMaxPosition() ?: -1
            queueDao.addToQueue(QueueItemEntity(episodeId = episodeId, position = maxPos + 1, addedAt = System.currentTimeMillis()))
        }
    }
}
