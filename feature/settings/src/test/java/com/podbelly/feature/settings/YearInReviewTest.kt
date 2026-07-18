package com.podbelly.feature.settings

import com.podbelly.core.database.dao.PodcastListeningStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YearInReviewTest {

    private fun stats(
        totalListenedMs: Long = 0L,
        daysListened: Int = 0,
        longestStreak: Int = 0,
        timeSavedBySpeedMs: Long = 0L,
        skipSavedMs: Long = 0L,
        finishedEpisodes: Int = 0,
        topShow: String? = null,
    ) = StatsUiState(
        totalListenedMs = totalListenedMs,
        daysListened = daysListened,
        longestStreak = longestStreak,
        timeSavedBySpeedMs = timeSavedBySpeedMs,
        skipSavedMs = skipSavedMs,
        finishedEpisodes = finishedEpisodes,
        mostListenedPodcasts = topShow?.let {
            listOf(PodcastListeningStat(1L, it, "", totalListenedMs, 10L))
        } ?: emptyList(),
    )

    @Test
    fun `share text includes every populated highlight`() {
        val text = buildYearReviewText(
            stats(
                totalListenedMs = 90_000_000L, // 25h 0m
                daysListened = 214,
                longestStreak = 32,
                timeSavedBySpeedMs = 3_600_000L,
                skipSavedMs = 1_800_000L,
                finishedEpisodes = 187,
                topShow = "The Rest Is History",
            ),
            year = 2026,
        )

        assertTrue(text.startsWith("My 2026 in podcasts"))
        assertTrue(text.contains("25h 0m listened"))
        assertTrue(text.contains("Top show: The Rest Is History"))
        assertTrue(text.contains("214 days with a podcast"))
        assertTrue(text.contains("Longest streak: 32 days"))
        // 1h + 0.5h saved
        assertTrue(text.contains("1h 30m saved by speed & skips"))
        assertTrue(text.contains("187 episodes finished"))
        assertTrue(text.endsWith("Tracked with Podbelly"))
    }

    @Test
    fun `share text omits empty highlights`() {
        val text = buildYearReviewText(
            stats(totalListenedMs = 60_000L),
            year = 2026,
        )

        assertFalse(text.contains("Top show"))
        assertFalse(text.contains("days with a podcast"))
        assertFalse(text.contains("streak"))
        assertFalse(text.contains("saved"))
        assertFalse(text.contains("finished"))
        // Exactly the header, the listened line, and the footer remain.
        assertEquals(3, text.lines().size)
    }

    @Test
    fun `a one-day streak is not worth bragging about`() {
        val text = buildYearReviewText(
            stats(totalListenedMs = 60_000L, longestStreak = 1),
            year = 2026,
        )

        assertFalse(text.contains("streak"))
    }
}
