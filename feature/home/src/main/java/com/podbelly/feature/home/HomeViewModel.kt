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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
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

    /** When feeds last refreshed successfully (epoch ms, 0 = never). */
    val lastRefreshedAt: StateFlow<Long> = preferencesManager.lastFeedRefreshAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _showMobileDataWarning = MutableStateFlow(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    // The "New" section shows episodes discovered after the persisted cutoff. The
    // cutoff is snapshotted per *visit* so the section doesn't dissolve while the
    // user is looking at it (episodes arriving mid-visit, e.g. from
    // pull-to-refresh, still enter the section live because their addedAt is
    // above the snapshot). The persisted cutoff advances as soon as the section's
    // contents have been surfaced, so the next visit shows them back in their
    // normal chronological position.
    //
    // A visit ends when Home hasn't been on screen for NEW_VISIT_AFTER_MS. The
    // ViewModel can survive the app sitting in the background for hours (the
    // process often outlives "closing" the app), so a per-ViewModel snapshot
    // alone would keep showing a stale New section on reopen. Collection stops
    // while Home is off screen (collectAsStateWithLifecycle + WhileSubscribed),
    // so the gap between collections measures exactly how long the user was away.
    //
    // On top of the soft visit cutoff sits a *hard* cutoff (hardNewCutoff,
    // persisted as homeNewDismissedAt): the moment the user last tapped the New
    // header to dismiss the section. The recency floor never overrides it — an
    // explicit tap is unambiguous "seen", unlike the glimpse heuristic the
    // floor protects against.
    private var sessionNewCutoff: Long? = null
    private var persistedNewCutoff = 0L
    private var homeLeftAt = 0L
    private val hardNewCutoff = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val newEpisodesFlow = flow {
        val now = System.currentTimeMillis()
        val cached = sessionNewCutoff?.takeIf { now - homeLeftAt < NEW_VISIT_AFTER_MS }
        val soft = cached ?: preferencesManager.homeNewEpisodesCutoff.first().also {
            sessionNewCutoff = it
            persistedNewCutoff = maxOf(persistedNewCutoff, it)
        }
        if (hardNewCutoff.value == null) {
            hardNewCutoff.value = preferencesManager.homeNewDismissedAt.first()
        }
        // Recency floor: anything discovered in the last 30 minutes counts as new
        // even if the persisted cutoff already advanced past it. A refresh inserts
        // feeds one at a time, so a session that dies mid-refresh (process killed
        // right after pull-to-refresh) would otherwise mark the few feeds that
        // landed as "seen" after barely a glance, stranding a just-published
        // episode under Earlier while the rest of its batch shows as New on the
        // next launch. The hard dismissal cutoff wins over the floor, so tapping
        // the header clears the section immediately and it stays cleared.
        emitAll(
            hardNewCutoff.filterNotNull().flatMapLatest { hard ->
                episodeDao.getEpisodesAddedSince(maxOf(hard, minOf(soft, now - RECENT_GRACE_MS)))
            }
        )
    }.onCompletion {
        homeLeftAt = System.currentTimeMillis()
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

    // Explicit dismissal is a hard cutoff the recency floor doesn't override.
    // Also advances the soft cutoff so the section stays gone on the next visit.
    fun dismissNewSection() {
        val now = System.currentTimeMillis()
        hardNewCutoff.value = now
        persistedNewCutoff = maxOf(persistedNewCutoff, now)
        viewModelScope.launch {
            preferencesManager.setHomeNewDismissedAt(now)
            preferencesManager.setHomeNewEpisodesCutoff(now)
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

    private companion object {
        const val RECENT_GRACE_MS = 30 * 60 * 1000L

        /** Being away from Home longer than this starts a fresh visit. */
        const val NEW_VISIT_AFTER_MS = 30 * 60 * 1000L
    }
}
