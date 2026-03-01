package com.podbelly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastSpeedItem(
    val podcastId: Long,
    val title: String,
    val artworkUrl: String,
    val speed: Float,
)

@HiltViewModel
class PlaybackSpeedViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
) : ViewModel() {

    val podcastSpeeds: StateFlow<List<PodcastSpeedItem>> =
        podcastDao.getPodcastsWithCustomSpeed()
            .map { podcasts ->
                podcasts.map { podcast ->
                    PodcastSpeedItem(
                        podcastId = podcast.id,
                        title = podcast.title,
                        artworkUrl = podcast.artworkUrl,
                        speed = podcast.playbackSpeed,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun resetSpeed(podcastId: Long) {
        viewModelScope.launch {
            podcastDao.resetPlaybackSpeed(podcastId)
        }
    }

    fun resetAllSpeeds() {
        viewModelScope.launch {
            podcastDao.resetAllPlaybackSpeeds()
        }
    }
}
