package com.podbelly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val searchRepository: PodcastSearchRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshResult = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val refreshResult: SharedFlow<Int> = _refreshResult.asSharedFlow()

    init {
        refreshFeeds()
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

                                val episodeEntities = rssFeed.episodes.map { rssEpisode ->
                                    EpisodeEntity(
                                        podcastId = podcast.id,
                                        guid = rssEpisode.guid,
                                        title = rssEpisode.title,
                                        description = rssEpisode.description,
                                        audioUrl = rssEpisode.audioUrl,
                                        publicationDate = rssEpisode.publishedAt,
                                        durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                        artworkUrl = rssEpisode.artworkUrl ?: "",
                                        fileSize = rssEpisode.fileSize
                                    )
                                }

                                val inserted = episodeDao.insertAll(episodeEntities)
                                insertCounts.addAndGet(inserted.count { it != -1L })

                                podcastDao.update(
                                    podcast.copy(lastRefreshedAt = System.currentTimeMillis())
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
            }
        }
    }
}
