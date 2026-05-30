package com.podbelly.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.dao.QueueEpisode
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
                podcastId = episode.podcastId,
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

    fun moveUp(queueId: Long) = reorder(queueId, -1)

    fun moveDown(queueId: Long) = reorder(queueId, +1)

    /**
     * Moves the queue item identified by [queueId] by [delta] positions. Resolving the
     * item by its stable id (rather than a UI list index) means a queue mutation between
     * render and tap — e.g. the head episode finishing and being auto-removed — can no
     * longer cause the wrong row to be moved.
     */
    private fun reorder(queueId: Long, delta: Int) {
        viewModelScope.launch {
            // Resolve + renumber inside a single DB transaction (QueueDao.moveItem) so a
            // concurrent queue mutation can't interleave between the read and the write.
            queueDao.moveItem(queueId, delta)
        }
    }
}
