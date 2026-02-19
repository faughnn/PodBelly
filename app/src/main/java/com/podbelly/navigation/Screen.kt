package com.podbelly.navigation

/**
 * Defines all navigation routes in the Podbelly app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Discover : Screen("discover")
    data object Library : Screen("library")
    data object Queue : Screen("queue")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
    data object Player : Screen("player")
    data object PodcastDetail : Screen("podcast/{podcastId}") {
        fun createRoute(podcastId: Long) = "podcast/$podcastId"
    }
    data object EpisodeDetail : Screen("episode/{episodeId}") {
        fun createRoute(episodeId: Long) = "episode/$episodeId"
    }
}
