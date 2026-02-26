package com.podbelly.feature.discover

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class DiscoverScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createPodcastItem(
        title: String = "Test Podcast",
        author: String = "Test Author",
        artworkUrl: String = "",
        feedUrl: String = "https://example.com/feed.xml",
        isSubscribed: Boolean = false
    ) = DiscoverPodcastItem(
        title = title,
        author = author,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        isSubscribed = isSubscribed
    )

    @Test
    fun `empty state shows search prompt`() {
        composeTestRule.setContent {
            MaterialTheme {
                DiscoverEmptyState()
            }
        }

        composeTestRule.onNodeWithText("Search for podcasts or add an RSS feed URL")
            .assertIsDisplayed()
    }

    @Test
    fun `search field accepts input`() {
        var currentQuery = ""

        composeTestRule.setContent {
            MaterialTheme {
                SearchSection(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it },
                    onSearch = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Search podcasts...").performTextInput("kotlin")
        assertEquals("kotlin", currentQuery)
    }

    @Test
    fun `subscribe button in search results triggers callback`() {
        var subscribeClicked = false
        val item = createPodcastItem(isSubscribed = false)

        composeTestRule.setContent {
            MaterialTheme {
                SearchResultItem(
                    item = item,
                    isSubscribing = false,
                    onSubscribe = { subscribeClicked = true },
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Subscribe").performClick()
        assertTrue(subscribeClicked)
    }

    @Test
    fun `subscribed item shows check mark and disabled button`() {
        val item = createPodcastItem(isSubscribed = true)

        composeTestRule.setContent {
            MaterialTheme {
                SearchResultItem(
                    item = item,
                    isSubscribing = false,
                    onSubscribe = {},
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Subscribed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subscribed").assertIsNotEnabled()
    }

    @Test
    fun `RSS subscribe button is disabled when URL is blank`() {
        composeTestRule.setContent {
            MaterialTheme {
                RssUrlSection(
                    feedUrl = "",
                    onFeedUrlChange = {},
                    onSubscribe = {},
                    isSubscribing = false
                )
            }
        }

        composeTestRule.onNodeWithText("Subscribe").assertIsNotEnabled()
    }
}
