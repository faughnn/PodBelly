package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.CrashLogStore
import com.podbelly.core.common.CrashReporter
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.common.VisualizerBackgroundMode
import com.podbelly.core.common.VisualizerStyle
import com.podbelly.core.common.di.IoDispatcher
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.opml.OpmlFeed
import com.podbelly.core.network.opml.OpmlHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportResult(
    val imported: Int,
    val total: Int,
    val skipped: Int,
    val failed: List<String>,
)

data class SettingsUiState(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val feedRefreshIntervalMinutes: Int = 60,
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadEpisodeCount: Int = 3,
    val autoDeletePlayedAfterDays: Int = 0,
    val smartAutoDownload: Boolean = false,
    val smartAutoDownloadWindowDays: Int = 30,
    val smartAutoDownloadKeepPerShow: Int = 0,
    val smartAutoDownloadChargingOnly: Boolean = false,
    val downloadOnWifiOnly: Boolean = true,
    val skipSilence: Boolean = false,
    val skipAdChapters: Boolean = false,
    val volumeBoost: Boolean = false,
    // switch doesn't render OFF then snap ON for never-configured users.
    val totalDownloadedBytes: Long = 0L,
    val importExportMessage: String? = null,
    val importResult: ImportResult? = null,
    val visualizerEnabled: Boolean = false,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val visualizerBackground: VisualizerBackgroundMode = VisualizerBackgroundMode.REPLACE,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val searchRepository: PodcastSearchRepository,
    private val downloadManager: DownloadManager,
    private val crashReporter: CrashReporter,
    private val crashLogStore: CrashLogStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _importExportMessage = MutableStateFlow<String?>(null)
    private val _importResult = MutableStateFlow<ImportResult?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.appTheme,
        preferencesManager.feedRefreshIntervalMinutes,
        preferencesManager.autoDownloadEnabled,
        preferencesManager.autoDownloadEpisodeCount,
        preferencesManager.autoDeletePlayedAfterDays,
    ) { appTheme, refreshInterval, autoDownload, autoDownloadCount, autoDelete ->
        PartialState(
            appTheme = appTheme,
            feedRefreshIntervalMinutes = refreshInterval,
            autoDownloadEnabled = autoDownload,
            autoDownloadEpisodeCount = autoDownloadCount,
            autoDeletePlayedAfterDays = autoDelete,
        )
    }.combine(
        combine(
            preferencesManager.downloadOnWifiOnly,
            preferencesManager.skipSilence,
            preferencesManager.volumeBoost,
            _importExportMessage,
            preferencesManager.smartAutoDownload,
            preferencesManager.skipAdChapters,
            preferencesManager.smartAutoDownloadWindowDays,
            preferencesManager.smartAutoDownloadKeepPerShow,
            preferencesManager.smartAutoDownloadChargingOnly,
            preferencesManager.visualizerEnabled,
            preferencesManager.visualizerStyle,
            preferencesManager.visualizerBackground,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            SecondaryState(
                downloadOnWifiOnly = values[0] as Boolean,
                skipSilence = values[1] as Boolean,
                volumeBoost = values[2] as Boolean,
                importExportMessage = values[3] as? String,
                smartAutoDownload = values[4] as Boolean,
                skipAdChapters = values[5] as Boolean,
                smartAutoDownloadWindowDays = values[6] as Int,
                smartAutoDownloadKeepPerShow = values[7] as Int,
                smartAutoDownloadChargingOnly = values[8] as Boolean,
                visualizerEnabled = values[9] as Boolean,
                visualizerStyle = values[10] as VisualizerStyle,
                visualizerBackground = values[11] as VisualizerBackgroundMode,
            )
        }
    ) { partial, secondary ->
        SettingsUiState(
            appTheme = partial.appTheme,
            feedRefreshIntervalMinutes = partial.feedRefreshIntervalMinutes,
            autoDownloadEnabled = partial.autoDownloadEnabled,
            autoDownloadEpisodeCount = partial.autoDownloadEpisodeCount,
            autoDeletePlayedAfterDays = partial.autoDeletePlayedAfterDays,
            smartAutoDownload = secondary.smartAutoDownload,
            smartAutoDownloadWindowDays = secondary.smartAutoDownloadWindowDays,
            smartAutoDownloadKeepPerShow = secondary.smartAutoDownloadKeepPerShow,
            smartAutoDownloadChargingOnly = secondary.smartAutoDownloadChargingOnly,
            downloadOnWifiOnly = secondary.downloadOnWifiOnly,
            skipSilence = secondary.skipSilence,
            skipAdChapters = secondary.skipAdChapters,
            volumeBoost = secondary.volumeBoost,
            importExportMessage = secondary.importExportMessage,
            visualizerEnabled = secondary.visualizerEnabled,
            visualizerStyle = secondary.visualizerStyle,
            visualizerBackground = secondary.visualizerBackground,
        )
    }.combine(_importResult) { state, importResult ->
        state.copy(importResult = importResult)
    }.combine(episodeDao.getTotalDownloadedBytes()) { state, totalBytes ->
        state.copy(totalDownloadedBytes = totalBytes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setAppTheme(mode: AppTheme) {
        viewModelScope.launch { preferencesManager.setAppTheme(mode) }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSkipSilence(enabled) }
    }

    fun setSkipAdChapters(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSkipAdChapters(enabled) }
    }

    fun setVolumeBoost(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setVolumeBoost(enabled) }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setVisualizerEnabled(enabled) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch { preferencesManager.setVisualizerStyle(style) }
    }

    fun setVisualizerBackground(mode: VisualizerBackgroundMode) {
        viewModelScope.launch { preferencesManager.setVisualizerBackground(mode) }
    }

    fun setAutoDownload(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoDownloadEnabled(enabled) }
    }

    fun setAutoDownloadEpisodeCount(count: Int) {
        viewModelScope.launch { preferencesManager.setAutoDownloadEpisodeCount(count) }
    }

    fun setDownloadOnWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { preferencesManager.setDownloadOnWifiOnly(wifiOnly) }
    }

    fun setAutoDeletePlayedAfterDays(days: Int) {
        viewModelScope.launch { preferencesManager.setAutoDeletePlayedAfterDays(days) }
    }

    fun setSmartAutoDownload(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSmartAutoDownload(enabled) }
    }

    fun setSmartAutoDownloadWindowDays(days: Int) {
        viewModelScope.launch { preferencesManager.setSmartAutoDownloadWindowDays(days) }
    }

    fun setSmartAutoDownloadKeepPerShow(count: Int) {
        viewModelScope.launch { preferencesManager.setSmartAutoDownloadKeepPerShow(count) }
    }

    fun setSmartAutoDownloadChargingOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSmartAutoDownloadChargingOnly(enabled) }
    }

    fun setFeedRefreshInterval(minutes: Int) {
        viewModelScope.launch { preferencesManager.setFeedRefreshIntervalMinutes(minutes) }
    }

    fun exportOpml(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val podcasts = podcastDao.getAll().first()
                val feeds = podcasts.map { podcast ->
                    OpmlFeed(
                        title = podcast.title,
                        feedUrl = podcast.feedUrl,
                    )
                }
                val xml = withContext(ioDispatcher) { OpmlHandler.generateOpml(feeds) }
                onResult(xml)
                _importExportMessage.value = "Exported ${feeds.size} subscription(s)"
            } catch (e: Exception) {
                _importExportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importOpml(xml: String) {
        viewModelScope.launch {
            try {
                val entries = withContext(ioDispatcher) { OpmlHandler.parseOpml(xml) }
                var imported = 0
                var skipped = 0
                val failed = mutableListOf<String>()

                for (entry in entries) {
                    try {
                        val normalizedUrl = normalizeFeedUrl(entry.feedUrl)
                        val existing = podcastDao.getByFeedUrl(normalizedUrl)
                            ?: podcastDao.getByFeedUrl(entry.feedUrl)
                        if (existing != null) {
                            skipped++
                            continue
                        }

                        // Fetch the same URL we persist, so a feed reachable only over
                        // the normalized (https) scheme doesn't import then fail to refresh.
                        val rssFeed = searchRepository.fetchFeed(normalizedUrl)
                        val now = System.currentTimeMillis()

                        val podcast = PodcastEntity(
                            feedUrl = normalizedUrl,
                            title = rssFeed.title.ifBlank { entry.title },
                            author = rssFeed.author,
                            description = rssFeed.description,
                            artworkUrl = rssFeed.artworkUrl,
                            link = rssFeed.link,
                            language = "",
                            lastBuildDate = now,
                            subscribedAt = now,
                            lastRefreshedAt = now,
                            episodeCount = rssFeed.episodes.size,
                        )
                        val podcastId = podcastDao.insert(podcast)

                        val episodes = rssFeed.episodes.map { ep ->
                            EpisodeEntity(
                                podcastId = podcastId,
                                guid = ep.guid,
                                title = ep.title,
                                description = ep.description,
                                audioUrl = ep.audioUrl,
                                publicationDate = ep.publishedAt,
                                durationSeconds = (ep.duration / 1000).toInt(),
                                artworkUrl = ep.artworkUrl ?: "",
                                fileSize = ep.fileSize,
                                transcriptUrl = ep.transcriptUrl ?: "",
                                transcriptType = ep.transcriptType ?: "",
                            )
                        }
                        try {
                            episodeDao.insertAll(episodes)
                        } catch (e: Exception) {
                            podcastDao.delete(podcast.copy(id = podcastId))
                            throw e
                        }
                        imported++
                    } catch (_: Exception) {
                        failed.add(entry.title.ifBlank { entry.feedUrl })
                    }
                }

                val result = ImportResult(
                    imported = imported,
                    total = entries.size,
                    skipped = skipped,
                    failed = failed,
                )
                _importResult.value = result

                if (failed.isNotEmpty()) {
                    crashReporter.recordException(
                        throwable = RuntimeException("OPML import: ${failed.size} feed(s) failed"),
                        keys = failed.withIndex().associate { (i, name) -> "failed_feed_$i" to name },
                    )
                }
            } catch (e: Exception) {
                _importResult.value = ImportResult(
                    imported = 0,
                    total = 0,
                    skipped = 0,
                    failed = listOf(e.message ?: "Unknown error"),
                )
            }
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            try {
                val count = downloadManager.deleteAllDownloads()
                _importExportMessage.value = "Deleted $count download(s)"
            } catch (e: Exception) {
                _importExportMessage.value = "Failed to delete downloads: ${e.message}"
            }
        }
    }

    fun shareCrashLogs(onContent: (String) -> Unit) {
        viewModelScope.launch {
            val content = crashLogStore.read()
            if (content.isNullOrBlank()) {
                _importExportMessage.value = "No crash logs recorded yet"
            } else {
                onContent(content)
            }
        }
    }

    fun clearCrashLogs() {
        viewModelScope.launch {
            val hadLogs = crashLogStore.hasLogs()
            crashLogStore.clear()
            _importExportMessage.value = if (hadLogs) {
                "Crash logs cleared"
            } else {
                "No crash logs to clear"
            }
        }
    }

    fun clearMessage() {
        _importExportMessage.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    companion object {
        internal fun normalizeFeedUrl(url: String): String {
            return url.trim()
                .removeSuffix("/")
                .let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
        }
    }

    private data class PartialState(
        val appTheme: AppTheme,
        val feedRefreshIntervalMinutes: Int,
        val autoDownloadEnabled: Boolean,
        val autoDownloadEpisodeCount: Int,
        val autoDeletePlayedAfterDays: Int,
    )

    private data class SecondaryState(
        val downloadOnWifiOnly: Boolean,
        val skipSilence: Boolean,
        val volumeBoost: Boolean,
        val importExportMessage: String?,
        val smartAutoDownload: Boolean,
        val skipAdChapters: Boolean,
        val smartAutoDownloadWindowDays: Int,
        val smartAutoDownloadKeepPerShow: Int,
        val smartAutoDownloadChargingOnly: Boolean,
        val visualizerEnabled: Boolean,
        val visualizerStyle: VisualizerStyle,
        val visualizerBackground: VisualizerBackgroundMode,
    )
}
