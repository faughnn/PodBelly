package com.podbelly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.LibrarySortOrder
import com.podbelly.core.common.LibraryViewMode
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val podcasts: List<PodcastEntity> = emptyList(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.NAME_A_TO_Z,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val listeningSessionDao: ListeningSessionDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        podcastDao.getAll(),
        preferencesManager.librarySortOrder,
        preferencesManager.libraryViewMode,
        episodeDao.getLatestEpisodeDateByPodcast(),
        listeningSessionDao.getMostListenedPodcasts(limit = Int.MAX_VALUE),
    ) { podcasts, sortOrder, viewMode, latestEpisodes, listenedStats ->
        val latestEpisodeMap = latestEpisodes.associate { it.podcastId to it.latestPublicationDate }
        val listenedMap = listenedStats.associate { it.podcastId to it.totalListenedMs }

        val sorted = when (sortOrder) {
            LibrarySortOrder.NAME_A_TO_Z -> podcasts.sortedBy { it.title.lowercase() }
            LibrarySortOrder.RECENTLY_ADDED -> podcasts.sortedByDescending { it.subscribedAt }
            LibrarySortOrder.EPISODE_COUNT -> podcasts.sortedByDescending { it.episodeCount }
            LibrarySortOrder.MOST_RECENT_EPISODE -> podcasts.sortedByDescending { latestEpisodeMap[it.id] ?: 0L }
            LibrarySortOrder.MOST_LISTENED -> podcasts.sortedByDescending { listenedMap[it.id] ?: 0L }
        }
        LibraryUiState(podcasts = sorted, sortOrder = sortOrder, viewMode = viewMode)
    }.combine(searchQuery) { state, query ->
        val filtered = if (query.isBlank()) {
            state.podcasts
        } else {
            val lowerQuery = query.lowercase()
            state.podcasts.filter { podcast ->
                podcast.title.lowercase().contains(lowerQuery) ||
                    podcast.author.lowercase().contains(lowerQuery)
            }
        }
        state.copy(podcasts = filtered, searchQuery = query)
    }.combine(isSearchActive) { state, active ->
        state.copy(isSearchActive = active)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        if (!active) {
            searchQuery.value = ""
        }
        isSearchActive.value = active
    }

    fun setSortOrder(sortOrder: LibrarySortOrder) {
        viewModelScope.launch {
            preferencesManager.setLibrarySortOrder(sortOrder)
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = uiState.value.viewMode
            val next = if (current == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
            preferencesManager.setLibraryViewMode(next)
        }
    }
}
