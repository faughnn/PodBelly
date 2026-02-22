package com.podbelly.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class HomeScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun emptyState() {
        paparazzi.snapshot {
            MaterialTheme {
                EmptyState()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun loadedStateWithEpisodes() {
        val episodes = listOf(
            HomeEpisodeItem(
                episodeId = 1L,
                title = "Understanding Kotlin Coroutines: A Deep Dive",
                podcastTitle = "Android Developers Backstage",
                artworkUrl = "",
                publicationDate = System.currentTimeMillis() - 3_600_000, // 1 hour ago
                durationSeconds = 2940, // 49 min
                played = false,
                downloadPath = ""
            ),
            HomeEpisodeItem(
                episodeId = 2L,
                title = "The Future of Compose Multiplatform",
                podcastTitle = "Talking Kotlin",
                artworkUrl = "",
                publicationDate = System.currentTimeMillis() - 86_400_000, // 1 day ago
                durationSeconds = 5400, // 1h 30m
                played = true,
                downloadPath = "/downloads/episode2.mp3"
            ),
            HomeEpisodeItem(
                episodeId = 3L,
                title = "Episode 42: Building Scalable Apps with Jetpack Libraries",
                podcastTitle = "Now in Android",
                artworkUrl = "",
                publicationDate = System.currentTimeMillis() - 172_800_000, // 2 days ago
                durationSeconds = 1800, // 30 min
                played = false,
                downloadPath = ""
            ),
        )

        paparazzi.snapshot {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Home",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        EpisodeList(
                            episodes = episodes,
                            inProgressEpisodes = emptyList(),
                            downloadProgress = emptyMap(),
                            onEpisodeClick = {},
                            onPlayClick = {},
                            onDownloadClick = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun continueListeningCarousel() {
        val inProgressEpisodes = listOf(
            HomeEpisodeItem(
                episodeId = 10L,
                title = "Deep Dive into Coroutines Part 2",
                podcastTitle = "Android Developers Backstage",
                artworkUrl = "",
                publicationDate = System.currentTimeMillis() - 3_600_000,
                durationSeconds = 2940, // 49 min
                played = false,
                downloadPath = "/downloads/episode10.mp3",
                playbackPosition = 900_000L, // 15 min in → 34 min left
            ),
            HomeEpisodeItem(
                episodeId = 11L,
                title = "State Management Patterns",
                podcastTitle = "Talking Kotlin",
                artworkUrl = "",
                publicationDate = System.currentTimeMillis() - 86_400_000,
                durationSeconds = 3600, // 1h
                played = false,
                downloadPath = "/downloads/episode11.mp3",
                playbackPosition = 1_800_000L, // 30 min in → 30 min left
            ),
        )

        paparazzi.snapshot {
            MaterialTheme {
                EpisodeList(
                    episodes = inProgressEpisodes,
                    inProgressEpisodes = inProgressEpisodes,
                    downloadProgress = emptyMap(),
                    onEpisodeClick = {},
                    onPlayClick = {},
                    onDownloadClick = {},
                )
            }
        }
    }

    @Test
    fun singleEpisodeCard() {
        val episode = HomeEpisodeItem(
            episodeId = 1L,
            title = "Why Functional Programming Matters in 2025",
            podcastTitle = "Software Engineering Daily",
            artworkUrl = "",
            publicationDate = System.currentTimeMillis() - 7_200_000, // 2 hours ago
            durationSeconds = 3660, // 1h 1m
            played = false,
            downloadPath = ""
        )

        paparazzi.snapshot {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = {},
                )
            }
        }
    }
}
