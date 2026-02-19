package com.podbelly.core.playback

data class PlaybackState(
    val episodeId: Long = 0L,
    val podcastTitle: String = "",
    val episodeTitle: String = "",
    val artworkUrl: String = "",
    val audioUrl: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val skipSilence: Boolean = false,
    val volumeBoost: Boolean = false,
)
