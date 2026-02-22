package com.podbelly.feature.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.podbelly.core.playback.Chapter
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PlayerScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -- MainControls tests --

    @Test
    fun `play button shows Play when not playing`() {
        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = false,
                    hasPreviousChapter = false,
                    hasNextChapter = false,
                    onSkipBack = {},
                    onTogglePlayPause = {},
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun `play button shows Pause when playing`() {
        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = true,
                    hasChapters = false,
                    hasPreviousChapter = false,
                    hasNextChapter = false,
                    onSkipBack = {},
                    onTogglePlayPause = {},
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun `play pause button triggers callback`() {
        var toggled = false

        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = false,
                    hasPreviousChapter = false,
                    hasNextChapter = false,
                    onSkipBack = {},
                    onTogglePlayPause = { toggled = true },
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(toggled)
    }

    @Test
    fun `skip back button triggers callback`() {
        var skippedBack = false

        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = false,
                    hasPreviousChapter = false,
                    hasNextChapter = false,
                    onSkipBack = { skippedBack = true },
                    onTogglePlayPause = {},
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Skip back 10 seconds").performClick()
        assertTrue(skippedBack)
    }

    @Test
    fun `skip forward button triggers callback`() {
        var skippedForward = false

        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = false,
                    hasPreviousChapter = false,
                    hasNextChapter = false,
                    onSkipBack = {},
                    onTogglePlayPause = {},
                    onSkipForward = { skippedForward = true },
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Skip forward 30 seconds").performClick()
        assertTrue(skippedForward)
    }

    @Test
    fun `chapter buttons appear when chapters exist`() {
        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = true,
                    hasPreviousChapter = true,
                    hasNextChapter = true,
                    onSkipBack = {},
                    onTogglePlayPause = {},
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Previous chapter").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next chapter").assertIsDisplayed()
    }

    @Test
    fun `previous chapter disabled when no previous`() {
        composeTestRule.setContent {
            MaterialTheme {
                MainControls(
                    isPlaying = false,
                    hasChapters = true,
                    hasPreviousChapter = false,
                    hasNextChapter = true,
                    onSkipBack = {},
                    onTogglePlayPause = {},
                    onSkipForward = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Previous chapter").assertIsNotEnabled()
    }

    // -- SecondaryControls tests --

    @Test
    fun `speed button shows current speed`() {
        composeTestRule.setContent {
            MaterialTheme {
                SecondaryControls(
                    playbackSpeed = 1.5f,
                    sleepTimerRemaining = 0L,
                    isSleepTimerActive = false,
                    skipSilence = false,
                    volumeBoost = false,
                    hasChapters = false,
                    onSpeedClick = {},
                    onSleepTimerClick = {},
                    onToggleSkipSilence = {},
                    onToggleVolumeBoost = {},
                    onChaptersClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("1.5x").assertIsDisplayed()
    }

    @Test
    fun `sleep timer shows Sleep when inactive`() {
        composeTestRule.setContent {
            MaterialTheme {
                SecondaryControls(
                    playbackSpeed = 1.0f,
                    sleepTimerRemaining = 0L,
                    isSleepTimerActive = false,
                    skipSilence = false,
                    volumeBoost = false,
                    hasChapters = false,
                    onSpeedClick = {},
                    onSleepTimerClick = {},
                    onToggleSkipSilence = {},
                    onToggleVolumeBoost = {},
                    onChaptersClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sleep").assertIsDisplayed()
    }

    @Test
    fun `skip silence toggle triggers callback`() {
        var toggled = false

        composeTestRule.setContent {
            MaterialTheme {
                SecondaryControls(
                    playbackSpeed = 1.0f,
                    sleepTimerRemaining = 0L,
                    isSleepTimerActive = false,
                    skipSilence = false,
                    volumeBoost = false,
                    hasChapters = false,
                    onSpeedClick = {},
                    onSleepTimerClick = {},
                    onToggleSkipSilence = { toggled = true },
                    onToggleVolumeBoost = {},
                    onChaptersClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Enable skip silence").performClick()
        assertTrue(toggled)
    }

    @Test
    fun `volume boost toggle triggers callback`() {
        var toggled = false

        composeTestRule.setContent {
            MaterialTheme {
                SecondaryControls(
                    playbackSpeed = 1.0f,
                    sleepTimerRemaining = 0L,
                    isSleepTimerActive = false,
                    skipSilence = false,
                    volumeBoost = false,
                    hasChapters = false,
                    onSpeedClick = {},
                    onSleepTimerClick = {},
                    onToggleSkipSilence = {},
                    onToggleVolumeBoost = { toggled = true },
                    onChaptersClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Enable volume boost").performClick()
        assertTrue(toggled)
    }

    // -- SpeedPickerContent tests --

    @Test
    fun `speed picker shows all speed options`() {
        composeTestRule.setContent {
            MaterialTheme {
                SpeedPickerContent(
                    currentSpeed = 1.0f,
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Playback Speed").assertIsDisplayed()
        // 1.0x appears in both the preset button and the current speed display
        composeTestRule.onAllNodesWithText("1.0x")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("1.5x").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.0x").assertIsDisplayed()
    }

    @Test
    fun `speed picker shows podcast title when provided`() {
        composeTestRule.setContent {
            MaterialTheme {
                SpeedPickerContent(
                    currentSpeed = 1.0f,
                    podcastTitle = "My Show",
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Speed for My Show").assertIsDisplayed()
    }

    // -- SleepTimerPickerContent tests --

    @Test
    fun `sleep timer picker shows all options`() {
        composeTestRule.setContent {
            MaterialTheme {
                SleepTimerPickerContent(
                    isSleepTimerActive = false,
                    sleepTimerRemaining = 0L,
                    onSelect = {},
                    onEndOfEpisode = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sleep Timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 minutes").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 minutes").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 minutes").assertIsDisplayed()
    }

    @Test
    fun `sleep timer picker shows turn off when active`() {
        composeTestRule.setContent {
            MaterialTheme {
                SleepTimerPickerContent(
                    isSleepTimerActive = true,
                    sleepTimerRemaining = 120000L,
                    onSelect = {},
                    onEndOfEpisode = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("2:00 remaining").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 minutes").assertIsDisplayed()
    }

    // -- ChaptersListContent tests --

    @Test
    fun `chapters list shows chapter titles`() {
        val chapters = listOf(
            Chapter("Introduction", 0L, 60000L),
            Chapter("Main Topic", 60000L, 180000L),
            Chapter("Conclusion", 180000L, 240000L),
        )

        composeTestRule.setContent {
            MaterialTheme {
                ChaptersListContent(
                    chapters = chapters,
                    currentChapterIndex = 1,
                    onChapterClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Chapters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Introduction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Main Topic").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conclusion").assertIsDisplayed()
    }

    @Test
    fun `chapters list click triggers callback`() {
        var clickedIndex = -1
        val chapters = listOf(
            Chapter("Chapter 1", 0L, 60000L),
            Chapter("Chapter 2", 60000L, 120000L),
        )

        composeTestRule.setContent {
            MaterialTheme {
                ChaptersListContent(
                    chapters = chapters,
                    currentChapterIndex = 0,
                    onChapterClick = { clickedIndex = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Chapter 2").performClick()
        assertTrue(clickedIndex == 1)
    }
}
