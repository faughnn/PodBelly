package com.podbelly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    val newEpisodes: List<HomeEpisodeItem> = emptyList(),
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

    // The "New" section shows episodes discovered after the persisted cutoff. The
    // cutoff is snapshotted once per ViewModel so the section doesn't dissolve
    // while the user is looking at it (episodes arriving mid-visit, e.g. from
    // pull-to-refresh, still enter the section live because their addedAt is
    // above the snapshot). The persisted cutoff advances as soon as the section's
    // contents have been surfaced, so the next visit shows them back in their
    // normal chronological position.
    private var sessionNewCutoff: Long? = null
    private var persistedNewCutoff = 0L

    private val newEpisodesFlow = flow {
        val cutoff = sessionNewCutoff ?: preferencesManager.homeNewEpisodesCutoff.first().also {
            sessionNewCutoff = it
            persistedNewCutoff = it
        }
        emitAll(episodeDao.getEpisodesAddedSince(cutoff))
    }

    val uiState: StateFlow<HomeUiState> = combine(
        episodeDao.getRecentEpisodes(50),
        episodeDao.getInProgressEpisodes(),
        podcastDao.getAll(),
        newEpisodesFlow,
    ) { episodes, inProgressEpisodes, podcasts, newEpisodes ->
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
        val newItems = newEpisodes.mapNotNull { toHomeItem(it) }

        // These episodes are about to be on screen — advance the persisted cutoff
        // so they leave the New section on the next visit.
        val maxAddedAt = newEpisodes.maxOfOrNull { it.addedAt } ?: 0L
        if (maxAddedAt > persistedNewCutoff) {
            persistedNewCutoff = maxAddedAt
            viewModelScope.launch {
                preferencesManager.setHomeNewEpisodesCutoff(maxAddedAt)
            }
        }

        val newIds = newItems.mapTo(HashSet()) { it.episodeId }

        HomeUiState(
            recentEpisodes = items.filterNot { it.episodeId in newIds },
            newEpisodes = newItems,
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
            // Shift + insert atomically so an interruption can't leave the queue
            // shifted with no item at the front.
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
