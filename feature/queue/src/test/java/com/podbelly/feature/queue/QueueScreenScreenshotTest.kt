package com.podbelly.feature.queue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class QueueScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun emptyQueue() {
        paparazzi.snapshot {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Up Next",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    EmptyQueueContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun queueWithMultipleItems() {
        val items = listOf(
            QueueItemUi(
                queueId = 1L,
                episodeId = 101L,
                episodeTitle = "Ep 204: Designing for Accessibility",
                podcastTitle = "Design Matters with Debbie Millman",
                artworkUrl = "",
                durationSeconds = 3240,  // 54 min
                position = 0
            ),
            QueueItemUi(
                queueId = 2L,
                episodeId = 102L,
                episodeTitle = "How AI Is Changing Software Development",
                podcastTitle = "Lex Fridman Podcast",
                artworkUrl = "",
                durationSeconds = 7200, // 2h 0m
                position = 1
            ),
            QueueItemUi(
                queueId = 3L,
                episodeId = 103L,
                episodeTitle = "The Science of Sleep and Productivity",
                podcastTitle = "Huberman Lab",
                artworkUrl = "",
                durationSeconds = 5580, // 1h 33m
                position = 2
            ),
        )

        paparazzi.snapshot {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Up Next",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                TextButton(onClick = {}) {
                                    Text(
                                        text = "Clear All",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    QueueContent(
                        items = items,
                        nowPlayingEpisodeId = 101L,
                        onPlayItem = {},
                        onRemoveItem = {},
                        onMoveUp = {},
                        onMoveDown = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun queueWithNothingPlaying() {
        val items = listOf(
            QueueItemUi(
                queueId = 1L,
                episodeId = 201L,
                episodeTitle = "Understanding Blockchain Beyond Crypto",
                podcastTitle = "a]16z Podcast",
                artworkUrl = "",
                durationSeconds = 2700, // 45 min
                position = 0
            ),
            QueueItemUi(
                queueId = 2L,
                episodeId = 202L,
                episodeTitle = "Mastering Negotiation Skills",
                podcastTitle = "The Tim Ferriss Show",
                artworkUrl = "",
                durationSeconds = 4200, // 1h 10m
                position = 1
            ),
        )

        paparazzi.snapshot {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Up Next",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                TextButton(onClick = {}) {
                                    Text(
                                        text = "Clear All",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    QueueContent(
                        items = items,
                        nowPlayingEpisodeId = null,
                        onPlayItem = {},
                        onRemoveItem = {},
                        onMoveUp = {},
                        onMoveDown = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}
