package com.podbelly.feature.home

import android.text.format.DateUtils
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.podbelly.core.common.MobileDataWarningDialog
import com.podbelly.core.common.RefreshProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    refreshProgress: RefreshProgress? = null,
    bannerMessage: String? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    onEpisodeClick: (Long) -> Unit,
    onPodcastClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val showMobileDataWarning by viewModel.showMobileDataWarning.collectAsStateWithLifecycle()
    val lastRefreshedAt by viewModel.lastRefreshedAt.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.downloadErrors.collect { error ->
            // Show in a child coroutine so a slow/queued snackbar doesn't block the
            // collector (which would back up rapid download-failure events).
            launch {
                snackbarHostState.showSnackbar(
                    message = "Download failed: ${error.message}",
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    if (showMobileDataWarning) {
        MobileDataWarningDialog(onDismiss = { viewModel.dismissMobileDataWarning() })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "pod",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "belly",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        RefreshStatusIndicator(
                            isRefreshing = isRefreshing,
                            refreshProgress = refreshProgress,
                            lastRefreshedAt = lastRefreshedAt,
                            bannerMessage = bannerMessage,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isEmpty) {
                EmptyState()
            } else {
                EpisodeList(
                    episodes = uiState.recentEpisodes,
                    newEpisodes = uiState.newEpisodes,
                    inProgressEpisodes = uiState.inProgressEpisodes,
                    downloadProgress = downloadProgress,
                    nowPlaying = nowPlaying,
                    onEpisodeClick = onEpisodeClick,
                    onPlayClick = { episodeId -> viewModel.playEpisode(episodeId) },
                    onDownloadClick = { episodeId -> viewModel.downloadEpisode(episodeId) },
                    onCancelDownloadClick = { episodeId -> viewModel.cancelDownload(episodeId) },
                    onDismissNewSection = viewModel::dismissNewSection,
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Empty state
// ------------------------------------------------------------------

@Composable
internal fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No episodes yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subscribe to podcasts to see new episodes here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ------------------------------------------------------------------
// Episode list with carousel
// ------------------------------------------------------------------

@Composable
internal fun EpisodeList(
    episodes: List<HomeEpisodeItem>,
    newEpisodes: List<HomeEpisodeItem> = emptyList(),
    inProgressEpisodes: List<HomeEpisodeItem> = emptyList(),
    downloadProgress: Map<Long, Float>,
    nowPlaying: NowPlayingState = NowPlayingState(),
    onEpisodeClick: (Long) -> Unit,
    onPlayClick: (Long) -> Unit,
    onDownloadClick: (Long) -> Unit,
    onCancelDownloadClick: (Long) -> Unit = {},
    onDismissNewSection: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Continue Listening horizontal carousel
        if (inProgressEpisodes.isNotEmpty()) {
            item(key = "carousel_header") {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Continue Listening",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            item(key = "carousel") {
                LazyRow(
                    modifier = Modifier.animateItem(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = inProgressEpisodes,
                        key = { "carousel_${it.episodeId}" }
                    ) { episode ->
                        CarouselCard(
                            episode = episode,
                            onClick = { onEpisodeClick(episode.episodeId) },
                        )
                    }
                }
            }
        }

        // New arrivals from the latest refreshes, pinned above the main list
        // until they've been seen. Sorted by publication date within the section.
        // Tapping the header dismisses the section.
        if (newEpisodes.isNotEmpty()) {
            item(key = "new_header") {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Dismiss new episodes",
                            onClick = onDismissNewSection,
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "${newEpisodes.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Same key as the main list (an episode is never in both — the
            // ViewModel filters new ids out of recentEpisodes), so animateItem()
            // slides a card between the sections on dismissal instead of
            // crossfading it out and back in.
            items(
                items = newEpisodes,
                key = { it.episodeId }
            ) { episode ->
                EpisodeCard(
                    episode = episode,
                    downloadProgress = downloadProgress[episode.episodeId],
                    isCurrentlyPlaying = nowPlaying.isPlaying &&
                        nowPlaying.episodeId == episode.episodeId,
                    onClick = { onEpisodeClick(episode.episodeId) },
                    onPlay = { onPlayClick(episode.episodeId) },
                    onDownload = { onDownloadClick(episode.episodeId) },
                    onCancelDownload = { onCancelDownloadClick(episode.episodeId) },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                            placementSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            ),
                        )
                        .padding(horizontal = 16.dp),
                )
            }

            if (episodes.isNotEmpty()) item(key = "new_footer") {
                Text(
                    text = "Earlier",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // Main episode list
        items(
            items = episodes,
            key = { it.episodeId }
        ) { episode ->
            EpisodeCard(
                episode = episode,
                downloadProgress = downloadProgress[episode.episodeId],
                isCurrentlyPlaying = nowPlaying.isPlaying &&
                    nowPlaying.episodeId == episode.episodeId,
                onClick = { onEpisodeClick(episode.episodeId) },
                onPlay = { onPlayClick(episode.episodeId) },
                onDownload = { onDownloadClick(episode.episodeId) },
                onCancelDownload = { onCancelDownloadClick(episode.episodeId) },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                        placementSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioLowBouncy,
                        ),
                    )
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

// ------------------------------------------------------------------
// Refresh status indicator (top bar, beside the logo)
// ------------------------------------------------------------------

@Composable
internal fun RefreshStatusIndicator(
    isRefreshing: Boolean,
    refreshProgress: RefreshProgress?,
    lastRefreshedAt: Long,
    bannerMessage: String?,
    modifier: Modifier = Modifier,
) {
    // Re-render every minute so "Updated X min ago" doesn't go stale while the
    // screen stays open.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRefreshing, lastRefreshedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000)
        }
    }

    when {
        isRefreshing -> {
            Row(
                modifier = modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Feeds refresh in parallel (5 at a time), so this counts
                // completions rather than a position in a sequence.
                val progressText = refreshProgress
                    ?.takeIf { it.total > 0 }
                    ?.let { "Checking ${it.completed}/${it.total}…" }
                    ?: "Checking…"
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        bannerMessage != null -> {
            Surface(
                modifier = modifier.padding(start = 12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = bannerMessage,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        lastRefreshedAt > 0L -> {
            val updatedText = if (now - lastRefreshedAt < DateUtils.MINUTE_IN_MILLIS) {
                "Updated just now"
            } else {
                "Updated " + DateUtils.getRelativeTimeSpanString(
                    lastRefreshedAt,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString().replaceFirstChar { it.lowercaseChar() }
            }
            Text(
                text = updatedText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(start = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ------------------------------------------------------------------
// Carousel card (130dp wide, artwork + title)
// ------------------------------------------------------------------

@Composable
private fun CarouselCard(
    episode: HomeEpisodeItem,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(10.dp)
    val hasProgress = episode.playbackPosition > 0L && episode.durationSeconds > 0

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
    ) {
        Box {
            AsyncImage(
                model = episode.artworkUrl.ifBlank { null },
                contentDescription = episode.title,
                placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                error = rememberVectorPainter(Icons.Default.Podcasts),
                fallback = rememberVectorPainter(Icons.Default.Podcasts),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cardShape),
                contentScale = ContentScale.Crop,
            )
            if (hasProgress) {
                val progress = (episode.playbackPosition.toFloat() / (episode.durationSeconds * 1000f))
                    .coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = episode.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = episode.podcastTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasProgress) {
            val remainingSeconds = episode.durationSeconds - (episode.playbackPosition / 1000).toInt()
            Text(
                text = "${formatDuration(remainingSeconds.coerceAtLeast(0))} left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

// ------------------------------------------------------------------
// Episode card — Jukebox styling with color-coded duration tags
// ------------------------------------------------------------------

@Composable
fun EpisodeCard(
    episode: HomeEpisodeItem,
    downloadProgress: Float?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit = {},
    isCurrentlyPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDownloaded = episode.downloadPath.isNotBlank()
    val isDownloading = downloadProgress != null
    val hasProgress = episode.playbackPosition > 0L && !episode.played && episode.durationSeconds > 0
    val playedAlpha = if (episode.played) 0.5f else 1f

    val cardShape = RoundedCornerShape(14.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.024f),
                shape = cardShape,
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = cardShape,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Podcast artwork
                AsyncImage(
                    model = episode.artworkUrl.ifBlank { null },
                    contentDescription = episode.podcastTitle,
                    placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                    error = rememberVectorPainter(Icons.Default.Podcasts),
                    fallback = rememberVectorPainter(Icons.Default.Podcasts),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .alpha(playedAlpha),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Text content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Episode title
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = playedAlpha),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Podcast title
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = playedAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Relative date and color-coded duration tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (episode.publicationDate > 0) {
                            Text(
                                text = DateUtils.getRelativeTimeSpanString(
                                    episode.publicationDate,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE
                                ).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = playedAlpha),
                            )
                        }
                        if (episode.durationSeconds > 0) {
                            DurationTag(
                                durationSeconds = episode.durationSeconds,
                                hasProgress = hasProgress,
                                playbackPosition = episode.playbackPosition,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Single action button: Download → Progress → Play / Played
                IconButton(
                    onClick = {
                        when {
                            isDownloaded -> onPlay()
                            isDownloading -> onCancelDownload()
                            else -> onDownload()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically),
                    colors = when {
                        isDownloaded && isCurrentlyPlaying -> IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        isDownloaded && episode.played -> IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        isDownloaded -> IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        else -> IconButtonDefaults.iconButtonColors()
                    }
                ) {
                    when {
                        isDownloading -> {
                            val pct = downloadProgress ?: 0f
                            if (pct < 0f) {
                                // Indeterminate: server sent no Content-Length.
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        isDownloaded && isCurrentlyPlaying -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause episode",
                            )
                        }
                        isDownloaded && episode.played -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Played",
                            )
                        }
                        isDownloaded -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play episode",
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            // Listening progress bar — gradient
            if (hasProgress) {
                val progress = (episode.playbackPosition.toFloat() / (episode.durationSeconds * 1000f))
                    .coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Color-coded duration tag
// ------------------------------------------------------------------

@Composable
private fun DurationTag(
    durationSeconds: Int,
    hasProgress: Boolean,
    playbackPosition: Long,
) {
    val durationMinutes = durationSeconds / 60
    val tagColor = when {
        durationMinutes < 15 -> MaterialTheme.colorScheme.secondary  // teal
        durationMinutes <= 45 -> MaterialTheme.colorScheme.tertiary   // amber
        else -> MaterialTheme.colorScheme.primary                     // coral
    }

    val text = if (hasProgress) {
        val remainingSeconds = durationSeconds - (playbackPosition / 1000).toInt()
        "${formatDuration(remainingSeconds.coerceAtLeast(0))} left"
    } else {
        formatDuration(durationSeconds)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tagColor.copy(alpha = 0.1f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tagColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------

/**
 * Formats a duration given in total seconds to a human-readable string.
 * Examples: "1h 23m", "45 min", "30s".
 */
fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "${seconds}s"
    }
}
