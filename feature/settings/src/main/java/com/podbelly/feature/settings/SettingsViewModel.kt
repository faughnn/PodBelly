package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.DarkThemeMode
import com.podbelly.core.common.DownloadManager
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.opml.OpmlFeed
import com.podbelly.core.network.opml.OpmlHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    val feedRefreshIntervalMinutes: Int = 60,
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadEpisodeCount: Int = 3,
    val autoDeletePlayed: Boolean = false,
    val downloadOnWifiOnly: Boolean = true,
    val skipSilence: Boolean = false,
    val volumeBoost: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val importExportMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val searchRepository: PodcastSearchRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private val _importExportMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.darkThemeMode,
        preferencesManager.feedRefreshIntervalMinutes,
        preferencesManager.autoDownloadEnabled,
        preferencesManager.autoDownloadEpisodeCount,
        preferencesManager.autoDeletePlayed,
    ) { darkTheme, refreshInterval, autoDownload, autoDownloadCount, autoDelete ->
        PartialState(
            darkThemeMode = darkTheme,
            feedRefreshIntervalMinutes = refreshInterval,
            autoDownloadEnabled = autoDownload,
            autoDownloadEpisodeCount = autoDownloadCount,
            autoDeletePlayed = autoDelete,
        )
    }.combine(
        combine(
            preferencesManager.downloadOnWifiOnly,
            preferencesManager.skipSilence,
            preferencesManager.volumeBoost,
            preferencesManager.playbackSpeed,
            _importExportMessage,
        ) { wifiOnly, skipSilence, volumeBoost, speed, message ->
            SecondaryState(
                downloadOnWifiOnly = wifiOnly,
                skipSilence = skipSilence,
                volumeBoost = volumeBoost,
                defaultPlaybackSpeed = speed,
                importExportMessage = message,
            )
        }
    ) { partial, secondary ->
        SettingsUiState(
            darkThemeMode = partial.darkThemeMode,
            feedRefreshIntervalMinutes = partial.feedRefreshIntervalMinutes,
            autoDownloadEnabled = partial.autoDownloadEnabled,
            autoDownloadEpisodeCount = partial.autoDownloadEpisodeCount,
            autoDeletePlayed = partial.autoDeletePlayed,
            downloadOnWifiOnly = secondary.downloadOnWifiOnly,
            skipSilence = secondary.skipSilence,
            volumeBoost = secondary.volumeBoost,
            defaultPlaybackSpeed = secondary.defaultPlaybackSpeed,
            importExportMessage = secondary.importExportMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { preferencesManager.setDarkThemeMode(mode) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { preferencesManager.setPlaybackSpeed(speed) }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSkipSilence(enabled) }
    }

    fun setVolumeBoost(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setVolumeBoost(enabled) }
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

    fun setAutoDeletePlayed(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoDeletePlayed(enabled) }
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
                val xml = OpmlHandler.generateOpml(feeds)
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
                val entries = OpmlHandler.parseOpml(xml)
                var imported = 0

                for (entry in entries) {
                    try {
                        val existing = podcastDao.getByFeedUrl(entry.feedUrl)
                        if (existing != null) continue

                        val rssFeed = searchRepository.fetchFeed(entry.feedUrl)
                        val now = System.currentTimeMillis()

                        val podcast = PodcastEntity(
                            feedUrl = entry.feedUrl,
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
                            )
                        }
                        episodeDao.insertAll(episodes)
                        imported++
                    } catch (_: Exception) {
                        // Skip feeds that fail
                    }
                }

                _importExportMessage.value = "Imported $imported of ${entries.size} subscription(s)"
            } catch (e: Exception) {
                _importExportMessage.value = "Import failed: ${e.message}"
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

    fun clearMessage() {
        _importExportMessage.value = null
    }

    private data class PartialState(
        val darkThemeMode: DarkThemeMode,
        val feedRefreshIntervalMinutes: Int,
        val autoDownloadEnabled: Boolean,
        val autoDownloadEpisodeCount: Int,
        val autoDeletePlayed: Boolean,
    )

    private data class SecondaryState(
        val downloadOnWifiOnly: Boolean,
        val skipSilence: Boolean,
        val volumeBoost: Boolean,
        val defaultPlaybackSpeed: Float,
        val importExportMessage: String?,
    )
}
