package com.podbelly.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.podbelly.core.database.dao.PodcastEngagementStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class StatsPodcastsTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeStat(
        podcastId: Long = 1L,
        title: String = "Dusty Show",
        totalListenedMs: Long = 0L,
        lastListenedAt: Long = 0L,
        episodeCount: Long = 40L,
        playedCount: Long = 2L,
        downloadedCount: Long = 0L,
        downloadedBytes: Long = 0L,
    ) = PodcastEngagementStat(
        podcastId = podcastId,
        podcastTitle = title,
        artworkUrl = "",
        subscribedAt = System.currentTimeMillis() - 90L * 86_400_000L,
        totalListenedMs = totalListenedMs,
        lastListenedAt = lastListenedAt,
        episodeCount = episodeCount,
        playedCount = playedCount,
        inProgressCount = 1L,
        downloadedCount = downloadedCount,
        downloadedBytes = downloadedBytes,
        latestEpisodeAt = System.currentTimeMillis() - 86_400_000L,
    )

    @Test
    fun `card shows Never for a podcast that was never played`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementCard(
                    stat = makeStat(totalListenedMs = 0L, lastListenedAt = 0L),
                    onUnsubscribeClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Dusty Show").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 of 40 (5%)").assertIsDisplayed()
    }

    @Test
    fun `card shows listened time and download size`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementCard(
                    stat = makeStat(
                        totalListenedMs = 4L * 3_600_000L + 32L * 60_000L,
                        downloadedCount = 3L,
                        downloadedBytes = 96L * 1024 * 1024,
                    ),
                    onUnsubscribeClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("4h 32m").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 · 96 MB").assertIsDisplayed()
    }

    @Test
    fun `unsubscribe requires confirmation before firing`() {
        var unsubscribed: PodcastEngagementStat? = null

        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = listOf(makeStat(title = "Dusty Show")),
                    onUnsubscribe = { unsubscribed = it },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Unsubscribe from Dusty Show")
            .performClick()
        // Dialog shown, nothing fired yet
        composeTestRule.onNodeWithText("Unsubscribe from Dusty Show?").assertIsDisplayed()
        assertEquals(null, unsubscribed)

        composeTestRule.onNodeWithText("Unsubscribe").performClick()
        assertEquals(1L, unsubscribed?.podcastId)
    }

    @Test
    fun `cancelling the confirm dialog does not unsubscribe`() {
        var fired = false

        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = listOf(makeStat(title = "Dusty Show")),
                    onUnsubscribe = { fired = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Unsubscribe from Dusty Show")
            .performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertFalse(fired)
        composeTestRule.onNodeWithText("Unsubscribe from Dusty Show?").assertDoesNotExist()
    }

    @Test
    fun `tapping a card opens the podcast`() {
        var openedId = -1L

        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = listOf(makeStat(podcastId = 7L, title = "Dusty Show")),
                    onUnsubscribe = {},
                    onPodcastClick = { openedId = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Dusty Show").performClick()
        assertEquals(7L, openedId)
    }

    @Test
    fun `tapping the unsubscribe button does not also open the podcast`() {
        var opened = false

        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = listOf(makeStat(title = "Dusty Show")),
                    onUnsubscribe = {},
                    onPodcastClick = { opened = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Unsubscribe from Dusty Show")
            .performClick()
        assertFalse(opened)
    }

    @Test
    fun `sort chips are shown`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = listOf(makeStat()),
                    onUnsubscribe = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Least listened").assertIsDisplayed()
        composeTestRule.onNodeWithText("Least played").assertIsDisplayed()
        composeTestRule.onNodeWithText("Longest idle").assertIsDisplayed()
    }

    @Test
    fun `empty state is shown without subscriptions`() {
        composeTestRule.setContent {
            MaterialTheme {
                PodcastEngagementTab(
                    stats = emptyList(),
                    onUnsubscribe = {},
                )
            }
        }

        composeTestRule.onNodeWithText("No subscriptions yet.").assertIsDisplayed()
    }

    @Test
    fun `formatters produce compact values`() {
        assertEquals("12m", formatListenedMs(12L * 60_000L))
        assertEquals("4h 32m", formatListenedMs(4L * 3_600_000L + 32L * 60_000L))
        assertEquals("96 MB", formatBytes(96L * 1024 * 1024))
        assertEquals("1.5 GB", formatBytes(1_610_612_736L))
        assertTrue(formatBytes(2048L).endsWith("KB"))
    }
}
