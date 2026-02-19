package com.podbelly.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverPodcastItem(
    val title: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val isSubscribed: Boolean,
)

data class DiscoverUiState(
    val searchQuery: String = "",
    val searchResults: List<DiscoverPodcastItem> = emptyList(),
    val isSearching: Boolean = false,
    val feedUrlInput: String = "",
    val isSubscribing: Boolean = false,
    val message: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val searchRepository: PodcastSearchRepository,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(400L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun updateFeedUrl(url: String) {
        _uiState.update { it.copy(feedUrlInput = url) }
    }

    fun search(query: String) {
        updateSearchQuery(query)
        if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(query)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = searchRepository.search(query)
            val items = results.map { result ->
                val existing = podcastDao.getByFeedUrl(result.feedUrl)
                DiscoverPodcastItem(
                    title = result.title,
                    author = result.author,
                    artworkUrl = result.artworkUrl,
                    feedUrl = result.feedUrl,
                    isSubscribed = existing?.subscribed == true,
                )
            }
            _uiState.update { it.copy(searchResults = items, isSearching = false) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    message = "Search failed: ${e.message}",
                )
            }
        }
    }

    fun subscribeToPodcast(feedUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubscribing = true) }
            try {
                val existing = podcastDao.getByFeedUrl(feedUrl)
                if (existing?.subscribed == true) {
                    _uiState.update {
                        it.copy(isSubscribing = false, message = "Already subscribed")
                    }
                    return@launch
                }

                val feed = searchRepository.fetchFeed(feedUrl)
                val now = System.currentTimeMillis()

                val podcastId = if (existing != null) {
                    podcastDao.update(existing.copy(subscribed = true, subscribedAt = now))
                    existing.id
                } else {
                    podcastDao.insert(
                        PodcastEntity(
                            feedUrl = feedUrl,
                            title = feed.title,
                            author = feed.author,
                            description = feed.description,
                            artworkUrl = feed.artworkUrl,
                            link = feed.link,
                            language = "",
                            lastBuildDate = now,
                            subscribed = true,
                            subscribedAt = now,
                            lastRefreshedAt = now,
                            episodeCount = feed.episodes.size,
                        )
                    )
                }

                val episodes = feed.episodes.map { episode ->
                    EpisodeEntity(
                        podcastId = podcastId,
                        guid = episode.guid,
                        title = episode.title,
                        description = episode.description,
                        audioUrl = episode.audioUrl,
                        publicationDate = episode.publishedAt,
                        durationSeconds = (episode.duration / 1000).toInt(),
                        artworkUrl = episode.artworkUrl ?: "",
                    )
                }
                episodeDao.insertAll(episodes)

                _uiState.update { state ->
                    state.copy(
                        isSubscribing = false,
                        message = "Subscribed to ${feed.title}",
                        searchResults = state.searchResults.map { item ->
                            if (item.feedUrl == feedUrl) item.copy(isSubscribed = true)
                            else item
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubscribing = false,
                        message = "Subscription failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun subscribeByUrl(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            _uiState.update { it.copy(message = "Please enter a feed URL") }
            return
        }
        subscribeToPodcast(trimmedUrl)
        _uiState.update { it.copy(feedUrlInput = "") }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
