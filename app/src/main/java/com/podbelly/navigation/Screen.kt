package com.podbelly.navigation

/**
 * Defines all navigation routes in the Podbelly app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Discover : Screen("discover")
    data object Library : Screen("library")
    data object Downloads : Screen("downloads")
    data object Profile : Screen("profile")
    data object SettingsSection : Screen("settings/{section}") {
        fun createRoute(section: String) = "settings/$section"
    }
    data object Player : Screen("player")
    data object PodcastDetail : Screen("podcast/{podcastId}") {
        fun createRoute(podcastId: Long) = "podcast/$podcastId"
    }
    data object EpisodeDetail : Screen("episode/{episodeId}") {
        fun createRoute(episodeId: Long) = "episode/$episodeId"
    }
    data object Stats : Screen("stats")
    data object PlaybackSpeeds : Screen("playback_speeds")
    data object AutoDownload : Screen("auto_download")
}
