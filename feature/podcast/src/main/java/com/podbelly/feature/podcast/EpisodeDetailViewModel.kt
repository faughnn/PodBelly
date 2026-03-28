package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.DownloadErrorEvent
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpisodeDetailUiState(
    val episodeId: Long = 0L,
    val title: String = "",
    val description: String = "",
    val podcastTitle: String = "",
    val podcastId: Long = 0L,
    val artworkUrl: String = "",
    val publicationDate: Long = 0L,
    val durationSeconds: Int = 0,
    val playbackPosition: Long = 0L,
    val played: Boolean = false,
    val isDownloaded: Boolean = false,
    val audioUrl: String = "",
    val downloadPath: String = "",
)

@HiltViewModel
class EpisodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
    private val queueDao: QueueDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val episodeId: Long = checkNotNull(savedStateHandle["episodeId"])

    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress
    val downloadErrors: SharedFlow<DownloadErrorEvent> = downloadManager.downloadErrors

    private val _showMobileDataWarning = MutableStateFlow(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    val queueEnabled: StateFlow<Boolean> = preferencesManager.queueEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isInQueue: StateFlow<Boolean> = queueDao.isInQueueFlow(episodeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<EpisodeDetailUiState> = episodeDao.getById(episodeId)
        .map { episode ->
        if (episode == null) return@map EpisodeDetailUiState()

        val podcast = podcastDao.getByIdOnce(episode.podcastId)

        EpisodeDetailUiState(
            episodeId = episode.id,
            title = episode.title,
            description = episode.description,
            podcastTitle = podcast?.title ?: "",
            podcastId = episode.podcastId,
            artworkUrl = episode.artworkUrl.ifEmpty { podcast?.artworkUrl ?: "" },
            publicationDate = episode.publicationDate,
            durationSeconds = episode.durationSeconds,
            playbackPosition = episode.playbackPosition,
            played = episode.played,
            isDownloaded = episode.downloadPath.isNotEmpty(),
            audioUrl = episode.audioUrl,
            downloadPath = episode.downloadPath,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EpisodeDetailUiState(),
    )

    fun playEpisode() {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            val podcast = podcastDao.getByIdOnce(episode.podcastId)

            playbackController.play(
                episodeId = episodeId,
                audioUrl = episode.downloadPath.ifEmpty { episode.audioUrl },
                title = episode.title,
                podcastTitle = podcast?.title ?: "",
                artworkUrl = episode.artworkUrl.ifEmpty { podcast?.artworkUrl ?: "" },
                startPosition = episode.playbackPosition,
                podcastId = episode.podcastId,
            )
        }
    }

    fun downloadEpisode() {
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

    fun deleteDownload() {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
        }
    }

    fun addToQueueNext() {
        viewModelScope.launch {
            if (queueDao.isInQueue(episodeId)) return@launch
            val items = queueDao.getQueueOnce()
            val shifted = items.map { it.queueItem.copy(position = it.queueItem.position + 1) }
            queueDao.updatePositions(shifted)
            queueDao.addToQueue(QueueItemEntity(episodeId = episodeId, position = 0, addedAt = System.currentTimeMillis()))
        }
    }

    fun addToQueueLast() {
        viewModelScope.launch {
            if (queueDao.isInQueue(episodeId)) return@launch
            val maxPos = queueDao.getMaxPosition() ?: -1
            queueDao.addToQueue(QueueItemEntity(episodeId = episodeId, position = maxPos + 1, addedAt = System.currentTimeMillis()))
        }
    }

    fun removeFromQueue() {
        viewModelScope.launch {
            queueDao.removeFromQueue(episodeId)
        }
    }
}
