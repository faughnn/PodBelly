package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val isInQueue: Boolean = false,
    val audioUrl: String = "",
    val downloadPath: String = "",
)

@HiltViewModel
class EpisodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val queueDao: QueueDao,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private val episodeId: Long = checkNotNull(savedStateHandle["episodeId"])

    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress

    val uiState: StateFlow<EpisodeDetailUiState> = combine(
        episodeDao.getById(episodeId),
        queueDao.getAll(),
    ) { episode, queueItems ->
        if (episode == null) return@combine EpisodeDetailUiState()

        val podcast = podcastDao.getByIdOnce(episode.podcastId)
        val queueEpisodeIds = queueItems.map { it.episodeId }.toSet()

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
            isInQueue = queueEpisodeIds.contains(episode.id),
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
            )
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            if (episode.played) {
                episodeDao.markAsUnplayed(episodeId)
            } else {
                episodeDao.markAsPlayed(episodeId)
            }
        }
    }

    fun addToQueue() {
        viewModelScope.launch {
            val maxPosition = queueDao.getMaxPosition() ?: -1
            queueDao.addToQueue(
                QueueItemEntity(
                    episodeId = episodeId,
                    position = maxPosition + 1,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun removeFromQueue() {
        viewModelScope.launch {
            queueDao.removeFromQueue(episodeId)
        }
    }

    fun downloadEpisode() {
        val job = viewModelScope.launch {
            downloadManager.downloadEpisode(episodeId)
        }
        downloadManager.registerDownloadJob(episodeId, job)
    }

    fun deleteDownload() {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
        }
    }
}
