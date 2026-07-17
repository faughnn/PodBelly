package com.podbelly.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.withRefreshedMetadata
import com.podbelly.core.common.DownloadErrorEvent
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val episodeCount: Int,
    val notifyNewEpisodes: Boolean = true,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
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
    private val playbackController: PlaybackController,
    private val searchRepository: PodcastSearchRepository,
    private val downloadManager: DownloadManager,
    private val queueDao: QueueDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    /** Download progress map exposed for the UI (episodeId -> 0.0..1.0). */
    val downloadProgress: StateFlow<Map<Long, Float>> = downloadManager.downloadProgress
    val downloadErrors: SharedFlow<DownloadErrorEvent> = downloadManager.downloadErrors

    val queueEnabled: StateFlow<Boolean> = preferencesManager.queueEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _showMobileDataWarning = MutableStateFlow(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    private val podcastId: Long = checkNotNull(savedStateHandle["podcastId"])

    private val _filter = MutableStateFlow(EpisodeFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<PodcastDetailUiState> = combine(
        podcastDao.getById(podcastId),
        episodeDao.getByPodcastId(podcastId),
        _filter,
        _isRefreshing
    ) { podcastEntity, episodes, filter, isRefreshing ->
        val podcastUi = podcastEntity?.let {
            PodcastUiModel(
                id = it.id,
                title = it.title,
                author = it.author,
                description = it.description,
                artworkUrl = it.artworkUrl,
                episodeCount = it.episodeCount,
                notifyNewEpisodes = it.notifyNewEpisodes,
                skipIntroSeconds = it.skipIntroSeconds,
                skipOutroSeconds = it.skipOutroSeconds,
            )
        }

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

                val newEpisodes = mutableListOf<EpisodeEntity>()
                for (rssEpisode in rssFeed.episodes) {
                    val existing = episodeDao.getByPodcastAndGuid(podcastId, rssEpisode.guid)
                    if (existing == null) {
                        newEpisodes.add(
                            EpisodeEntity(
                                podcastId = podcastId,
                                guid = rssEpisode.guid,
                                title = rssEpisode.title,
                                description = rssEpisode.description,
                                audioUrl = rssEpisode.audioUrl,
                                publicationDate = rssEpisode.publishedAt,
                                durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                artworkUrl = rssEpisode.artworkUrl ?: "",
                                addedAt = System.currentTimeMillis(),
                                transcriptUrl = rssEpisode.transcriptUrl ?: "",
                                transcriptType = rssEpisode.transcriptType ?: "",
                            )
                        )
                    } else {
                        // Refresh feed-derived fields so publisher corrections propagate.
                        episodeDao.updateFeedFields(
                            podcastId = podcastId,
                            guid = rssEpisode.guid,
                            title = rssEpisode.title,
                            description = rssEpisode.description,
                            audioUrl = rssEpisode.audioUrl,
                            publicationDate = rssEpisode.publishedAt,
                            durationSeconds = (rssEpisode.duration / 1000).toInt(),
                            artworkUrl = rssEpisode.artworkUrl ?: "",
                            fileSize = rssEpisode.fileSize,
                            transcriptUrl = rssEpisode.transcriptUrl ?: "",
                            transcriptType = rssEpisode.transcriptType ?: "",
                        )
                    }
                }

                if (newEpisodes.isNotEmpty()) {
                    episodeDao.insertAll(newEpisodes)
                }

                podcastDao.update(
                    podcastEntity.withRefreshedMetadata(
                        title = rssFeed.title,
                        author = rssFeed.author,
                        description = rssFeed.description,
                        artworkUrl = rssFeed.artworkUrl,
                        link = rssFeed.link,
                    ).copy(
                        lastRefreshedAt = System.currentTimeMillis(),
                        // Derive from actual stored rows rather than an additive guess.
                        episodeCount = episodeDao.countByPodcastId(podcastId),
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
                podcastId = podcast.id,
            )
        }
    }

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

    fun setFilter(filter: EpisodeFilter) {
        _filter.value = filter
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            val current = uiState.value.podcast?.notifyNewEpisodes ?: true
            podcastDao.setNotifyNewEpisodes(podcastId, !current)
        }
    }

    fun unsubscribe() {
        viewModelScope.launch {
            podcastDao.unsubscribe(podcastId)
        }
    }

    /** Persists the per-podcast intro auto-skip (seconds; 0 disables it). */
    fun setSkipIntroSeconds(seconds: Int) {
        viewModelScope.launch {
            podcastDao.updateSkipIntroSeconds(podcastId, seconds.coerceAtLeast(0))
            playbackController.refreshSkipSettings()
        }
    }

    /** Persists the per-podcast outro auto-skip (seconds; 0 disables it). */
    fun setSkipOutroSeconds(seconds: Int) {
        viewModelScope.launch {
            podcastDao.updateSkipOutroSeconds(podcastId, seconds.coerceAtLeast(0))
            // If this podcast is playing, the outro applies to the current episode too.
            playbackController.refreshSkipSettings()
        }
    }

    fun addToQueueNext(episodeId: Long) {
        viewModelScope.launch {
            // addToFront shifts + inserts inside one @Transaction so an interruption or
            // concurrent mutation can't leave the queue shifted with no head item.
            queueDao.addToFront(episodeId, System.currentTimeMillis())
        }
    }

    fun addToQueueLast(episodeId: Long) {
        viewModelScope.launch {
            // Read-max + insert atomically (single @Transaction) so two concurrent
            // enqueues can't both land at the same position.
            queueDao.addToEnd(episodeId, System.currentTimeMillis())
        }
    }
}
