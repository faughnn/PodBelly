package com.podbelly.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.podbelly.core.common.DarkThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SettingsScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `switch row shows title and subtitle`() {
        composeTestRule.setContent {
            MaterialTheme {
                SwitchRow(
                    title = "Skip silence",
                    subtitle = "Automatically skip silent sections",
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Skip silence").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatically skip silent sections").assertIsDisplayed()
    }

    @Test
    fun `switch row click toggles state`() {
        var lastValue: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                SwitchRow(
                    title = "Volume boost",
                    checked = false,
                    onCheckedChange = { lastValue = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Volume boost").performClick()
        assertEquals(true, lastValue)
    }

    @Test
    fun `theme picker shows all three options`() {
        composeTestRule.setContent {
            MaterialTheme {
                ThemePickerRow(
                    selectedMode = DarkThemeMode.SYSTEM,
                    onModeSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("System default").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun `theme picker selects correct option`() {
        composeTestRule.setContent {
            MaterialTheme {
                ThemePickerRow(
                    selectedMode = DarkThemeMode.DARK,
                    onModeSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dark").assertIsSelected()
    }

    @Test
    fun `playback speed shows current speed`() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackSpeedRow(
                    currentSpeed = 1.5f,
                    onSpeedChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("1.5x").assertIsDisplayed()
    }
}
