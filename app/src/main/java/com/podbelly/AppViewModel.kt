package com.podbelly

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.withRefreshedMetadata
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.ui.WhatsNew
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val application: Application,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val searchRepository: PodcastSearchRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val queueEnabled: StateFlow<Boolean> = preferencesManager.queueEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var lastRefreshTime = 0L

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // replay = 1 so the startup banner's collector (composed ~1s later, after the
    // splash) still receives the result even if the refresh finished first.
    private val _refreshResult = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    val refreshResult: SharedFlow<Int> = _refreshResult.asSharedFlow()

    private val _showWhatsNew = MutableStateFlow<List<String>?>(null)
    val showWhatsNew: StateFlow<List<String>?> = _showWhatsNew.asStateFlow()

    init {
        refreshFeeds()
        checkWhatsNew()
    }

    private fun checkWhatsNew() {
        viewModelScope.launch {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            val lastSeen = preferencesManager.getLastSeenVersionCode()

            when {
                lastSeen == 0 -> {
                    // Fresh install — don't show dialog, just record current version
                    preferencesManager.setLastSeenVersionCode(currentVersionCode)
                }
                lastSeen < currentVersionCode -> {
                    // Upgrade — collect all changes since last seen version
                    val changes = WhatsNew.changelog
                        .filterKeys { it in (lastSeen + 1)..currentVersionCode }
                        .toSortedMap(compareByDescending { it })
                        .values
                        .flatten()
                    if (changes.isNotEmpty()) {
                        _showWhatsNew.value = changes
                    } else {
                        preferencesManager.setLastSeenVersionCode(currentVersionCode)
                    }
                }
                // Already up to date — do nothing
            }
        }
    }

    fun dismissWhatsNew() {
        viewModelScope.launch {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            preferencesManager.setLastSeenVersionCode(PackageInfoCompat.getLongVersionCode(packageInfo).toInt())
            _showWhatsNew.value = null
        }
    }

    fun refreshIfStale() {
        viewModelScope.launch {
            // Honor the user's configured refresh interval. 0 means "Manual only",
            // in which case the foreground refresh should not fire automatically.
            val intervalMinutes = preferencesManager.feedRefreshIntervalMinutes.first()
            if (intervalMinutes <= 0) return@launch

            val elapsed = System.currentTimeMillis() - lastRefreshTime
            if (elapsed >= intervalMinutes * 60_000L) {
                refreshFeeds()
            }
        }
    }

    fun refreshFeeds() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true

        viewModelScope.launch {
            var newEpisodeCount = 0
            try {
                val podcasts: List<PodcastEntity> = podcastDao.getAll().first()
                val semaphore = Semaphore(5)
                val insertCounts = java.util.concurrent.atomic.AtomicInteger(0)
                podcasts.map { podcast ->
                    launch {
                        semaphore.withPermit {
                            try {
                                val rssFeed = searchRepository.fetchFeed(podcast.feedUrl)

                                // Insert new episodes; refresh feed-derived fields on
                                // existing ones so publisher corrections propagate.
                                val newEpisodes = mutableListOf<EpisodeEntity>()
                                for (rssEpisode in rssFeed.episodes) {
                                    val existing = episodeDao.getByPodcastAndGuid(podcast.id, rssEpisode.guid)
                                    if (existing == null) {
                                        newEpisodes.add(
                                            EpisodeEntity(
                                                podcastId = podcast.id,
                                                guid = rssEpisode.guid,
                                                title = rssEpisode.title,
                                                description = rssEpisode.description,
                                                audioUrl = rssEpisode.audioUrl,
                                                publicationDate = rssEpisode.publishedAt,
                                                durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                                artworkUrl = rssEpisode.artworkUrl ?: "",
                                                fileSize = rssEpisode.fileSize,
                                                addedAt = System.currentTimeMillis()
                                            )
                                        )
                                    } else {
                                        episodeDao.updateFeedFields(
                                            podcastId = podcast.id,
                                            guid = rssEpisode.guid,
                                            title = rssEpisode.title,
                                            description = rssEpisode.description,
                                            audioUrl = rssEpisode.audioUrl,
                                            publicationDate = rssEpisode.publishedAt,
                                            durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                            artworkUrl = rssEpisode.artworkUrl ?: "",
                                            fileSize = rssEpisode.fileSize,
                                        )
                                    }
                                }

                                if (newEpisodes.isNotEmpty()) {
                                    episodeDao.insertAll(newEpisodes)
                                    insertCounts.addAndGet(newEpisodes.size)
                                }

                                podcastDao.update(
                                    podcast.withRefreshedMetadata(
                                        title = rssFeed.title,
                                        author = rssFeed.author,
                                        description = rssFeed.description,
                                        artworkUrl = rssFeed.artworkUrl,
                                        link = rssFeed.link,
                                    ).copy(lastRefreshedAt = System.currentTimeMillis())
                                )
                            } catch (_: Exception) {
                                // Skip this feed and continue with the next one.
                            }
                        }
                    }
                }.forEach { it.join() }

                newEpisodeCount = insertCounts.get()
            } finally {
                _isRefreshing.value = false
                _refreshResult.tryEmit(newEpisodeCount)
                lastRefreshTime = System.currentTimeMillis()
            }
        }
    }
}
