package com.podbelly.feature.settings

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.podbelly.core.database.dao.EpisodeListeningStat
import com.podbelly.core.database.dao.PodcastDownloadStat
import com.podbelly.core.database.dao.PodcastListeningStat
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPodcast: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val engagementStats by viewModel.engagementStats.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showYearReview by rememberSaveable { mutableStateOf(false) }

    if (showYearReview) {
        // Collected only while the dialog is open — this drives the ViewModel's
        // WhileSubscribed year-pinned stats pipeline.
        val yearReview by viewModel.yearReview.collectAsStateWithLifecycle()
        YearInReviewDialog(
            stats = yearReview,
            year = remember { Calendar.getInstance().get(Calendar.YEAR) },
            onDismiss = { showYearReview = false },
            onShare = { text ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share your year"))
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Top") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Podcasts") },
                )
            }

            when (selectedTab) {
                0 -> StatsOverviewTab(
                    uiState = uiState,
                    period = period,
                    onPeriodSelected = { viewModel.setPeriod(it) },
                    onYearReviewClick = { showYearReview = true },
                )
                1 -> StatsTopTab(
                    uiState = uiState,
                    onPodcastClick = onNavigateToPodcast,
                )
                else -> PodcastEngagementTab(
                    stats = engagementStats,
                    duplicateGroups = duplicateGroups,
                    onMergeDuplicates = { group ->
                        viewModel.mergeDuplicates(group, keepPodcastId = group.first().podcastId)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Merged ${group.size} copies of ${group.first().podcastTitle}",
                            )
                        }
                    },
                    onPodcastClick = onNavigateToPodcast,
                    onUnsubscribe = { stat ->
                        viewModel.unsubscribe(stat.podcastId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Unsubscribed from ${stat.podcastTitle}",
                                actionLabel = "Undo",
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoUnsubscribe(stat.podcastId)
                            }
                        }
                    },
                )
            }
        }
    }
}

// =====================================================================
// Overview tab
// =====================================================================

