package com.podbelly.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class HomeScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createEpisode(
        episodeId: Long = 1L,
        title: String = "Test Episode",
        podcastTitle: String = "Test Podcast",
        artworkUrl: String = "",
        publicationDate: Long = 0L,
        durationSeconds: Int = 0,
        played: Boolean = false,
        downloadPath: String = "",
        playbackPosition: Long = 0L,
    ) = HomeEpisodeItem(
        episodeId = episodeId,
        title = title,
        podcastTitle = podcastTitle,
        artworkUrl = artworkUrl,
        publicationDate = publicationDate,
        durationSeconds = durationSeconds,
        played = played,
        downloadPath = downloadPath,
        playbackPosition = playbackPosition,
    )

    @Test
    fun `empty state shows correct text`() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyState()
            }
        }

        composeTestRule.onNodeWithText("No episodes yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subscribe to podcasts to see new episodes here.")
            .assertIsDisplayed()
    }

    @Test
    fun `episode card shows title and podcast name`() {
        val episode = createEpisode(
            title = "Understanding Coroutines",
            podcastTitle = "Kotlin Weekly"
        )

        composeTestRule.setContent {
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

        composeTestRule.onNodeWithText("Understanding Coroutines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin Weekly").assertIsDisplayed()
    }

    @Test
    fun `play button click triggers callback when downloaded`() {
        var playClicked = false
        val episode = createEpisode(downloadPath = "/downloads/episode.mp3")

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = { playClicked = true },
                    onDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Play episode").performClick()
        assertTrue(playClicked)
    }

    @Test
    fun `episode card click triggers callback`() {
        var cardClicked = false
        val episode = createEpisode()

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = { cardClicked = true },
                    onPlay = {},
                    onDownload = {},
                )
            }
        }

        // Click on the episode title text, which is inside the clickable card
        composeTestRule.onNodeWithText("Test Episode").performClick()
        assertTrue(cardClicked)
    }

    @Test
    fun `episode list renders all items`() {
        val episodes = listOf(
            createEpisode(episodeId = 1L, title = "Episode Alpha"),
            createEpisode(episodeId = 2L, title = "Episode Beta"),
            createEpisode(episodeId = 3L, title = "Episode Gamma")
        )

        composeTestRule.setContent {
            MaterialTheme {
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

        composeTestRule.onNodeWithText("Episode Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode Beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode Gamma").assertIsDisplayed()
    }

    @Test
    fun `continue listening carousel appears when in-progress episodes exist`() {
        val inProgress = listOf(
            createEpisode(
                episodeId = 10L,
                title = "In Progress Episode",
                playbackPosition = 60_000L,
                durationSeconds = 1800,
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeList(
                    episodes = inProgress,
                    inProgressEpisodes = inProgress,
                    downloadProgress = emptyMap(),
                    onEpisodeClick = {},
                    onPlayClick = {},
                    onDownloadClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Continue Listening").assertIsDisplayed()
    }

    @Test
    fun `continue listening carousel hidden when no in-progress episodes`() {
        val episodes = listOf(
            createEpisode(episodeId = 1L, title = "Some Episode")
        )

        composeTestRule.setContent {
            MaterialTheme {
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

        composeTestRule.onNodeWithText("Continue Listening").assertDoesNotExist()
    }

    @Test
    fun `duration chip shows formatted duration`() {
        val episode = createEpisode(durationSeconds = 5400) // 1h 30m

        composeTestRule.setContent {
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

        composeTestRule.onNodeWithText("1h 30m").assertIsDisplayed()
    }
}
