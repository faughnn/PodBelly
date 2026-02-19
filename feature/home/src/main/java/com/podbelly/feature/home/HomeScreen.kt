package com.podbelly.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Podcasts
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onEpisodeClick: (Long) -> Unit,
    onPodcastClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Home",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFeeds() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isEmpty) {
                EmptyState()
            } else {
                EpisodeList(
                    episodes = uiState.recentEpisodes,
                    downloadProgress = downloadProgress,
                    onEpisodeClick = onEpisodeClick,
                    onPlayClick = { episodeId -> viewModel.playEpisode(episodeId) },
                    onDownloadClick = { episodeId -> viewModel.downloadEpisode(episodeId) },
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
// Episode list
// ------------------------------------------------------------------

@Composable
internal fun EpisodeList(
    episodes: List<HomeEpisodeItem>,
    downloadProgress: Map<Long, Float>,
    onEpisodeClick: (Long) -> Unit,
    onPlayClick: (Long) -> Unit,
    onDownloadClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = episodes,
            key = { it.episodeId }
        ) { episode ->
            EpisodeCard(
                episode = episode,
                downloadProgress = downloadProgress[episode.episodeId],
                onClick = { onEpisodeClick(episode.episodeId) },
                onPlay = { onPlayClick(episode.episodeId) },
                onDownload = { onDownloadClick(episode.episodeId) },
            )
        }
    }
}

// ------------------------------------------------------------------
// Episode card
// ------------------------------------------------------------------

@Composable
internal fun EpisodeCard(
    episode: HomeEpisodeItem,
    downloadProgress: Float?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val isDownloaded = episode.downloadPath.isNotBlank()
    val isDownloading = downloadProgress != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
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
                    .clip(RoundedCornerShape(8.dp)),
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
                    color = if (episode.played) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Podcast title
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Relative date and duration
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (episode.durationSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = formatDuration(episode.durationSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Single action button: Download → Progress → Play
            IconButton(
                onClick = {
                    when {
                        isDownloaded -> onPlay()
                        isDownloading -> { /* downloading, do nothing */ }
                        else -> onDownload()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically),
                colors = if (isDownloaded) {
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    IconButtonDefaults.iconButtonColors()
                }
            ) {
                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            progress = { downloadProgress ?: 0f },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    isDownloaded -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play episode"
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------

/**
 * Formats a duration given in total seconds to a human-readable string.
 * Examples: "1h 23m", "45 min", "30s".
 */
internal fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "${seconds}s"
    }
}
