package com.podbelly.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.dao.QueueEpisode
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QueueItemUi(
    val queueId: Long,
    val episodeId: Long,
    val episodeTitle: String,
    val podcastTitle: String,
    val artworkUrl: String,
    val durationSeconds: Int,
    val position: Int
)

data class QueueUiState(
    val queueItems: List<QueueItemUi> = emptyList(),
    val isEmpty: Boolean = true,
    val nowPlayingEpisodeId: Long? = null
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val queueDao: QueueDao,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val playbackController: PlaybackController
) : ViewModel() {

    val uiState: StateFlow<QueueUiState> = combine(
        queueDao.getQueueWithEpisodes(),
        podcastDao.getAll(),
        playbackController.playbackState
    ) { queueEpisodes, podcasts, playbackState ->
        val podcastMap = podcasts.associateBy { it.id }

        val items = queueEpisodes.map { queueEpisode ->
            val episode = queueEpisode.episode
            val queueItem = queueEpisode.queueItem
            val podcast = podcastMap[episode.podcastId]

            QueueItemUi(
                queueId = queueItem.id,
                episodeId = episode.id,
                episodeTitle = episode.title,
                podcastTitle = podcast?.title.orEmpty(),
                artworkUrl = episode.artworkUrl.ifBlank { podcast?.artworkUrl.orEmpty() },
                durationSeconds = episode.durationSeconds,
                position = queueItem.position
            )
        }

        QueueUiState(
            queueItems = items,
            isEmpty = items.isEmpty(),
            nowPlayingEpisodeId = playbackState.episodeId.takeIf { it != 0L }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QueueUiState())

    fun playItem(episodeId: Long) {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            val podcast = podcastDao.getByIdOnce(episode.podcastId)
            val audioUrl = episode.downloadPath.ifBlank { episode.audioUrl }

            playbackController.play(
                episodeId = episode.id,
                audioUrl = audioUrl,
                title = episode.title,
                podcastTitle = podcast?.title.orEmpty(),
                artworkUrl = episode.artworkUrl.ifBlank { podcast?.artworkUrl.orEmpty() },
                startPosition = episode.playbackPosition,
            )
        }
    }

    fun removeItem(episodeId: Long) {
        viewModelScope.launch {
            queueDao.removeFromQueue(episodeId)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            queueDao.clearQueue()
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            // Snapshot the current queue items ordered by position
            val currentItems = queueDao.getQueueWithEpisodes().first()
            if (fromIndex !in currentItems.indices || toIndex !in currentItems.indices) return@launch

            val mutableList = currentItems.toMutableList()
            val movedItem = mutableList.removeAt(fromIndex)
            mutableList.add(toIndex, movedItem)

            // Rebuild position values for all affected items
            val updatedEntities = mutableList.mapIndexed { index, queueEpisode ->
                queueEpisode.queueItem.copy(position = index)
            }
            queueDao.updatePositions(updatedEntities)
        }
    }
}
