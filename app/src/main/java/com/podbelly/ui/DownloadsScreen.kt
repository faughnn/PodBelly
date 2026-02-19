package com.podbelly.ui

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.podbelly.core.common.DownloadsSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onEpisodeClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DownloadsTopBar(
                sortOrder = uiState.sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
            )
        }
    ) { paddingValues ->
        if (uiState.episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download episodes to listen offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.episodes,
                    key = { it.episodeId }
                ) { episode ->
                    DownloadedEpisodeCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode.episodeId) },
                        onPlay = { viewModel.playEpisode(episode.episodeId) },
                        onDelete = { viewModel.deleteDownload(episode.episodeId) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTopBar(
    sortOrder: DownloadsSortOrder,
    onSortOrderChange: (DownloadsSortOrder) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "Downloads",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort downloads",
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DownloadsSortMenuItem(
                        label = "Newest Downloaded",
                        selected = sortOrder == DownloadsSortOrder.DATE_NEWEST,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(DownloadsSortOrder.DATE_NEWEST)
                        }
                    )
                    DownloadsSortMenuItem(
                        label = "Oldest Downloaded",
                        selected = sortOrder == DownloadsSortOrder.DATE_OLDEST,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(DownloadsSortOrder.DATE_OLDEST)
                        }
                    )
                    DownloadsSortMenuItem(
                        label = "Name (A-Z)",
                        selected = sortOrder == DownloadsSortOrder.NAME_A_TO_Z,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(DownloadsSortOrder.NAME_A_TO_Z)
                        }
                    )
                    DownloadsSortMenuItem(
                        label = "Podcast",
                        selected = sortOrder == DownloadsSortOrder.PODCAST_NAME,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(DownloadsSortOrder.PODCAST_NAME)
                        }
                    )
                    DownloadsSortMenuItem(
                        label = "File Size",
                        selected = sortOrder == DownloadsSortOrder.FILE_SIZE,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(DownloadsSortOrder.FILE_SIZE)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun DownloadsSortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
    )
}

@Composable
private fun DownloadedEpisodeCard(
    episode: DownloadedEpisodeItem,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
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

                // Date and duration
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
                                text = formatDownloadDuration(episode.durationSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (episode.fileSize > 0) {
                        Text(
                            text = formatFileSize(episode.fileSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete download button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Play button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play episode"
                )
            }
        }
    }
}

private fun formatDownloadDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "${seconds}s"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.0f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}
