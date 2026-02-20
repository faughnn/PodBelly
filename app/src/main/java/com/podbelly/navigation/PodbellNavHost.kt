package com.podbelly.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import coil.compose.AsyncImage
import com.podbelly.core.playback.PlaybackController
import com.podbelly.core.playback.PlaybackState
import com.podbelly.feature.discover.DiscoverScreen
import com.podbelly.feature.home.HomeScreen
import com.podbelly.feature.podcast.EpisodeDetailScreen
import com.podbelly.feature.podcast.PodcastDetailScreen
import com.podbelly.feature.settings.SettingsScreen
import com.podbelly.feature.settings.StatsScreen
import com.podbelly.ui.DownloadsScreen
import com.podbelly.ui.LibraryScreen
import com.podbelly.feature.player.PlayerScreen

private sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    data object Discover : BottomNavItem("discover", "Discover", Icons.Filled.Search)
    data object Library : BottomNavItem("library", "Library", Icons.Filled.Podcasts)
    data object Downloads : BottomNavItem("downloads", "Downloads", Icons.Filled.Download)
    data object Settings : BottomNavItem("settings", "Settings", Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Discover,
    BottomNavItem.Library,
    BottomNavItem.Downloads,
    BottomNavItem.Settings,
)

@Composable
fun PodbellNavHost(
    playbackController: PlaybackController,
    navController: NavHostController = rememberNavController()
) {
    val playbackState by playbackController.playbackState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine whether to show bottom nav and mini player
    val isFullScreenRoute = currentRoute == Screen.Player.route
    val showMiniPlayer = playbackState.episodeId != 0L && !isFullScreenRoute

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isFullScreenRoute) {
                Column(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Mini player above the bottom navigation
                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
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
                    }

                    // Bottom navigation bar
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Screen.EpisodeDetail.createRoute(episodeId))
                    },
                    onPodcastClick = { podcastId ->
                        navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                    }
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen()
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

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToStats = {
                        navController.navigate(Screen.Stats.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
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
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            // Progress bar
            if (playbackState.duration > 0L) {
                LinearProgressIndicator(
                    progress = {
                        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat())
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                AsyncImage(
                    model = playbackState.artworkUrl.ifBlank { null },
                    contentDescription = null,
                    placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                    error = rememberVectorPainter(Icons.Default.Podcasts),
                    fallback = rememberVectorPainter(Icons.Default.Podcasts),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Title and podcast name
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playbackState.episodeTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playbackState.podcastTitle.isNotBlank()) {
                        Text(
                            text = playbackState.podcastTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play/Pause button
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
