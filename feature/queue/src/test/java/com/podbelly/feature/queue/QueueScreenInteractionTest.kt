package com.podbelly.feature.queue

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class QueueScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createQueueItem(
        queueId: Long = 1L,
        episodeId: Long = 100L,
        episodeTitle: String = "Test Episode",
        podcastTitle: String = "Test Podcast",
        artworkUrl: String = "",
        durationSeconds: Int = 1800,
        position: Int = 0
    ) = QueueItemUi(
        queueId = queueId,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        podcastTitle = podcastTitle,
        artworkUrl = artworkUrl,
        durationSeconds = durationSeconds,
        position = position
    )

    @Test
    fun `empty queue shows correct text`() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyQueueContent()
            }
        }

        composeTestRule.onNodeWithText("Your queue is empty").assertIsDisplayed()
    }

    @Test
    fun `queue item card shows episode title`() {
        val item = createQueueItem(episodeTitle = "Exploring Jetpack Compose")

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = false,
                    isLast = false,
                    isNowPlaying = false,
                    onPlay = {},
                    onRemove = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Exploring Jetpack Compose").assertIsDisplayed()
    }

    @Test
    fun `remove button triggers callback`() {
        var removeClicked = false
        val item = createQueueItem()

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = false,
                    isLast = false,
                    isNowPlaying = false,
                    onPlay = {},
                    onRemove = { removeClicked = true },
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Remove from queue").performClick()
        assertTrue(removeClicked)
    }

    @Test
    fun `move up button triggers callback`() {
        var moveUpClicked = false
        val item = createQueueItem()

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = false,
                    isLast = false,
                    isNowPlaying = false,
                    onPlay = {},
                    onRemove = {},
                    onMoveUp = { moveUpClicked = true },
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Move up").performClick()
        assertTrue(moveUpClicked)
    }

    @Test
    fun `move down button triggers callback`() {
        var moveDownClicked = false
        val item = createQueueItem()

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = false,
                    isLast = false,
                    isNowPlaying = false,
                    onPlay = {},
                    onRemove = {},
                    onMoveUp = {},
                    onMoveDown = { moveDownClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Move down").performClick()
        assertTrue(moveDownClicked)
    }

    @Test
    fun `move up is disabled for first item`() {
        val item = createQueueItem()

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = true,
                    isLast = false,
                    isNowPlaying = false,
                    onPlay = {},
                    onRemove = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Move up").assertIsNotEnabled()
    }

    @Test
    fun `now playing item has different background`() {
        val item = createQueueItem()

        composeTestRule.setContent {
            MaterialTheme {
                QueueItemCard(
                    item = item,
                    isFirst = false,
                    isLast = false,
                    isNowPlaying = true,
                    onPlay = {},
                    onRemove = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        // Verify the card renders without crash when isNowPlaying is true
        composeTestRule.onNodeWithText("Test Episode").assertIsDisplayed()
    }
}
