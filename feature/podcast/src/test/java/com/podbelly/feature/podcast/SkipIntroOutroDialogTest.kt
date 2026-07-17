package com.podbelly.feature.podcast

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.podbelly.core.common.SkipIntroOutroDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SkipIntroOutroDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `save persists the typed seconds`() {
        var intro = -1
        var outro = -1

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 30,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = { intro = it },
                    onSetSkipOutro = { outro = it },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("75")
        composeTestRule.onAllNodes(hasSetTextAction())[1].performTextReplacement("120")
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(75, intro)
        assertEquals(120, outro)
    }

    @Test
    fun `blank field saves as zero`() {
        var intro = -1

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 30,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = { intro = it },
                    onSetSkipOutro = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("")
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(0, intro)
    }

    @Test
    fun `non-digit characters are filtered from the input`() {
        var intro = -1

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 0,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = { intro = it },
                    onSetSkipOutro = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("1a2s")
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(12, intro)
    }

    @Test
    fun `cancel does not persist anything`() {
        var saved = false

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 0,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = { saved = true },
                    onSetSkipOutro = { saved = true },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("42")
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(false, saved)
    }

    @Test
    fun `position shortcuts are hidden without a playback position`() {
        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 0,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = {},
                    onSetSkipOutro = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Up to now", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("After now", substring = true).assertDoesNotExist()
    }

    @Test
    fun `up to now fills the intro from the paused position`() {
        var intro = -1

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 0,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = { intro = it },
                    onSetSkipOutro = {},
                    onDismiss = {},
                    currentPositionSeconds = 131,
                    remainingSeconds = 2023,
                )
            }
        }

        composeTestRule.onNodeWithText("Up to now (2:11)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Up to now (2:11)").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(131, intro)
    }

    @Test
    fun `after now fills the outro with the time remaining`() {
        var outro = -1

        composeTestRule.setContent {
            MaterialTheme {
                SkipIntroOutroDialog(
                    skipIntroSeconds = 0,
                    skipOutroSeconds = 0,
                    onSetSkipIntro = {},
                    onSetSkipOutro = { outro = it },
                    onDismiss = {},
                    currentPositionSeconds = 131,
                    remainingSeconds = 2023,
                )
            }
        }

        composeTestRule.onNodeWithText("After now (33:43)").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(2023, outro)
    }
}
