package com.podbelly.feature.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.podbelly.core.database.dao.PodcastEngagementStat

/** Sort orders for the engagement list; all surface neglected shows first. */
internal enum class EngagementSort(val label: String) {
    LEAST_LISTENED("Least listened"),
    LEAST_PLAYED("Least played"),
    LONGEST_IDLE("Longest idle"),
}

/**
 * The Stats "Podcasts" tab: every subscription with its engagement metrics,
 * neglected shows first, with an inline unsubscribe (confirm dialog; the caller
 * shows the undo snackbar). Modeled on AntennaPod's per-subscription statistics
 * list, inverted to answer "what am I *not* listening to?".
 */
@Composable
internal fun PodcastEngagementTab(
    stats: List<PodcastEngagementStat>,
    onUnsubscribe: (PodcastEngagementStat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sort by rememberSaveable { mutableStateOf(EngagementSort.LEAST_LISTENED) }
    var confirmTarget by remember { mutableStateOf<PodcastEngagementStat?>(null) }

    val sortedStats = remember(stats, sort) {
        when (sort) {
            EngagementSort.LEAST_LISTENED ->
                stats.sortedBy { it.totalListenedMs }
            EngagementSort.LEAST_PLAYED ->
                stats.sortedBy { stat ->
                    if (stat.episodeCount == 0L) 0.0
                    else stat.playedCount.toDouble() / stat.episodeCount
                }
            EngagementSort.LONGEST_IDLE ->
                stats.sortedBy { it.lastListenedAt }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "engagement_intro") {
            Text(
                text = "Your subscriptions, least listened first — spot the shows worth pruning.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item(key = "engagement_sort") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EngagementSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }

        if (stats.isEmpty()) {
            item(key = "engagement_empty") {
                Text(
                    text = "No subscriptions yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        items(
            items = sortedStats,
            key = { it.podcastId },
        ) { stat ->
            PodcastEngagementCard(
                stat = stat,
                onUnsubscribeClick = { confirmTarget = stat },
            )
        }
    }

    confirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            title = { Text("Unsubscribe from ${target.podcastTitle}?") },
            text = {
                Text(
                    "Its episodes will leave your feed. Downloaded files stay on " +
                        "your device until you delete them.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmTarget = null
                        onUnsubscribe(target)
                    },
                ) {
                    Text("Unsubscribe", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
internal fun PodcastEngagementCard(
    stat: PodcastEngagementStat,
    onUnsubscribeClick: () -> Unit,
) {
    val podcastsFallback = rememberVectorPainter(Icons.Default.Podcasts)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = stat.artworkUrl.ifBlank { null },
                    contentDescription = "${stat.podcastTitle} artwork",
                    placeholder = podcastsFallback,
                    error = podcastsFallback,
                    fallback = podcastsFallback,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.podcastTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Subscribed ${relativeTime(stat.subscribedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onUnsubscribeClick) {
                    Icon(
                        imageVector = Icons.Outlined.PersonRemove,
                        contentDescription = "Unsubscribe from ${stat.podcastTitle}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val playedPercent = if (stat.episodeCount > 0L) {
                (stat.playedCount * 100 / stat.episodeCount).toInt()
            } else 0

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EngagementMetric(
                        label = "Listened",
                        value = if (stat.totalListenedMs > 0L) {
                            formatListenedMs(stat.totalListenedMs)
                        } else "Never",
                        highlight = stat.totalListenedMs == 0L,
                    )
                    EngagementMetric(
                        label = "Played",
                        value = "${stat.playedCount} of ${stat.episodeCount} ($playedPercent%)",
                    )
                    EngagementMetric(
                        label = "In progress",
                        value = "${stat.inProgressCount}",
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EngagementMetric(
                        label = "Last listened",
                        value = if (stat.lastListenedAt > 0L) {
                            relativeTime(stat.lastListenedAt)
                        } else "Never",
                        highlight = stat.lastListenedAt == 0L,
                    )
                    EngagementMetric(
                        label = "Newest episode",
                        value = if (stat.latestEpisodeAt > 0L) {
                            relativeTime(stat.latestEpisodeAt)
                        } else "—",
                    )
                    EngagementMetric(
                        label = "Downloads",
                        value = if (stat.downloadedCount > 0L) {
                            "${stat.downloadedCount} · ${formatBytes(stat.downloadedBytes)}"
                        } else "0",
                    )
                }
            }
        }
    }
}

@Composable
private fun EngagementMetric(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

/** "4h 32m" for >= 1h, "12m" below that (the metric grid is tighter than Overview's). */
internal fun formatListenedMs(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / 1024} KB"
}
