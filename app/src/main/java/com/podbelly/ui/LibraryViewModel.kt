package com.podbelly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.common.LibrarySortOrder
import com.podbelly.core.common.PreferencesManager
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val podcasts: List<PodcastEntity> = emptyList(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.NAME_A_TO_Z,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        podcastDao.getAll(),
        preferencesManager.librarySortOrder,
    ) { podcasts, sortOrder ->
        val sorted = when (sortOrder) {
            LibrarySortOrder.NAME_A_TO_Z -> podcasts.sortedBy { it.title.lowercase() }
            LibrarySortOrder.RECENTLY_ADDED -> podcasts.sortedByDescending { it.subscribedAt }
            LibrarySortOrder.EPISODE_COUNT -> podcasts.sortedByDescending { it.episodeCount }
        }
        LibraryUiState(podcasts = sorted, sortOrder = sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setSortOrder(sortOrder: LibrarySortOrder) {
        viewModelScope.launch {
            preferencesManager.setLibrarySortOrder(sortOrder)
        }
    }
}
