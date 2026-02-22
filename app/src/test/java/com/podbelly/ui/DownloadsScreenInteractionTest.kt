package com.podbelly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.podbelly.core.database.dao.DownloadErrorWithEpisode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class DownloadsScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeError(
        id: Long = 1L,
        episodeId: Long = 10L,
        episodeTitle: String = "Failed Episode",
        errorMessage: String = "HTTP 500",
        errorCode: Int = 500,
        retryCount: Int = 0,
    ) = DownloadErrorWithEpisode(
        id = id,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        errorMessage = errorMessage,
        errorCode = errorCode,
        timestamp = 1700000000000L,
        retryCount = retryCount,
    )

    @Test
    fun `failed downloads header shows count`() {
        composeTestRule.setContent {
            MaterialTheme {
                FailedDownloadsHeader(
                    count = 3,
                    onClearAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Failed Downloads (3)").assertIsDisplayed()
    }

    @Test
    fun `clear all button triggers callback`() {
        var clearAllClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                FailedDownloadsHeader(
                    count = 2,
                    onClearAll = { clearAllClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear all errors").performClick()
        assertTrue(clearAllClicked)
    }

    @Test
    fun `failed download card shows episode title and error message`() {
        val error = makeError(episodeTitle = "Broken Episode", errorMessage = "Connection timeout")

        composeTestRule.setContent {
            MaterialTheme {
                FailedDownloadCard(
                    error = error,
                    onRetry = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Broken Episode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection timeout").assertIsDisplayed()
    }

    @Test
    fun `retry button triggers callback`() {
        var retryClicked = false
        val error = makeError()

        composeTestRule.setContent {
            MaterialTheme {
                FailedDownloadCard(
                    error = error,
                    onRetry = { retryClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Retry download").performClick()
        assertTrue(retryClicked)
    }
}
