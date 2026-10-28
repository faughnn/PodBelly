package com.podbelly.feature.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.FeedItemKey
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.normalizeTitleForMatch
import com.podbelly.core.network.api.PodcastSearchRepository
import com.podbelly.core.network.model.EpisodeSearchResult
import com.podbelly.core.network.model.RssEpisode
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchField
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

/**
 * A browsable chart category, mapping a chip label to an iTunes genre id, with the
 * category's own sub-categories (each chartable on its own).
 */
data class ChartCategory(
    val label: String,
    val genreId: Int,
    val subcategories: List<ChartCategory> = emptyList(),
)

/** A selectable chart storefront region. */
data class ChartRegion(val code: String, val label: String)

/** Whether a search looks for whole shows or individual episodes. */
enum class SearchScope { SHOWS, EPISODES }

data class DiscoverUiState(
    val searchQuery: String = "",
    val searchResults: List<DiscoverPodcastItem> = emptyList(),
    val searchScope: SearchScope = SearchScope.SHOWS,
    /** Which field a show search matches; episode search always matches everything. */
    val searchField: SearchField = SearchField.ANYTHING,
    val episodeResults: List<EpisodeSearchResult> = emptyList(),
    /** Episodes being resolved to a stored row after a tap, so their rows can show progress. */
    val openingEpisodes: Set<EpisodeSearchResult> = emptySet(),
    val isSearching: Boolean = false,
    val feedUrlInput: String = "",
    /** Feed URLs with a subscription currently in flight, so each row can spin/disable independently. */
    val subscribingFeedUrls: Set<String> = emptySet(),
    val message: String? = null,
    val chartCategories: List<ChartCategory> = DiscoverViewModel.CHART_CATEGORIES,
    val selectedChartGenreId: Int = DiscoverViewModel.TOP_CHART_GENRE_ID,
    /** A sub-category of the selected category, or null for the whole category. */
    val selectedChartSubgenreId: Int? = null,
    val chartResults: List<DiscoverPodcastItem> = emptyList(),
    /** The trending-episodes chart, shown instead of [chartResults] when it's selected. */
    val chartEpisodeResults: List<EpisodeSearchResult> = emptyList(),
    val isLoadingChart: Boolean = false,
    val chartError: String? = null,
    val chartRegions: List<ChartRegion> = DiscoverViewModel.CHART_REGIONS,
    val selectedChartCountry: String = "",
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val searchRepository: PodcastSearchRepository,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _navigateToPodcast = Channel<Long>(Channel.BUFFERED)
    val navigateToPodcast = _navigateToPodcast.receiveAsFlow()

    private val _navigateToEpisode = Channel<Long>(Channel.BUFFERED)
    val navigateToEpisode = _navigateToEpisode.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    /** Explicit (e.g. IME "Search" action) queries that should run without the debounce. */
    private val immediateSearch = Channel<String>(Channel.CONFLATED)

    /** Charts already fetched this session, so switching chips back is instant. */
    private val chartCache = mutableMapOf<Int, List<DiscoverPodcastItem>>()
    private var trendingEpisodesCache: List<EpisodeSearchResult>? = null

    // Charts are region-specific. The stored preference wins; blank means
    // "follow the device locale", falling back to the US chart when the locale
    // carries no country. Resolved before the first chart load in init.
    private var chartCountry: String = "us"

    private fun defaultChartCountry(): String =
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

        viewModelScope.launch {
            val stored = preferencesManager.chartCountry.first()
            chartCountry = stored.ifBlank { defaultChartCountry() }
            _uiState.update { it.copy(selectedChartCountry = chartCountry) }
            loadChart(TOP_CHART_GENRE_ID)
        }
    }

    fun selectChartRegion(countryCode: String) {
        if (countryCode == chartCountry) return
        chartCountry = countryCode
        // Cached charts belong to the previous region.
        chartCache.clear()
        trendingEpisodesCache = null
        _uiState.update {
            it.copy(
                selectedChartCountry = countryCode,
                chartResults = emptyList(),
                chartEpisodeResults = emptyList(),
            )
        }
        viewModelScope.launch { preferencesManager.setChartCountry(countryCode) }
        loadChart(activeChartGenreId())
        // Search follows the chart region's storefront, so re-run it there too.
        rerunSearch()
    }

    fun selectChartCategory(genreId: Int) {
        _uiState.update { it.copy(selectedChartGenreId = genreId, selectedChartSubgenreId = null) }
        loadChart(genreId)
    }

    /** Narrows the selected category's chart to one sub-category; null shows the whole category. */
    fun selectChartSubcategory(subgenreId: Int?) {
        _uiState.update { it.copy(selectedChartSubgenreId = subgenreId) }
        loadChart(activeChartGenreId())
    }

    fun retryChart() {
        loadChart(activeChartGenreId())
    }

    /** The genre whose chart is on screen: the sub-category when one is picked. */
    private fun activeChartGenreId(): Int =
        _uiState.value.let { it.selectedChartSubgenreId ?: it.selectedChartGenreId }

    private fun loadChart(genreId: Int) {
        if (genreId == TRENDING_EPISODES_ID) {
            loadTrendingEpisodes()
            return
        }
        chartCache[genreId]?.let { cached ->
            _uiState.update { it.copy(chartResults = cached, isLoadingChart = false, chartError = null) }
            return
        }

        val requestCountry = chartCountry
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, chartError = null) }
            try {
                val results = searchRepository.topPodcasts(requestCountry, genreId, CHART_SIZE)
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
                // Only publish/cache if this chip and region are still selected; a
                // slow response for a deselected category (or the previous region)
                // must not overwrite or pollute the current chart.
                if (chartCountry != requestCountry) return@launch
                chartCache[genreId] = items
                if (activeChartGenreId() == genreId) {
                    _uiState.update { it.copy(chartResults = items, isLoadingChart = false) }
                }
            } catch (e: Exception) {
                if (chartCountry == requestCountry && activeChartGenreId() == genreId) {
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

    private fun loadTrendingEpisodes() {
        trendingEpisodesCache?.let { cached ->
            _uiState.update { it.copy(chartEpisodeResults = cached, isLoadingChart = false, chartError = null) }
            return
        }

        val requestCountry = chartCountry
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, chartError = null) }
            try {
                val results = searchRepository.trendingEpisodes(requestCountry, TRENDING_EPISODES_SIZE)
                // Same staleness rule as loadChart: drop it if the region or chip changed.
                if (chartCountry != requestCountry) return@launch
                trendingEpisodesCache = results
                if (activeChartGenreId() == TRENDING_EPISODES_ID) {
                    _uiState.update { it.copy(chartEpisodeResults = results, isLoadingChart = false) }
                }
            } catch (e: Exception) {
                if (chartCountry == requestCountry && activeChartGenreId() == TRENDING_EPISODES_ID) {
                    _uiState.update {
                        it.copy(
                            isLoadingChart = false,
                            chartError = "Couldn't load trending episodes: ${e.message}",
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
            _uiState.update {
                it.copy(searchResults = emptyList(), episodeResults = emptyList(), isSearching = false)
            }
        }
    }

    /** Switches between searching shows and episodes, re-running the current query. */
    fun selectSearchScope(scope: SearchScope) {
        if (scope == _uiState.value.searchScope) return
        _uiState.update { it.copy(searchScope = scope) }
        rerunSearch()
    }

    /** Narrows a show search to titles or hosts, re-running the current query. */
    fun selectSearchField(field: SearchField) {
        if (field == _uiState.value.searchField) return
        _uiState.update { it.copy(searchField = field) }
        rerunSearch()
    }

    /** Re-runs the current query now (skipping the debounce) after a filter changed. */
    private fun rerunSearch() {
        val query = _uiState.value.searchQuery
        if (query.isNotBlank()) immediateSearch.trySend(query)
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
        val scope = _uiState.value.searchScope
        val field = _uiState.value.searchField
        try {
            if (scope == SearchScope.EPISODES) {
                val episodes = searchRepository.searchEpisodes(query, chartCountry)
                if (_uiState.value.searchQuery != query) return
                _uiState.update { it.copy(episodeResults = episodes, isSearching = false) }
                return
            }
            // Searches the chart region's storefront: some shows aren't listed in the
            // default (US) one at all.
            val results = searchRepository.search(query, field, chartCountry)
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
        } catch (e: CancellationException) {
            // A newer query or filter superseded this search (collectLatest); not a failure.
            throw e
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
                    // insertAll below skips stored GUIDs; re-key ones the host changed.
                    episodeDao.repairChangedGuids(existing.id, feed.episodes.map {
                        FeedItemKey(it.guid, it.title, it.audioUrl, it.publishedAt, (it.duration / 1000).toInt())
                    })
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
                        transcriptUrl = episode.transcriptUrl ?: "",
                        transcriptType = episode.transcriptType ?: "",
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
                val podcastId = storePreview(feedUrl, feed) ?: return@launch
                _navigateToPodcast.send(podcastId)
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to load podcast: ${e.message}") }
            }
        }
    }

    /**
     * Opens an episode from episode search or the trending chart. Its show is stored
     * (unsubscribed) if it isn't already, like a podcast preview, then the episode is
     * found among the show's episodes and its page opened; that page offers the usual
     * download-then-play button. Falls back to the show's page if the feed no longer
     * lists the episode.
     */
    fun onEpisodeClick(item: EpisodeSearchResult) {
        if (item in _uiState.value.openingEpisodes) return
        _uiState.update { it.copy(openingEpisodes = it.openingEpisodes + item) }
        viewModelScope.launch {
            try {
                val existing = podcastDao.getByFeedUrl(item.feedUrl)
                existing?.let { podcast ->
                    findStoredEpisode(podcast.id, item)?.let { episodeId ->
                        _navigateToEpisode.send(episodeId)
                        return@launch
                    }
                }

                // New show, or a stored copy older than this episode: fetch the feed.
                val feed = searchRepository.fetchFeed(item.feedUrl)
                val podcastId = if (existing == null) {
                    storePreview(item.feedUrl, feed) ?: return@launch
                } else {
                    episodeDao.repairChangedGuids(existing.id, feed.episodes.map {
                        FeedItemKey(it.guid, it.title, it.audioUrl, it.publishedAt, (it.duration / 1000).toInt())
                    })
                    episodeDao.insertAll(feed.episodes.map { it.toEntity(existing.id) })
                    existing.id
                }

                val episodeId = findStoredEpisode(podcastId, item)
                if (episodeId != null) {
                    _navigateToEpisode.send(episodeId)
                } else {
                    _navigateToPodcast.send(podcastId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to load episode: ${e.message}") }
            } finally {
                _uiState.update { it.copy(openingEpisodes = it.openingEpisodes - item) }
            }
        }
    }

    /**
     * The stored episode [item] refers to: by GUID when Apple gave one, else by audio
     * URL or title (trending entries carry neither GUID nor audio URL).
     */
    private suspend fun findStoredEpisode(podcastId: Long, item: EpisodeSearchResult): Long? {
        item.episodeGuid?.let { guid ->
            episodeDao.getByPodcastAndGuid(podcastId, guid)?.let { return it.id }
        }
        val title = normalizeTitleForMatch(item.title)
        val rows = episodeDao.getMatchRowsForPodcast(podcastId)
        return (
            rows.firstOrNull { item.audioUrl != null && it.audioUrl == item.audioUrl }
                ?: rows.firstOrNull { title.isNotEmpty() && normalizeTitleForMatch(it.title) == title }
            )?.id
    }

    /**
     * Stores a show being previewed (not subscribed) with its episodes, returning its
     * id. Insert-if-absent (not REPLACE) so a concurrent second tap can't replace the
     * row with a new id and CASCADE-delete the episodes just inserted.
     */
    private suspend fun storePreview(feedUrl: String, feed: RssFeed): Long? {
        val now = System.currentTimeMillis()
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
            podcastDao.getByFeedUrl(feedUrl)?.id ?: return null
        }
        episodeDao.insertAll(feed.episodes.map { it.toEntity(podcastId) })
        prefetchArtwork(feed.artworkUrl)
        return podcastId
    }

    private fun RssEpisode.toEntity(podcastId: Long) = EpisodeEntity(
        podcastId = podcastId,
        guid = guid,
        title = title,
        description = description,
        audioUrl = audioUrl,
        publicationDate = publishedAt,
        durationSeconds = (duration / 1000).toInt(),
        artworkUrl = artworkUrl ?: "",
        transcriptUrl = transcriptUrl ?: "",
        transcriptType = transcriptType ?: "",
    )

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

        /** Pseudo genre id for the "Trending episodes" chip (not an iTunes genre). */
        const val TRENDING_EPISODES_ID = -1

        /** Shows per chart; Apple's chart feed serves at most 200. */
        const val CHART_SIZE = 200

        const val TRENDING_EPISODES_SIZE = 50

        val CHART_REGIONS = listOf(
            ChartRegion("ie", "Ireland"),
            ChartRegion("gb", "United Kingdom"),
            ChartRegion("us", "United States"),
            ChartRegion("au", "Australia"),
            ChartRegion("ca", "Canada"),
            ChartRegion("nz", "New Zealand"),
            ChartRegion("de", "Germany"),
            ChartRegion("fr", "France"),
            ChartRegion("es", "Spain"),
            ChartRegion("it", "Italy"),
            ChartRegion("nl", "Netherlands"),
            ChartRegion("se", "Sweden"),
            ChartRegion("no", "Norway"),
            ChartRegion("dk", "Denmark"),
            ChartRegion("pt", "Portugal"),
            ChartRegion("pl", "Poland"),
            ChartRegion("br", "Brazil"),
            ChartRegion("mx", "Mexico"),
            ChartRegion("jp", "Japan"),
            ChartRegion("in", "India"),
            ChartRegion("za", "South Africa"),
        )

        val CHART_CATEGORIES = listOf(
            ChartCategory("Top", TOP_CHART_GENRE_ID),
            ChartCategory("Trending episodes", TRENDING_EPISODES_ID),
        ) + APPLE_PODCAST_CATEGORIES
    }
}
