package com.podbelly

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.AutoDownloadCandidate
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.common.RefreshProgress
import com.podbelly.core.common.shouldAutoDownload
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.hasSameFeedFields
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
import kotlinx.coroutines.flow.update
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
    private val listeningSessionDao: ListeningSessionDao,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private var lastRefreshTime = 0L

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Non-null while a refresh is running: how many feeds have finished so far. */
    private val _refreshProgress = MutableStateFlow<RefreshProgress?>(null)
    val refreshProgress: StateFlow<RefreshProgress?> = _refreshProgress.asStateFlow()

    // replay = 1 so the startup banner's collector (composed ~1s later, after the
    // splash) still receives the result even if the refresh finished first.
    private val _refreshResult = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    val refreshResult: SharedFlow<Int> = _refreshResult.asSharedFlow()

    private val _showWhatsNew = MutableStateFlow<List<String>?>(null)
    val showWhatsNew: StateFlow<List<String>?> = _showWhatsNew.asStateFlow()

    init {
        // Refresh on launch only when the configured interval has elapsed since the
        // last successful refresh. A cold open shortly after a background refresh
        // used to kick off a full all-feeds refresh anyway, hammering the network
        // and database exactly while the first screen is drawing.
        refreshIfStale()
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

            // lastRefreshTime only survives within this process; the persisted
            // timestamp covers cold starts and refreshes done by the background
            // worker, so a fresh launch doesn't always look stale.
            val lastRefresh = maxOf(
                lastRefreshTime,
                preferencesManager.lastFeedRefreshAt.first(),
            )
            if (System.currentTimeMillis() - lastRefresh >= intervalMinutes * 60_000L) {
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
                _refreshProgress.value = RefreshProgress(completed = 0, total = podcasts.size)
                // Most of a feed fetch is server latency, not bandwidth, so wide
                // parallelism nearly divides refresh time by the concurrency. 32 is
                // safe now that feeds stream straight into the parser (memory per
                // in-flight feed is just its parsed episodes, bounded by the 10MB
                // read cap) — buffering whole documents was the old reason for 5.
                // Smart auto-download: new arrivals from engaged shows (or per-show
                // Always overrides) start downloading as soon as the refresh finds
                // them, unless the Wi-Fi or charging gate blocks auto-downloads.
                val smartEnabled = preferencesManager.smartAutoDownload.first()
                val autoDownloadBlocked = downloadManager.isDownloadBlockedByWifiSetting() ||
                    downloadManager.isAutoDownloadBlockedByChargingSetting()
                val keepPerShow = preferencesManager.smartAutoDownloadKeepPerShow.first()
                val engagedPodcastIds = if (smartEnabled && !autoDownloadBlocked) {
                    val windowDays = preferencesManager.smartAutoDownloadWindowDays.first()
                    listeningSessionDao
                        .getEngagedPodcastIds(System.currentTimeMillis() - windowDays * 86_400_000L)
                        .toSet()
                } else emptySet()

                val semaphore = Semaphore(32)
                val insertCounts = java.util.concurrent.atomic.AtomicInteger(0)
                val successCounts = java.util.concurrent.atomic.AtomicInteger(0)
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
                                                addedAt = System.currentTimeMillis(),
                                                transcriptUrl = rssEpisode.transcriptUrl ?: "",
                                                transcriptType = rssEpisode.transcriptType ?: "",
                                            )
                                        )
                                    } else if (!existing.hasSameFeedFields(
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
                                    ) {
                                        // Only write when a feed field actually changed;
                                        // every write invalidates the home screen's Room
                                        // flows, and 100+ feeds of no-op updates made the
                                        // UI churn for the whole refresh.
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
                                            transcriptUrl = rssEpisode.transcriptUrl ?: "",
                                            transcriptType = rssEpisode.transcriptType ?: "",
                                        )
                                    }
                                }

                                if (newEpisodes.isNotEmpty()) {
                                    val insertedIds = episodeDao.insertAll(newEpisodes)
                                    insertCounts.addAndGet(newEpisodes.size)
                                    val autoDownload = !autoDownloadBlocked && shouldAutoDownload(
                                        autoDownloadMode = podcast.autoDownloadMode,
                                        smartEnabled = smartEnabled,
                                        engaged = podcast.id in engagedPodcastIds,
                                    )
                                    if (autoDownload) {
                                        downloadManager.autoDownloadNewEpisodes(
                                            podcastId = podcast.id,
                                            inserted = newEpisodes.zip(insertedIds) { ep, id ->
                                                AutoDownloadCandidate(id, ep.publicationDate)
                                            },
                                            keepPerShow = keepPerShow,
                                        )
                                    }
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
                                successCounts.incrementAndGet()
                            } catch (_: Exception) {
                                // Skip this feed and continue with the next one.
                            } finally {
                                // Count failures too — progress tracks completions.
                                _refreshProgress.update { it?.copy(completed = it.completed + 1) }
                            }
                        }
                    }
                }.forEach { it.join() }

                newEpisodeCount = insertCounts.get()
                // Record the refresh only if it actually reached at least one feed,
                // so an offline attempt doesn't claim the feed is up to date.
                if (successCounts.get() > 0) {
                    preferencesManager.setLastFeedRefreshAt(System.currentTimeMillis())
                }
            } finally {
                _isRefreshing.value = false
                _refreshProgress.value = null
                _refreshResult.tryEmit(newEpisodeCount)
                lastRefreshTime = System.currentTimeMillis()
            }
        }
    }
}
