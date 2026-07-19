package com.podbelly.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.podbelly.core.database.dao.DailyListeningStat
import com.podbelly.core.database.dao.DayOfWeekStat
import com.podbelly.core.database.dao.HourOfDayStat

/**
 * A row of bottom-aligned bars, normalized to the largest value. The peak bar is
 * tinted primary; the rest use a muted tone. Values of 0 render a hairline stub so
 * the axis stays readable.
 */
@Composable
internal fun BarRow(
    values: List<Long>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 72,
) {
    val maxValue = values.maxOrNull() ?: 0L
    val max = maxValue.takeIf { it > 0L } ?: 1L
    val maxIndex = if (maxValue > 0L) values.indexOf(maxValue) else -1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value.toFloat() / max).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceAtLeast(0.03f))
                    .background(
                        color = if (index == maxIndex && value > 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                    ),
            )
        }
    }
}

/**
 * Buckets [stats] (sparse, keyed by local epoch day) into the trailing [days]
 * days ending today, oldest first. Missing days become 0.
 */
internal fun bucketDailyListening(
    stats: List<DailyListeningStat>,
    todayEpochDay: Long,
    days: Int = 30,
): List<Long> {
    val byDay = stats.associate { it.epochDay to it.totalListenedMs }
    return ((todayEpochDay - days + 1)..todayEpochDay).map { byDay[it] ?: 0L }
}

/** Buckets day-of-week stats into Monday..Sunday order (dayOfWeek 0 = Monday). */
internal fun bucketDayOfWeek(stats: List<DayOfWeekStat>): List<Long> {
    val byDay = stats.associate { it.dayOfWeek to it.totalListenedMs }
    return (0..6).map { byDay[it] ?: 0L }
}

/** Buckets hour-of-day stats into 0..23 order. */
internal fun bucketHourOfDay(stats: List<HourOfDayStat>): List<Long> {
    val byHour = stats.associate { it.hour to it.totalListenedMs }
    return (0..23).map { byHour[it] ?: 0L }
}

@Composable
internal fun DayOfWeekChart(
    stats: List<DayOfWeekStat>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BarRow(values = bucketDayOfWeek(stats))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun HourOfDayChart(
    stats: List<HourOfDayStat>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BarRow(values = bucketHourOfDay(stats))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