@Composable
internal fun StatsOverviewTab(
    uiState: StatsUiState,
    period: StatsPeriod = StatsPeriod.ALL_TIME,
    onPeriodSelected: (StatsPeriod) -> Unit = {},
    onYearReviewClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Period selector ───────────────────────────────────────
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatsPeriod.entries) { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { onPeriodSelected(option) },
                        label = { Text(option.label) },
                    )
                }
            }
        }

        // ── Year in Review ────────────────────────────────────────
        item {
            val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onYearReviewClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$year in Review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "Your listening year, ready to share",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        // ── Hero: total listened ──────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Listened",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = formatDurationMs(uiState.totalListenedMs),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    daysOfAudioLabel(uiState.totalListenedMs)?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Today ${formatDurationMs(uiState.listenedTodayMs)} · " +
                            "This week ${formatDurationMs(uiState.listenedThisWeekMs)} · " +
                            "This month ${formatDurationMs(uiState.listenedThisMonthMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // ── Last 30 days chart ────────────────────────────────────
        item {
            StatsSection(title = "Last 30 Days") {
                Column(modifier = Modifier.padding(16.dp)) {
                    val tzOffsetMs = remember {
                        TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
                    }
                    val buckets = bucketDailyListening(
                        stats = uiState.dailyListening,
                        todayEpochDay = (System.currentTimeMillis() + tzOffsetMs) / 86_400_000L,
                    )
                    BarRow(values = buckets)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "30 days ago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ── Time saved ────────────────────────────────────────────
        item {
            val totalSaved = uiState.timeSavedBySpeedMs + uiState.skipSavedMs
            StatsSection(title = "Time Saved") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formatDurationMs(totalSaved),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TimeSavedRow(label = "Faster playback speed", value = uiState.timeSavedBySpeedMs)
                    TimeSavedRow(label = "Intros & outros skipped", value = uiState.skipSavedMs)
                }
            }
        }

        // ── Streaks (lifetime) ────────────────────────────────────
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

        // ── Listening habits ──────────────────────────────────────
        item { SectionHeader(title = "Listening Habits") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatsSummaryCard(
                    title = "Average Session",
                    value = formatDurationMs(uiState.averageSessionLengthMs),
                    modifier = Modifier.weight(1f),
                )
                StatsSummaryCard(
                    title = "Average Speed",
                    value = if (uiState.averageSpeed > 0f) {
                        String.format("%.2fx", uiState.averageSpeed)
                    } else "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (uiState.dayOfWeekStats.isNotEmpty()) {
            item {
                StatsSection(
                    title = "By Day" +
                        (uiState.mostActiveDay.takeIf { it.isNotEmpty() }
                            ?.let { " · busiest $it" } ?: ""),
                ) {
                    DayOfWeekChart(
                        stats = uiState.dayOfWeekStats,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (uiState.hourOfDayStats.isNotEmpty()) {
            item {
                StatsSection(
                    title = "By Hour" +
                        (uiState.mostActiveHour.takeIf { it.isNotEmpty() }
                            ?.let { " · peak $it" } ?: ""),
                ) {
                    HourOfDayChart(
                        stats = uiState.hourOfDayStats,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        // ── Sessions ──────────────────────────────────────────────
        if (uiState.sessionCount > 0) {
            item { SectionHeader(title = "Sessions") }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatsSummaryCard(
                            title = "Sessions",
                            value = "${uiState.sessionCount}",
                            modifier = Modifier.weight(1f),
                        )
                        StatsSummaryCard(
                            title = "Longest Session",
                            value = formatDurationMs(uiState.longestSessionMs),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatsSummaryCard(
                            title = "Days Listened",
                            value = "${uiState.daysListened}",
                            modifier = Modifier.weight(1f),
                        )
                        StatsSummaryCard(
                            title = "Avg per Active Day",
                            value = formatDurationMs(uiState.averagePerActiveDayMs),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── Keeping up ────────────────────────────────────────────
        if (uiState.keepUpTotal > 0) {
            item {
                val percent = uiState.keepUpPlayed * 100 / uiState.keepUpTotal
                StatsSection(title = "Keeping Up") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You played ${uiState.keepUpPlayed} of the " +
                                "${uiState.keepUpTotal} episodes that arrived in the last 30 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ── Library ───────────────────────────────────────────────
        item { SectionHeader(title = "Library") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsSummaryCard(
                        title = "Subscriptions",
                        value = "${uiState.subscriptionCount}",
                        modifier = Modifier.weight(1f),
                    )
                    StatsSummaryCard(
                        title = "Episodes",
                        value = "${uiState.libraryEpisodeCount}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsSummaryCard(
                        title = "Played",
                        value = if (uiState.libraryEpisodeCount > 0) {
                            "${uiState.libraryPlayedCount * 100 / uiState.libraryEpisodeCount}%"
                        } else "0%",
                        modifier = Modifier.weight(1f),
                    )
                    StatsSummaryCard(
                        title = "Downloads",
                        value = formatBytes(uiState.downloadedBytes),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Completion ────────────────────────────────────────────
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
    }
}

@Composable
private fun TimeSavedRow(label: String, value: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDurationMs(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// =====================================================================
// Top tab (the top-10 lists, moved off the Overview scroll)
// =====================================================================

@Composable
internal fun StatsTopTab(
    uiState: StatsUiState,
    onPodcastClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (uiState.mostListenedPodcasts.isEmpty() &&
            uiState.mostListenedEpisodes.isEmpty() &&
            uiState.mostDownloadedPodcasts.isEmpty()
        ) {
            item {
                Text(
                    text = "Play some episodes and your most-listened shows will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        if (uiState.mostListenedPodcasts.isNotEmpty()) {
            item {
                StatsSection(title = "Most Listened Podcasts") {
                    PodcastStatsList(uiState.mostListenedPodcasts, onPodcastClick = onPodcastClick)
                }
            }
        }

        if (uiState.mostListenedEpisodes.isNotEmpty()) {
            item {
                StatsSection(title = "Most Listened Episodes") {
                    EpisodeStatsList(uiState.mostListenedEpisodes)
                }
            }
        }

        if (uiState.mostDownloadedPodcasts.isNotEmpty()) {
            item {
                StatsSection(title = "Most Downloaded Podcasts") {
                    DownloadStatsList(uiState.mostDownloadedPodcasts, onPodcastClick = onPodcastClick)
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * A section with a header and a single containing card for all rows.
 */
@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun PodcastStatsList(
    stats: List<PodcastListeningStat>,
    onPodcastClick: (Long) -> Unit = {},
) {
    val podcastsFallback = rememberVectorPainter(Icons.Default.Podcasts)

    Column {
        stats.forEachIndexed { index, stat ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPodcastClick(stat.podcastId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )

                AsyncImage(
                    model = stat.artworkUrl.ifBlank { null },
                    contentDescription = "${stat.podcastTitle} artwork",
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
                        text = stat.podcastTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val episodeLabel = if (stat.episodeCount == 1L) "episode" else "episodes"
                    Text(
                        text = "${formatDurationMs(stat.totalListenedMs)} · ${stat.episodeCount} $episodeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun EpisodeStatsList(stats: List<EpisodeListeningStat>) {
    Column {
        stats.forEachIndexed { index, stat ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.episodeTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stat.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDurationMs(stat.totalListenedMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadStatsList(
    stats: List<PodcastDownloadStat>,
    onPodcastClick: (Long) -> Unit = {},
) {
    val podcastsFallback = rememberVectorPainter(Icons.Default.Podcasts)

    Column {
        stats.forEachIndexed { index, stat ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPodcastClick(stat.podcastId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )

                AsyncImage(
                    model = stat.artworkUrl.ifBlank { null },
                    contentDescription = "${stat.podcastTitle} artwork",
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
                        text = stat.podcastTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stat.downloadCount} download${if (stat.downloadCount != 1L) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// =====================================================================
// Year in Review
// =====================================================================

@Composable
internal fun YearInReviewDialog(
    stats: StatsUiState,
    year: Int,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$year in Review",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stats.totalListenedMs <= 0L) {
                    Text(
                        text = "Nothing tracked yet this year — play something and check back!",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    YearReviewRow(label = "Time listened", value = formatDurationMs(stats.totalListenedMs))
                    stats.mostListenedPodcasts.firstOrNull()?.let {
                        YearReviewRow(label = "Top show", value = it.podcastTitle)
                    }
                    if (stats.daysListened > 0) {
                        YearReviewRow(label = "Days with a podcast", value = "${stats.daysListened}")
                    }
                    if (stats.longestStreak > 1) {
                        YearReviewRow(label = "Longest streak", value = "${stats.longestStreak} days")
                    }
                    val saved = stats.timeSavedBySpeedMs + stats.skipSavedMs
                    if (saved > 0L) {
                        YearReviewRow(label = "Time saved", value = formatDurationMs(saved))
                    }
                    if (stats.finishedEpisodes > 0) {
                        YearReviewRow(
                            label = "Episodes finished",
                            value = "${stats.finishedEpisodes}",
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onShare(buildYearReviewText(stats, year)) },
                enabled = stats.totalListenedMs > 0L,
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun YearReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

/** The plain-text share card behind the Year in Review Share button. */
internal fun buildYearReviewText(stats: StatsUiState, year: Int): String = buildString {
    appendLine("My $year in podcasts 🎧")
    append("• ").append(formatDurationMs(stats.totalListenedMs)).append(" listened")
    daysOfAudioLabel(stats.totalListenedMs)?.let { append(" ($it)") }
    appendLine()
    stats.mostListenedPodcasts.firstOrNull()?.let {
        appendLine("• Top show: ${it.podcastTitle}")
    }
    if (stats.daysListened > 0) {
        appendLine("• ${stats.daysListened} days with a podcast in my ears")
    }
    if (stats.longestStreak > 1) {
        appendLine("• Longest streak: ${stats.longestStreak} days")
    }
    val saved = stats.timeSavedBySpeedMs + stats.skipSavedMs
    if (saved > 0L) {
        appendLine("• ${formatDurationMs(saved)} saved by speed & skips")
    }
    if (stats.finishedEpisodes > 0) {
        appendLine("• ${stats.finishedEpisodes} episodes finished")
    }
    append("Tracked with Podbelly")
}

// =====================================================================
// Utility functions
// =====================================================================

internal fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0h 0m"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

/** "≈ 12.5 days of audio", or null below one day (where it adds nothing). */
internal fun daysOfAudioLabel(ms: Long): String? {
    val days = ms / 86_400_000.0
    return if (days >= 1.0) "≈ ${String.format("%.1f", days)} days of audio" else null
}
