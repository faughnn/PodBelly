package com.podbelly.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.podbelly.AppViewModel
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
import com.podbelly.feature.discover.DiscoverScreen
import com.podbelly.feature.home.HomeScreen
import com.podbelly.feature.podcast.EpisodeDetailScreen
import com.podbelly.feature.podcast.PodcastDetailScreen
import com.podbelly.feature.settings.AutoDownloadScreen
import com.podbelly.feature.settings.PlaybackSpeedScreen
import com.podbelly.feature.settings.ProfileScreen
import com.podbelly.feature.settings.SettingsScreen
import com.podbelly.feature.settings.SettingsSection
import com.podbelly.feature.settings.StatsScreen
import com.podbelly.ui.DownloadsScreen
import com.podbelly.ui.LibraryScreen
import com.podbelly.ui.VersionHistoryScreen
import com.podbelly.feature.player.MiniPlayer
import com.podbelly.feature.player.PlayerScreen
import androidx.compose.material3.NavigationBarItemDefaults
import kotlinx.coroutines.delay

private sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    data object Discover : BottomNavItem("discover", "Discover", Icons.Filled.Search)
    data object Library : BottomNavItem("library", "Library", Icons.Filled.Podcasts)
    data object Downloads : BottomNavItem("downloads", "Downloads", Icons.Filled.Download)
    data object Profile : BottomNavItem("profile", "You", Icons.Filled.Person)
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Discover,
    BottomNavItem.Library,
    BottomNavItem.Downloads,
    BottomNavItem.Profile,
)

/**
 * Bottom-nav tap behavior. Three cases, in order:
 *
 * 1. Already on the tab's root: reset it to a fresh root — scroll back to top,
 *    search cleared (the "pop to root on reselect" pattern from Pocket Casts).
 * 2. Somewhere above the tab's root (a podcast/episode page pushed from it, or
 *    from another tab): pop straight back to that root. Detail routes aren't
 *    nested under a tab in this flat graph, so the old selected-only check
 *    never fired here and tapping used to just re-restore the detail screen.
 *    Combined with case 1, a double-tap always lands on a fresh tab root.
 * 3. The tab isn't on the back stack: normal tab switch, saving/restoring each
 *    tab's state so scroll position and search text survive switching.
 */
internal fun NavHostController.onTabClick(route: String) {
    val currentRoute = currentBackStackEntry?.destination?.route
    when {
        currentRoute == route -> navigate(route) {
            popUpTo(route) { inclusive = true }
            launchSingleTop = true
        }
        isRouteOnBackStack(route) -> popBackStack(route, inclusive = false, saveState = true)
        else -> navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

private fun NavHostController.isRouteOnBackStack(route: String): Boolean = try {
    getBackStackEntry(route)
    true
} catch (_: IllegalArgumentException) {
    false
}

@Composable
fun PodbellNavHost(
    playbackController: PlaybackController,
    appViewModel: AppViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    deepLinkFeedUrl: String? = null,
    onSubscribeDeepLink: suspend (String) -> Long? = { null },
    onDeepLinkConsumed: () -> Unit = {},
) {
    val playbackState by playbackController.playbackState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isRefreshing by appViewModel.isRefreshing.collectAsStateWithLifecycle()
    val refreshProgress by appViewModel.refreshProgress.collectAsStateWithLifecycle()

    // Determine whether to show bottom nav and mini player
    val isFullScreenRoute = currentRoute == Screen.Player.route

    var bannerMessage by remember { mutableStateOf<String?>(null) }

    // Subscribe deep link (podcast://…): fetch + subscribe, then jump to the
    // show. Keyed on the URL so each new link fires once; the callback clears it
    // so a recomposition can't re-subscribe.
    LaunchedEffect(deepLinkFeedUrl) {
        val url = deepLinkFeedUrl ?: return@LaunchedEffect
        val podcastId = onSubscribeDeepLink(url)
        if (podcastId != null) {
            navController.navigate(Screen.PodcastDetail.createRoute(podcastId)) {
                launchSingleTop = true
            }
        } else {
            bannerMessage = "Couldn't subscribe to that feed"
            delay(3000L)
            bannerMessage = null
        }
        // Consume last: clearing the URL re-keys this effect, which would cancel
        // the coroutine mid-flight if done before the banner delay above.
        onDeepLinkConsumed()
    }

    LaunchedEffect(Unit) {
        appViewModel.refreshResult.collect { newCount ->
            bannerMessage = if (newCount > 0) {
                "$newCount new episode${if (newCount == 1) "" else "s"} found"
            } else {
                "Everything up to date"
            }
            delay(3000L)
            bannerMessage = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isFullScreenRoute) {
                Column(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Mini player above the bottom navigation
                    MiniPlayer(
                        playbackState = playbackState,
                        onTogglePlayPause = {
                            if (playbackState.isPlaying) {
                                playbackController.pause()
                            } else {
                                playbackController.resume()
                            }
                        },
                        onClick = {
                            navController.navigate(Screen.Player.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    // Bottom navigation bar — Jukebox styling
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.onTabClick(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    isRefreshing = isRefreshing,
                    onRefresh = { appViewModel.refreshFeeds() },
                    refreshProgress = refreshProgress,
                    bannerMessage = bannerMessage,
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Screen.EpisodeDetail.createRoute(episodeId))
                    },
                    onPodcastClick = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    },
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onPodcastClick = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToPodcast = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Screen.EpisodeDetail.createRoute(episodeId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToStats = {
                        navController.navigate(Screen.Stats.route)
                    },
                    onNavigateToSection = { section ->
                        navController.navigate(Screen.SettingsSection.createRoute(section.key))
                    },
                    onNavigateToVersionHistory = {
                        navController.navigate(Screen.VersionHistory.route)
                    },
                )
            }

            composable(
                route = Screen.SettingsSection.route,
                arguments = listOf(
                    navArgument("section") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                SettingsScreen(
                    section = SettingsSection.fromKey(backStackEntry.arguments?.getString("section")),
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlaybackSpeeds = {
                        navController.navigate(Screen.PlaybackSpeeds.route)
                    },
                    onNavigateToAutoDownload = {
                        navController.navigate(Screen.AutoDownload.route)
                    },
                )
            }

            composable(Screen.AutoDownload.route) {
                AutoDownloadScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.VersionHistory.route) {
                VersionHistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPodcast = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }

            composable(Screen.PlaybackSpeeds.route) {
                PlaybackSpeedScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.PodcastDetail.route,
                arguments = listOf(
                    navArgument("podcastId") { type = NavType.LongType }
                )
            ) {
                PodcastDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Screen.EpisodeDetail.createRoute(episodeId))
                    }
                )
            }

            composable(
                route = Screen.EpisodeDetail.route,
                arguments = listOf(
                    navArgument("episodeId") { type = NavType.LongType }
                )
            ) {
                EpisodeDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPodcast = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }

            composable(Screen.Player.route) {
                PlayerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPodcast = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }
        }

        }
    }
}

