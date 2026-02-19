package com.podbelly.feature.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LibraryPodcastItem(
    val id: Long,
    val title: String,
    val author: String,
    val artworkUrl: String,
    val episodeCount: Int,
    val unplayedCount: Int
)

data class LibraryUiState(
    val podcasts: List<LibraryPodcastItem> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    podcastDao: PodcastDao,
    episodeDao: EpisodeDao
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        podcastDao.getAll(),
        episodeDao.getRecentEpisodes(Int.MAX_VALUE)
    ) { podcasts, allEpisodes ->
        // Group episodes by podcastId to compute unplayed counts
        val unplayedCountsByPodcast = allEpisodes
            .filter { !it.played }
            .groupBy { it.podcastId }
            .mapValues { (_, episodes) -> episodes.size }

        val podcastItems = podcasts.map { entity ->
            LibraryPodcastItem(
                id = entity.id,
                title = entity.title,
                author = entity.author,
                artworkUrl = entity.artworkUrl,
                episodeCount = entity.episodeCount,
                unplayedCount = unplayedCountsByPodcast[entity.id] ?: 0
            )
        }

        LibraryUiState(podcasts = podcastItems)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )
}
