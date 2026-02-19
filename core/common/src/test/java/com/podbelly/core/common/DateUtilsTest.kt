package com.podbelly.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateUtilsTest {

    // -------------------------------------------------------------------------
    // formatDuration(Long)
    // -------------------------------------------------------------------------

    @Test
    fun `formatDuration with zero seconds`() {
        assertEquals("0m", DateUtils.formatDuration(0L))
    }

    @Test
    fun `formatDuration with seconds less than a minute`() {
        // 45 seconds -> 0 minutes
        assertEquals("0m", DateUtils.formatDuration(45L))
    }

    @Test
    fun `formatDuration with exact minutes`() {
        // 120 seconds = 2 minutes
        assertEquals("2m", DateUtils.formatDuration(120L))
    }

    @Test
    fun `formatDuration with minutes and leftover seconds`() {
        // 125 seconds = 2 minutes 5 seconds -> "2m" (seconds truncated)
        assertEquals("2m", DateUtils.formatDuration(125L))
    }

    @Test
    fun `formatDuration with hours and minutes`() {
        // 3723 seconds = 1h 2m 3s -> "1h 2m"
        assertEquals("1h 2m", DateUtils.formatDuration(3723L))
    }

    @Test
    fun `formatDuration with exact hours`() {
        // 7200 seconds = 2h 0m
        assertEquals("2h 0m", DateUtils.formatDuration(7200L))
    }

    @Test
    fun `formatDuration with large value`() {
        // 86400 seconds = 24h 0m
        assertEquals("24h 0m", DateUtils.formatDuration(86400L))
    }

    @Test
    fun `formatDuration with negative value returns 0m`() {
        assertEquals("0m", DateUtils.formatDuration(-100L))
    }

    // -------------------------------------------------------------------------
    // formatDuration(Int) overload
    // -------------------------------------------------------------------------

    @Test
    fun `formatDuration Int overload delegates correctly`() {
        assertEquals("1h 2m", DateUtils.formatDuration(3723))
        assertEquals("0m", DateUtils.formatDuration(0))
    }

    // -------------------------------------------------------------------------
    // formatDurationMillis
    // -------------------------------------------------------------------------

    @Test
    fun `formatDurationMillis converts millis to seconds then formats`() {
        // 3723000 ms = 3723 seconds = 1h 2m
        assertEquals("1h 2m", DateUtils.formatDurationMillis(3723000L))
    }

    @Test
    fun `formatDurationMillis with zero`() {
        assertEquals("0m", DateUtils.formatDurationMillis(0L))
    }

    @Test
    fun `formatDurationMillis with sub-second millis`() {
        // 999 ms -> 0 seconds -> "0m"
        assertEquals("0m", DateUtils.formatDurationMillis(999L))
    }

    @Test
    fun `formatDurationMillis with exact minute in millis`() {
        // 60000 ms = 60 seconds = 1 minute
        assertEquals("1m", DateUtils.formatDurationMillis(60000L))
    }

    // -------------------------------------------------------------------------
    // relativeDate
    // -------------------------------------------------------------------------

    @Test
    fun `relativeDate just now for less than 60 seconds ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val thirtySecondsAgo = now.minusSeconds(30)

        assertEquals("Just now", DateUtils.relativeDate(thirtySecondsAgo, now))
    }

    @Test
    fun `relativeDate 1 minute ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val oneMinuteAgo = now.minusSeconds(60)

        assertEquals("1 minute ago", DateUtils.relativeDate(oneMinuteAgo, now))
    }

    @Test
    fun `relativeDate multiple minutes ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val fiveMinutesAgo = now.minusSeconds(5 * 60)

        assertEquals("5 minutes ago", DateUtils.relativeDate(fiveMinutesAgo, now))
    }

    @Test
    fun `relativeDate 1 hour ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val oneHourAgo = now.minusSeconds(3600)

        assertEquals("1 hour ago", DateUtils.relativeDate(oneHourAgo, now))
    }

    @Test
    fun `relativeDate multiple hours ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val threeHoursAgo = now.minusSeconds(3 * 3600)

        assertEquals("3 hours ago", DateUtils.relativeDate(threeHoursAgo, now))
    }

    @Test
    fun `relativeDate yesterday`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val yesterday = now.minusSeconds(36 * 3600) // 36 hours ago to ensure previous calendar day

        assertEquals("Yesterday", DateUtils.relativeDate(yesterday, now))
    }

    @Test
    fun `relativeDate days ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val threeDaysAgo = now.minusSeconds(3 * 24 * 3600L)

        assertEquals("3 days ago", DateUtils.relativeDate(threeDaysAgo, now))
    }

    @Test
    fun `relativeDate last week`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val tenDaysAgo = now.minusSeconds(10 * 24 * 3600L)

        assertEquals("Last week", DateUtils.relativeDate(tenDaysAgo, now))
    }

    @Test
    fun `relativeDate weeks ago`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val twentyDaysAgo = now.minusSeconds(20 * 24 * 3600L)

        // 20 days / 7 = 2 weeks
        assertEquals("2 weeks ago", DateUtils.relativeDate(twentyDaysAgo, now))
    }

    @Test
    fun `relativeDate absolute date for more than 30 days`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val sixtyDaysAgo = now.minusSeconds(60 * 24 * 3600L)

        // Should return an absolute date like "Apr 16, 2025"
        val result = DateUtils.relativeDate(sixtyDaysAgo, now)
        // The result should be formatted as "MMM d, yyyy"
        // The exact date depends on timezone but should contain a year
        assertTrue(
            "Absolute date should contain year: got '$result'",
            result.contains("2025")
        )
    }

    @Test
    fun `relativeDate with future date returns absolute date`() {
        val now = Instant.parse("2025-06-15T12:00:00Z")
        val future = now.plusSeconds(24 * 3600)

        val result = DateUtils.relativeDate(future, now)
        // Future dates should render as absolute date
        assertTrue(
            "Future date should return absolute date: got '$result'",
            result.contains("2025")
        )
    }

    // -------------------------------------------------------------------------
    // relativeDate(Long, Long) overload
    // -------------------------------------------------------------------------

    @Test
    fun `relativeDate epoch millis overload works correctly`() {
        val nowMillis = Instant.parse("2025-06-15T12:00:00Z").toEpochMilli()
        val thirtySecondsAgoMillis = nowMillis - 30_000L

        assertEquals("Just now", DateUtils.relativeDate(thirtySecondsAgoMillis, nowMillis))
    }
}
