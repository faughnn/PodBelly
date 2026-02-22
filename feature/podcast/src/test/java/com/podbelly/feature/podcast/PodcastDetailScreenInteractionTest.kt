package com.podbelly.feature.podcast

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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
class PodcastDetailScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeEpisode(
        id: Long = 1L,
        title: String = "Test Episode",
        played: Boolean = false,
        isDownloaded: Boolean = false,
        durationSeconds: Int = 1800,
        playbackPosition: Long = 0L,
    ) = EpisodeUiModel(
        id = id,
        title = title,
        description = "Description",
        publicationDate = 1700000000000L,
        durationSeconds = durationSeconds,
        played = played,
        playbackPosition = playbackPosition,
        isDownloaded = isDownloaded,
    )

    // -- FilterChipsRow tests --

    @Test
    fun `filter chips show all three options`() {
        composeTestRule.setContent {
            MaterialTheme {
                FilterChipsRow(
                    currentFilter = EpisodeFilter.ALL,
                    onFilterSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unplayed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloaded").assertIsDisplayed()
    }

    @Test
    fun `filter chip ALL is selected by default`() {
        composeTestRule.setContent {
            MaterialTheme {
                FilterChipsRow(
                    currentFilter = EpisodeFilter.ALL,
                    onFilterSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("All").assertIsSelected()
    }

    @Test
    fun `clicking filter chip triggers callback`() {
        var selectedFilter: EpisodeFilter? = null

        composeTestRule.setContent {
            MaterialTheme {
                FilterChipsRow(
                    currentFilter = EpisodeFilter.ALL,
                    onFilterSelected = { selectedFilter = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Unplayed").performClick()
        assertEquals(EpisodeFilter.UNPLAYED, selectedFilter)
    }

    // -- EpisodeCard tests --

    @Test
    fun `episode card shows title`() {
        val episode = makeEpisode(title = "Great Episode Title")

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Great Episode Title").assertIsDisplayed()
    }

    @Test
    fun `episode card shows download button when not downloaded`() {
        val episode = makeEpisode(isDownloaded = false)

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Download episode").assertIsDisplayed()
    }

    @Test
    fun `episode card shows play button when downloaded`() {
        val episode = makeEpisode(isDownloaded = true)

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Play episode").assertIsDisplayed()
    }

    @Test
    fun `download button triggers onDownload callback`() {
        var downloadClicked = false
        val episode = makeEpisode(isDownloaded = false)

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = { downloadClicked = true },
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Download episode").performClick()
        assertTrue(downloadClicked)
    }

    @Test
    fun `play button triggers onPlay when downloaded`() {
        var playClicked = false
        val episode = makeEpisode(isDownloaded = true)

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = { playClicked = true },
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Play episode").performClick()
        assertTrue(playClicked)
    }

    @Test
    fun `episode card click triggers onClick callback`() {
        var clicked = false
        val episode = makeEpisode()

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = { clicked = true },
                    onPlay = {},
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Test Episode").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `downloaded badge shows when episode is downloaded`() {
        val episode = makeEpisode(isDownloaded = true)

        composeTestRule.setContent {
            MaterialTheme {
                EpisodeCard(
                    episode = episode,
                    downloadProgress = null,
                    onClick = {},
                    onPlay = {},
                    onDownload = {},
                    onDeleteDownload = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Downloaded").assertIsDisplayed()
    }
}
