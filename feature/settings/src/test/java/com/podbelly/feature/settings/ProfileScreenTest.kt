package com.podbelly.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fromKey resolves every section and falls back to PLAYBACK`() {
        SettingsSection.entries.forEach { section ->
            assertEquals(section, SettingsSection.fromKey(section.key))
        }
        assertEquals(SettingsSection.PLAYBACK, SettingsSection.fromKey("nonsense"))
        assertEquals(SettingsSection.PLAYBACK, SettingsSection.fromKey(null))
    }

    @Test
    fun `listening summary shows stats and opens full stats on tap`() {
        var opened = false
        composeTestRule.setContent {
            MaterialTheme {
                ListeningSummaryCard(
                    stats = StatsUiState(
                        listenedTodayMs = 60 * 60_000L,
                        listenedThisWeekMs = 5 * 60 * 60_000L,
                        currentStreak = 4,
                    ),
                    onClick = { opened = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("1h 0m").assertIsDisplayed()
        composeTestRule.onNodeWithText("This week").assertIsDisplayed()
        composeTestRule.onNodeWithText("5h 0m").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 days").assertIsDisplayed()

        composeTestRule.onNodeWithText("Your listening").performClick()
        assertTrue(opened)
    }

    @Test
    fun `category row shows title and subtitle and fires click`() {
        var clicked: SettingsSection? = null
        composeTestRule.setContent {
            MaterialTheme {
                SettingsCategoryRow(
                    section = SettingsSection.DOWNLOADS,
                    onClick = { clicked = SettingsSection.DOWNLOADS },
                )
            }
        }

        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto-download, cleanup, storage").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloads").performClick()
        assertEquals(SettingsSection.DOWNLOADS, clicked)
    }
}
