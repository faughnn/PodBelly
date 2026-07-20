package com.podbelly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
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
@Config(sdk = [33])
class VersionHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `changelog data is consistent`() {
        assertTrue(WhatsNew.changelog.isNotEmpty())
        // The dialog gate can never point below the newest documented release.
        assertTrue(WhatsNew.LATEST_VERSION_CODE >= WhatsNew.changelog.keys.max())
        // Every release has at least one user-facing note.
        WhatsNew.changelog.forEach { (code, notes) ->
            assertTrue("version $code has no notes", notes.isNotEmpty())
        }
    }

    @Test
    fun `versionTitle marks only the installed release with its name`() {
        assertEquals(
            "v1.6.30 · current",
            versionTitle(versionCode = 78, currentVersionName = "1.6.30", currentVersionCode = 78),
        )
        assertEquals(
            "Version 71",
            versionTitle(versionCode = 71, currentVersionName = "1.6.30", currentVersionCode = 78),
        )
        // Unknown installed version (e.g. lookup failed): no false "current" badge.
        assertEquals(
            "Version 78",
            versionTitle(versionCode = 78, currentVersionName = "", currentVersionCode = 78),
        )
    }

    @Test
    fun `screen lists the newest release first with its notes`() {
        var backClicked = false
        composeTestRule.setContent {
            MaterialTheme {
                VersionHistoryScreen(onNavigateBack = { backClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Version history").assertIsDisplayed()

        val newestNote = WhatsNew.changelog.entries.maxBy { it.key }.value.first()
        composeTestRule.onNodeWithText("•  $newestNote").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assertTrue(backClicked)
    }
}
