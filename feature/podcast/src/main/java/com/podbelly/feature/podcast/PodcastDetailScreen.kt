package com.podbelly.feature.podcast

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.podbelly.core.common.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    viewModel: PodcastDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onEpisodeClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PodcastDetailTopBar(
                title = uiState.podcast?.title ?: "",
                onNavigateBack = onNavigateBack,
                onUnsubscribe = {
                    viewModel.unsubscribe()
                    onNavigateBack()
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Header section: artwork, title, author, description
                item(key = "header") {
                    PodcastHeader(
                        podcast = uiState.podcast
                    )
                }

                // Filter chips row
                item(key = "filters") {
                    FilterChipsRow(
                        currentFilter = uiState.filter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )
                }

                // Episode list
                items(
                    items = uiState.episodes,
                    key = { it.id }
                ) { episode ->
                    EpisodeCard(
                        episode = episode,
                        downloadProgress = downloadProgress[episode.id],
                        onClick = { onEpisodeClick(episode.id) },
                        onPlay = { viewModel.playEpisode(episode.id) },
                        onDownload = { viewModel.downloadEpisode(episode.id) },
                        onDeleteDownload = { viewModel.deleteDownload(episode.id) },
                        onTogglePlayed = { viewModel.togglePlayed(episode.id) },
                        onAddToQueue = { viewModel.addToQueue(episode.id) },
                        onRemoveFromQueue = { viewModel.removeFromQueue(episode.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Unsubscribe") },
                        onClick = {
                            showOverflowMenu = false
                            onUnsubscribe()
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
private fun PodcastHeader(
    podcast: PodcastUiModel?
) {
    if (podcast == null) return

    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Artwork
        AsyncImage(
            model = podcast.artworkUrl.ifBlank { null },
            contentDescription = "${podcast.title} artwork",
            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
            error = rememberVectorPainter(Icons.Default.Podcasts),
            fallback = rememberVectorPainter(Icons.Default.Podcasts),
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Title, author, description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = podcast.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
            )

            if (podcast.description.length > 100) {
                Text(
                    text = if (isDescriptionExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    currentFilter: EpisodeFilter,
    onFilterSelected: (EpisodeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EpisodeFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            EpisodeFilter.ALL -> "All"
                            EpisodeFilter.UNPLAYED -> "Unplayed"
                            EpisodeFilter.DOWNLOADED -> "Downloaded"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: EpisodeUiModel,
    downloadProgress: Float?,
    onClick: () -> Unit = {},
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onTogglePlayed: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromQueue: () -> Unit
) {
    val isDownloading = downloadProgress != null
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
            // Played indicator icon
            Icon(
                imageVector = if (episode.played) Icons.Filled.Circle else Icons.Outlined.Circle,
                contentDescription = if (episode.played) "Played" else "Unplayed",
                modifier = Modifier
                    .size(12.dp)
                    .padding(top = 4.dp),
                tint = if (episode.played) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Episode info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title (bold)
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date + duration info line
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (episode.publicationDate > 0) {
                        Text(
                            text = DateUtils.relativeDate(episode.publicationDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (episode.durationSeconds > 0) {
                        Text(
                            text = DateUtils.formatDuration(episode.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (episode.isDownloaded) {
                        Text(
                            text = "Downloaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Playback progress bar (if episode has partial progress)
                if (episode.playbackPosition > 0 && episode.durationSeconds > 0) {
                    val progress = (episode.playbackPosition.toFloat() / 1000f) /
                        episode.durationSeconds.toFloat()

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Trailing action icons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Play button
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play episode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Download button
                IconButton(
                    onClick = {
                        when {
                            isDownloading -> { /* downloading, do nothing */ }
                            episode.isDownloaded -> { /* already downloaded, do nothing */ }
                            else -> onDownload()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    when {
                        isDownloading -> {
                            CircularProgressIndicator(
                                progress = { downloadProgress ?: 0f },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        episode.isDownloaded -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Downloaded — tap to delete",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Download episode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Overflow menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Add to queue / Remove from queue
                        if (episode.isInQueue) {
                            DropdownMenuItem(
                                text = { Text("Remove from queue") },
                                onClick = {
                                    showMenu = false
                                    onRemoveFromQueue()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Add to queue") },
                                onClick = {
                                    showMenu = false
                                    onAddToQueue()
                                }
                            )
                        }

                        // Download / Delete download
                        if (episode.isDownloaded) {
                            DropdownMenuItem(
                                text = { Text("Delete download") },
                                onClick = {
                                    showMenu = false
                                    onDeleteDownload()
                                }
                            )
                        } else if (!isDownloading) {
                            DropdownMenuItem(
                                text = { Text("Download") },
                                onClick = {
                                    showMenu = false
                                    onDownload()
                                }
                            )
                        }

                        // Mark played / unplayed
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (episode.played) "Mark as unplayed"
                                    else "Mark as played"
                                )
                            },
                            onClick = {
                                showMenu = false
                                onTogglePlayed()
                            }
                        )
                    }
                }
            }
        }
    }
}
