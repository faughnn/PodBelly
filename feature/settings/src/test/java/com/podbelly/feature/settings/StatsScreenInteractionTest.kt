package com.podbelly.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.PodcastListeningStat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class StatsScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `summary card shows title and value`() {
        composeTestRule.setContent {
            MaterialTheme {
                StatsSummaryCard(
                    title = "Listened",
                    value = "42h 15m",
                )
            }
        }

        composeTestRule.onNodeWithText("Listened").assertIsDisplayed()
        composeTestRule.onNodeWithText("42h 15m").assertIsDisplayed()
    }

    @Test
    fun `podcast stats list shows rank, title, and episode count`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastStatsList(
                    stats = listOf(
                        PodcastListeningStat(
                            podcastId = 1L,
                            podcastTitle = "Best Podcast",
                            artworkUrl = "",
                            totalListenedMs = 630_000L, // 10h 30m
                            episodeCount = 12L,
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Best Podcast").assertIsDisplayed()
        // Subtitle shows cumulative duration and episode count
        composeTestRule.onNodeWithText("0h 10m \u00b7 12 episodes").assertIsDisplayed()
    }

    @Test
    fun `podcast stats list shows singular episode for count of 1`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastStatsList(
                    stats = listOf(
                        PodcastListeningStat(
                            podcastId = 1L,
                            podcastTitle = "New Show",
                            artworkUrl = "",
                            totalListenedMs = 3_600_000L,
                            episodeCount = 1L,
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("1h 0m \u00b7 1 episode").assertIsDisplayed()
    }

    @Test
    fun `episode stats list shows rank, title, podcast and duration`() {
        composeTestRule.setContent {
            MaterialTheme {
                EpisodeStatsList(
                    stats = listOf(
                        EpisodeListeningStat(
                            episodeId = 10L,
                            episodeTitle = "Episode 42",
                            podcastTitle = "Science Weekly",
                            totalListenedMs = 8_100_000L, // 2h 15m
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode 42").assertIsDisplayed()
        composeTestRule.onNodeWithText("Science Weekly").assertIsDisplayed()
        composeTestRule.onNodeWithText("2h 15m").assertIsDisplayed()
    }

    @Test
    fun `podcast stats list shows multiple ranks`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastStatsList(
                    stats = listOf(
                        PodcastListeningStat(1L, "First Place", "", 50_000L, 5L),
                        PodcastListeningStat(2L, "Second Place", "", 30_000L, 3L),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("First Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Place").assertIsDisplayed()
    }
}
