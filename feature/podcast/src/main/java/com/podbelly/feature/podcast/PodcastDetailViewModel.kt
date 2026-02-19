package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.QueueItemEntity
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EpisodeFilter {
    ALL, UNPLAYED, DOWNLOADED
}

data class PodcastUiModel(
    val id: Long,
    val title: String,
    val author: String,
    val description: String,
    val artworkUrl: String,
    val episodeCount: Int
)

data class EpisodeUiModel(
    val id: Long,
    val title: String,
    val description: String,
    val publicationDate: Long,
    val durationSeconds: Int,
    val played: Boolean,
    val playbackPosition: Long,
    val isDownloaded: Boolean,
    val isInQueue: Boolean
)

data class PodcastDetailUiState(
    val podcast: PodcastUiModel? = null,
    val episodes: List<EpisodeUiModel> = emptyList(),
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val queueDao: QueueDao,
    private val playbackController: PlaybackController,
    private val searchRepository: PodcastSearchRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    /** Download progress map exposed for the UI (episodeId -> 0.0..1.0). */
    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress

    private val podcastId: Long = checkNotNull(savedStateHandle["podcastId"])

    private val _filter = MutableStateFlow(EpisodeFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<PodcastDetailUiState> = combine(
        podcastDao.getById(podcastId),
        episodeDao.getByPodcastId(podcastId),
        queueDao.getAll(),
        _filter,
        _isRefreshing
    ) { values ->
        val podcastEntity = values[0] as? com.podbelly.core.database.entity.PodcastEntity
        @Suppress("UNCHECKED_CAST")
        val episodes = values[1] as List<EpisodeEntity>
        @Suppress("UNCHECKED_CAST")
        val queueItems = values[2] as List<QueueItemEntity>
        val filter = values[3] as EpisodeFilter
        val isRefreshing = values[4] as Boolean

        val podcastUi = podcastEntity?.let {
            PodcastUiModel(
                id = it.id,
                title = it.title,
                author = it.author,
                description = it.description,
                artworkUrl = it.artworkUrl,
                episodeCount = it.episodeCount
            )
        }

        val queueEpisodeIds = queueItems.map { it.episodeId }.toSet()

        val allEpisodeUiModels = episodes.map { entity ->
            EpisodeUiModel(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                publicationDate = entity.publicationDate,
                durationSeconds = entity.durationSeconds,
                played = entity.played,
                playbackPosition = entity.playbackPosition,
                isDownloaded = entity.downloadPath.isNotEmpty(),
                isInQueue = queueEpisodeIds.contains(entity.id)
            )
        }

        val filteredEpisodes = when (filter) {
            EpisodeFilter.ALL -> allEpisodeUiModels
            EpisodeFilter.UNPLAYED -> allEpisodeUiModels.filter { !it.played }
            EpisodeFilter.DOWNLOADED -> allEpisodeUiModels.filter { it.isDownloaded }
        }

        PodcastDetailUiState(
            podcast = podcastUi,
            episodes = filteredEpisodes,
            filter = filter,
            isRefreshing = isRefreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PodcastDetailUiState()
    )

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val podcastEntity = podcastDao.getById(podcastId).first() ?: return@launch
                val rssFeed = searchRepository.fetchFeed(podcastEntity.feedUrl)

                val newEpisodes = rssFeed.episodes.mapNotNull { rssEpisode ->
                    val existing = episodeDao.getByGuid(rssEpisode.guid)
                    if (existing == null) {
                        EpisodeEntity(
                            podcastId = podcastId,
                            guid = rssEpisode.guid,
                            title = rssEpisode.title,
                            description = rssEpisode.description,
                            audioUrl = rssEpisode.audioUrl,
                            publicationDate = rssEpisode.publishedAt,
                            durationSeconds = (rssEpisode.duration / 1000).toInt(),
                            artworkUrl = rssEpisode.artworkUrl ?: "",
                        )
                    } else {
                        null
                    }
                }

                if (newEpisodes.isNotEmpty()) {
                    episodeDao.insertAll(newEpisodes)
                }

                podcastDao.update(
                    podcastEntity.copy(
                        lastRefreshedAt = System.currentTimeMillis(),
                        episodeCount = podcastEntity.episodeCount + newEpisodes.size,
                    )
                )
            } catch (_: Exception) {
                // Refresh failed; episodes remain unchanged
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun playEpisode(episodeId: Long) {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            val podcast = uiState.value.podcast ?: return@launch

            playbackController.play(
                episodeId = episodeId,
                audioUrl = episode.downloadPath.ifEmpty { episode.audioUrl },
                title = episode.title,
                podcastTitle = podcast.title,
                artworkUrl = episode.artworkUrl.ifEmpty { podcast.artworkUrl },
                startPosition = episode.playbackPosition,
            )
        }
    }

    fun togglePlayed(episodeId: Long) {
        viewModelScope.launch {
            val episode = episodeDao.getByIdOnce(episodeId) ?: return@launch
            if (episode.played) {
                episodeDao.markAsUnplayed(episodeId)
            } else {
                episodeDao.markAsPlayed(episodeId)
            }
        }
    }

    fun addToQueue(episodeId: Long) {
        viewModelScope.launch {
            val maxPosition = queueDao.getMaxPosition() ?: -1
            queueDao.addToQueue(
                QueueItemEntity(
                    episodeId = episodeId,
                    position = maxPosition + 1,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeFromQueue(episodeId: Long) {
        viewModelScope.launch {
            queueDao.removeFromQueue(episodeId)
        }
    }

    fun downloadEpisode(episodeId: Long) {
        val job = viewModelScope.launch {
            downloadManager.downloadEpisode(episodeId)
        }
        downloadManager.registerDownloadJob(episodeId, job)
    }

    fun deleteDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
        }
    }

    fun setFilter(filter: EpisodeFilter) {
        _filter.value = filter
    }

    fun unsubscribe() {
        viewModelScope.launch {
            podcastDao.unsubscribe(podcastId)
        }
    }
}
