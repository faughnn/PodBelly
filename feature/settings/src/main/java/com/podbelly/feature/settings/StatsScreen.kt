package com.podbelly.feature.settings

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playback Statistics",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Summary Cards ────────────────────────────────────────

            item {
                StatsSummaryCard(
                    title = "Total Listening Time",
                    value = formatDurationMs(uiState.totalListenedMs),
                )
            }

            item {
                StatsSummaryCard(
                    title = "Time Saved by Speed",
                    value = formatDurationMs(uiState.timeSavedBySpeedMs),
                )
            }

            item {
                StatsSummaryCard(
                    title = "Time Saved by Silence",
                    value = formatDurationMs(uiState.silenceTrimmedMs),
                )
            }

            // ── Streaks ────────────────────────────────────────────
            if (uiState.currentStreak > 0 || uiState.longestStreak > 0) {
                item { SectionHeader(title = "Streaks") }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatsSummaryCard(
                            title = "Current Streak",
                            value = "${uiState.currentStreak} day${if (uiState.currentStreak != 1) "s" else ""}",
                            modifier = Modifier.weight(1f),
                        )
                        StatsSummaryCard(
                            title = "Longest Streak",
                            value = "${uiState.longestStreak} day${if (uiState.longestStreak != 1) "s" else ""}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Recent Activity ────────────────────────────────────
            if (uiState.listenedThisWeekMs > 0 || uiState.listenedThisMonthMs > 0) {
                item { SectionHeader(title = "Recent Activity") }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatsSummaryCard(
                            title = "This Week",
                            value = formatDurationMs(uiState.listenedThisWeekMs),
                            modifier = Modifier.weight(1f),
                        )
                        StatsSummaryCard(
                            title = "This Month",
                            value = formatDurationMs(uiState.listenedThisMonthMs),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Listening Habits ───────────────────────────────────
            if (uiState.averageSessionLengthMs > 0) {
                item { SectionHeader(title = "Listening Habits") }

                item {
                    StatsSummaryCard(
                        title = "Average Session",
                        value = formatDurationMs(uiState.averageSessionLengthMs),
                    )
                }

                if (uiState.mostActiveDay.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatsSummaryCard(
                                title = "Most Active Day",
                                value = uiState.mostActiveDay,
                                modifier = Modifier.weight(1f),
                            )
                            StatsSummaryCard(
                                title = "Peak Hour",
                                value = uiState.mostActiveHour,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ── Completion ─────────────────────────────────────────
            if (uiState.averageCompletionPercent > 0) {
                item { SectionHeader(title = "Completion") }

                item {
                    StatsSummaryCard(
                        title = "Average Completion",
                        value = "${uiState.averageCompletionPercent}%",
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatsSummaryCard(
                            title = "Finished",
                            value = "${uiState.finishedEpisodes} episode${if (uiState.finishedEpisodes != 1) "s" else ""}",
                            modifier = Modifier.weight(1f),
                        )
                        StatsSummaryCard(
                            title = "Abandoned",
                            value = "${uiState.abandonedEpisodes} episode${if (uiState.abandonedEpisodes != 1) "s" else ""}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Most Listened Podcasts ───────────────────────────────

            if (uiState.mostListenedPodcasts.isNotEmpty()) {
                item { SectionHeader(title = "Most Listened Podcasts") }

                itemsIndexed(uiState.mostListenedPodcasts) { index, stat ->
                    PodcastStatRow(
                        rank = index + 1,
                        title = stat.podcastTitle,
                        artworkUrl = stat.artworkUrl,
                        subtitle = formatDurationMs(stat.totalListenedMs),
                    )
                }
            }

            // ── Most Listened Episodes ───────────────────────────────

            if (uiState.mostListenedEpisodes.isNotEmpty()) {
                item { SectionHeader(title = "Most Listened Episodes") }

                itemsIndexed(uiState.mostListenedEpisodes) { index, stat ->
                    EpisodeStatRow(
                        rank = index + 1,
                        title = stat.episodeTitle,
                        podcastTitle = stat.podcastTitle,
                        subtitle = formatDurationMs(stat.totalListenedMs),
                    )
                }
            }

            // ── Most Downloaded Podcasts ─────────────────────────────

            if (uiState.mostDownloadedPodcasts.isNotEmpty()) {
                item { SectionHeader(title = "Most Downloaded Podcasts") }

                itemsIndexed(uiState.mostDownloadedPodcasts) { index, stat ->
                    PodcastStatRow(
                        rank = index + 1,
                        title = stat.podcastTitle,
                        artworkUrl = stat.artworkUrl,
                        subtitle = "${stat.downloadCount} download${if (stat.downloadCount != 1L) "s" else ""}",
                    )
                }
            }
        }
    }
}

// =====================================================================
// Stats components
// =====================================================================

@Composable
internal fun StatsSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun PodcastStatRow(
    rank: Int,
    title: String,
    artworkUrl: String,
    subtitle: String,
) {
    val podcastsFallback = rememberVectorPainter(Icons.Default.Podcasts)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )

            AsyncImage(
                model = artworkUrl.ifBlank { null },
                contentDescription = "$title artwork",
                placeholder = podcastsFallback,
                error = podcastsFallback,
                fallback = podcastsFallback,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun EpisodeStatRow(
    rank: Int,
    title: String,
    podcastTitle: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// =====================================================================
// Utility functions
// =====================================================================

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0h 0m"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}
