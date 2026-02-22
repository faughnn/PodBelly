package com.podbelly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.podbelly.core.database.entity.PodcastEntity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class LibraryScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makePodcast(
        id: Long = 1L,
        title: String = "Test Podcast",
        author: String = "Author",
        artworkUrl: String = "",
        episodeCount: Int = 10,
    ) = PodcastEntity(
        id = id,
        feedUrl = "https://example.com/feed$id.xml",
        title = title,
        author = author,
        description = "Description",
        artworkUrl = artworkUrl,
        link = "https://example.com",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
        episodeCount = episodeCount,
    )

    @Test
    fun `grid item shows podcast title`() {
        val podcast = makePodcast(title = "My Favorite Show")

        composeTestRule.setContent {
            MaterialTheme {
                PodcastGridItem(
                    podcast = podcast,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("My Favorite Show").assertIsDisplayed()
    }

    @Test
    fun `grid item click triggers callback`() {
        var clicked = false
        val podcast = makePodcast()

        composeTestRule.setContent {
            MaterialTheme {
                PodcastGridItem(
                    podcast = podcast,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Test Podcast").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `list row shows podcast title and author`() {
        val podcast = makePodcast(title = "Great Podcast", author = "Jane Doe", episodeCount = 42)

        composeTestRule.setContent {
            MaterialTheme {
                PodcastListRow(
                    podcast = podcast,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Great Podcast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("42 episodes").assertIsDisplayed()
    }

    @Test
    fun `list row click triggers callback`() {
        var clicked = false
        val podcast = makePodcast()

        composeTestRule.setContent {
            MaterialTheme {
                PodcastListRow(
                    podcast = podcast,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Test Podcast").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `list row hides author when blank`() {
        val podcast = makePodcast(author = "")

        composeTestRule.setContent {
            MaterialTheme {
                PodcastListRow(
                    podcast = podcast,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Podcast").assertIsDisplayed()
        // Author text should not appear
        composeTestRule.onNodeWithText("Author").assertDoesNotExist()
    }
}
