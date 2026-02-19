package com.podbelly.core.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    /**
     * Formats a duration given in **seconds** into a human-readable string.
     *
     * Examples:
     * - 0        -> "0m"
     * - 45       -> "1m"
     * - 125      -> "2m"
     * - 3723     -> "1h 2m"
     * - 7200     -> "2h 0m"
     * - 86400    -> "24h 0m"
     */
    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds < 0) return "0m"

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Overload accepting an [Int] for convenience.
     */
    fun formatDuration(totalSeconds: Int): String = formatDuration(totalSeconds.toLong())

    /**
     * Formats a duration given in **milliseconds** into a human-readable string.
     *
     * Delegates to [formatDuration] after converting to seconds.
     */
    fun formatDurationMillis(millis: Long): String = formatDuration(millis / 1000)

    /**
     * Returns a human-readable relative date string for the given [Instant].
     *
     * Examples:
     * - Just now         (< 1 minute ago)
     * - 5 minutes ago    (< 1 hour ago)
     * - 2 hours ago      (< 24 hours ago, same calendar day or close)
     * - Yesterday
     * - 3 days ago       (2-6 days)
     * - Last week        (7-13 days)
     * - 2 weeks ago      (14-29 days)
     * - Feb 3, 2025      (>= 30 days)
     */
    fun relativeDate(instant: Instant, now: Instant = Instant.now()): String {
        val duration = Duration.between(instant, now)

        // Future dates -- just show the absolute date.
        if (duration.isNegative) {
            return formatAbsoluteDate(instant)
        }

        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> {
                if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            }
            hours < 24 -> {
                if (hours == 1L) "1 hour ago" else "$hours hours ago"
            }
            else -> {
                val nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate()
                val thenDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val daysBetween = ChronoUnit.DAYS.between(thenDate, nowDate)

                when {
                    daysBetween == 1L -> "Yesterday"
                    daysBetween in 2..6 -> "$daysBetween days ago"
                    daysBetween in 7..13 -> "Last week"
                    daysBetween in 14..29 -> "${daysBetween / 7} weeks ago"
                    else -> formatAbsoluteDate(instant)
                }
            }
        }
    }

    /**
     * Overload accepting epoch millis for convenience.
     */
    fun relativeDate(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        return relativeDate(Instant.ofEpochMilli(epochMillis), Instant.ofEpochMilli(nowMillis))
    }

    // -- Private helpers --------------------------------------------------

    private val absoluteDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy")

    private fun formatAbsoluteDate(instant: Instant): String {
        val localDate: LocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.format(absoluteDateFormatter)
    }
}
