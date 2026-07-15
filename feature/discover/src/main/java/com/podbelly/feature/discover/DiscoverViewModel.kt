package com.podbelly.feature.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.network.api.PodcastSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
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

/** A browsable chart category, mapping a chip label to an iTunes genre id. */
data class ChartCategory(val label: String, val genreId: Int)

data class DiscoverUiState(
    val searchQuery: String = "",
    val searchResults: List<DiscoverPodcastItem> = emptyList(),
    val isSearching: Boolean = false,
    val feedUrlInput: String = "",
    /** Feed URLs with a subscription currently in flight, so each row can spin/disable independently. */
    val subscribingFeedUrls: Set<String> = emptySet(),
    val message: String? = null,
    val chartCategories: List<ChartCategory> = DiscoverViewModel.CHART_CATEGORIES,
    val selectedChartGenreId: Int = DiscoverViewModel.TOP_CHART_GENRE_ID,
    val chartResults: List<DiscoverPodcastItem> = emptyList(),
    val isLoadingChart: Boolean = false,
    val chartError: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val searchRepository: PodcastSearchRepository,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _navigateToPodcast = Channel<Long>(Channel.BUFFERED)
    val navigateToPodcast = _navigateToPodcast.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    /** Explicit (e.g. IME "Search" action) queries that should run without the debounce. */
    private val immediateSearch = Channel<String>(Channel.CONFLATED)

    /** Charts already fetched this session, so switching chips back is instant. */
    private val chartCache = mutableMapOf<Int, List<DiscoverPodcastItem>>()

    // Charts are region-specific; follow the device locale, falling back to the
    // US chart when the locale carries no country.
    private val chartCountry: String =
        java.util.Locale.getDefault().country.lowercase().ifBlank { "us" }

    init {
        viewModelScope.launch {
            // A single pipeline runs every search. collectLatest cancels any in-flight
            // search when a newer query arrives, so two concurrent requests can no longer
            // race to overwrite the results (last-writer-wins).
            merge(
                searchQueryFlow
                    .debounce(400L)
                    .distinctUntilChanged()
                    .filter { it.isNotBlank() },
                immediateSearch.receiveAsFlow(),
            ).collectLatest { query ->
                performSearch(query)
            }
        }

        loadChart(TOP_CHART_GENRE_ID)
    }

    fun selectChartCategory(genreId: Int) {
        _uiState.update { it.copy(selectedChartGenreId = genreId) }
        loadChart(genreId)
    }

    fun retryChart() {
        loadChart(_uiState.value.selectedChartGenreId)
    }

    private fun loadChart(genreId: Int) {
        chartCache[genreId]?.let { cached ->
            _uiState.update { it.copy(chartResults = cached, isLoadingChart = false, chartError = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, chartError = null) }
            try {
                val results = searchRepository.topPodcasts(chartCountry, genreId)
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
                chartCache[genreId] = items
                // Only publish if this chip is still selected; a slow response for a
                // deselected category must not overwrite the current chart.
                if (_uiState.value.selectedChartGenreId == genreId) {
                    _uiState.update { it.copy(chartResults = items, isLoadingChart = false) }
                }
            } catch (e: Exception) {
                if (_uiState.value.selectedChartGenreId == genreId) {
                    _uiState.update {
                        it.copy(
                            isLoadingChart = false,
                            chartError = "Couldn't load charts: ${e.message}",
                        )
                    }
                }
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
            // Bypass the debounce for an explicit search; the shared pipeline still
            // serializes it via collectLatest.
            immediateSearch.trySend(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = searchRepository.search(query)
            // A blank query is filtered out of the search pipeline, so clearing the box
            // doesn't cancel this in-flight call via collectLatest. Bail if the query box
            // no longer matches what we searched, so stale results can't repopulate a
            // box the user has since cleared or changed.
            if (_uiState.value.searchQuery != query) return
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
        viewModelScope.launch { performSubscribe(feedUrl) }
    }

    private suspend fun performSubscribe(feedUrl: String) {
        _uiState.update { it.copy(subscribingFeedUrls = it.subscribingFeedUrls + feedUrl) }
        try {
                val existing = podcastDao.getByFeedUrl(feedUrl)
                if (existing?.subscribed == true) {
                    _uiState.update { it.copy(message = "Already subscribed") }
                    return
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
                prefetchArtwork(feed.artworkUrl)

                fun List<DiscoverPodcastItem>.markSubscribed() = map { item ->
                    if (item.feedUrl == feedUrl) item.copy(isSubscribed = true) else item
                }
                _uiState.update { state ->
                    state.copy(
                        message = "Subscribed to ${feed.title}",
                        searchResults = state.searchResults.markSubscribed(),
                        chartResults = state.chartResults.markSubscribed(),
                    )
                }
                // Cached charts hold their own isSubscribed flags — keep them honest.
                for ((genreId, items) in chartCache) {
                    chartCache[genreId] = items.markSubscribed()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Subscription failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(subscribingFeedUrls = it.subscribingFeedUrls - feedUrl) }
            }
    }

    fun subscribeByUrl(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            _uiState.update { it.copy(message = "Please enter a feed URL") }
            return
        }
        // Clear the input only after the subscribe completes. The RSS section derives its
        // spinner from `feedUrlInput in subscribingFeedUrls`; clearing the field up front
        // (as before) made that test always false, so the progress indicator never showed.
        viewModelScope.launch {
            performSubscribe(trimmedUrl)
            _uiState.update { it.copy(feedUrlInput = "") }
        }
    }

    fun onPodcastClick(feedUrl: String) {
        // Pure navigation — must NOT touch subscribingFeedUrls (that would disable
        // every Subscribe button while this fetch runs).
        viewModelScope.launch {
            try {
                val existing = podcastDao.getByFeedUrl(feedUrl)
                if (existing != null) {
                    _navigateToPodcast.send(existing.id)
                    return@launch
                }

                val feed = searchRepository.fetchFeed(feedUrl)
                val now = System.currentTimeMillis()

                // Insert-if-absent (not REPLACE) so a concurrent second tap can't replace
                // the row with a new id and CASCADE-delete the episodes we just inserted.
                val newId = podcastDao.insertIfAbsent(
                    PodcastEntity(
                        feedUrl = feedUrl,
                        title = feed.title,
                        author = feed.author,
                        description = feed.description,
                        artworkUrl = feed.artworkUrl,
                        link = feed.link,
                        language = "",
                        lastBuildDate = now,
                        subscribed = false,
                        subscribedAt = 0L,
                        lastRefreshedAt = now,
                        episodeCount = feed.episodes.size,
                    )
                )
                val podcastId = if (newId != -1L) newId else {
                    podcastDao.getByFeedUrl(feedUrl)?.id ?: return@launch
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
                prefetchArtwork(feed.artworkUrl)

                _navigateToPodcast.send(podcastId)
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to load podcast: ${e.message}") }
            }
        }
    }

    private fun prefetchArtwork(url: String) {
        if (url.isBlank()) return
        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        imageLoader.enqueue(request)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        /** iTunes' root "Podcasts" genre — the overall chart. */
        const val TOP_CHART_GENRE_ID = 26

        val CHART_CATEGORIES = listOf(
            ChartCategory("Top", TOP_CHART_GENRE_ID),
            ChartCategory("Comedy", 1303),
            ChartCategory("News", 1489),
            ChartCategory("True Crime", 1488),
            ChartCategory("Technology", 1318),
            ChartCategory("Society & Culture", 1324),
            ChartCategory("Sport", 1545),
            ChartCategory("Business", 1321),
            ChartCategory("History", 1487),
            ChartCategory("Science", 1533),
            ChartCategory("Health & Fitness", 1512),
            ChartCategory("Music", 1310),
            ChartCategory("TV & Film", 1309),
            ChartCategory("Education", 1304),
        )
    }
}
