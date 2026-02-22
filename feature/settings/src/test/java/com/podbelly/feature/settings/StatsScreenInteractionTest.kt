package com.podbelly.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    title = "Total Listening Time",
                    value = "42h 15m",
                )
            }
        }

        composeTestRule.onNodeWithText("Total Listening Time").assertIsDisplayed()
        composeTestRule.onNodeWithText("42h 15m").assertIsDisplayed()
    }

    @Test
    fun `podcast stat row shows rank and title`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastStatRow(
                    rank = 1,
                    title = "Best Podcast",
                    artworkUrl = "",
                    subtitle = "10h 30m",
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Best Podcast").assertIsDisplayed()
        composeTestRule.onNodeWithText("10h 30m").assertIsDisplayed()
    }

    @Test
    fun `episode stat row shows rank, title, podcast and duration`() {
        composeTestRule.setContent {
            MaterialTheme {
                EpisodeStatRow(
                    rank = 3,
                    title = "Episode 42",
                    podcastTitle = "Science Weekly",
                    subtitle = "2h 15m",
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode 42").assertIsDisplayed()
        composeTestRule.onNodeWithText("Science Weekly").assertIsDisplayed()
        composeTestRule.onNodeWithText("2h 15m").assertIsDisplayed()
    }

    @Test
    fun `podcast stat row shows different ranks`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastStatRow(
                    rank = 10,
                    title = "Tenth Place Show",
                    artworkUrl = "",
                    subtitle = "1h 5m",
                )
            }
        }

        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tenth Place Show").assertIsDisplayed()
    }
}
