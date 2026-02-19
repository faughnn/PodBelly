package com.podbelly.feature.queue

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel(),
    onNavigateToPlayer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Up Next",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (!uiState.isEmpty) {
                        TextButton(onClick = { viewModel.clearQueue() }) {
                            Text(
                                text = "Clear All",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isEmpty) {
            EmptyQueueContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            QueueContent(
                items = uiState.queueItems,
                nowPlayingEpisodeId = uiState.nowPlayingEpisodeId,
                onPlayItem = { episodeId ->
                    viewModel.playItem(episodeId)
                    onNavigateToPlayer()
                },
                onRemoveItem = { episodeId -> viewModel.removeItem(episodeId) },
                onMoveUp = { index ->
                    if (index > 0) viewModel.moveItem(index, index - 1)
                },
                onMoveDown = { index ->
                    if (index < uiState.queueItems.lastIndex) {
                        viewModel.moveItem(index, index + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
internal fun EmptyQueueContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your queue is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add episodes from podcast pages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun QueueContent(
    items: List<QueueItemUi>,
    nowPlayingEpisodeId: Long?,
    onPlayItem: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onMoveUp: (index: Int) -> Unit,
    onMoveDown: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.queueId }
        ) { index, item ->
            val isNowPlaying = item.episodeId == nowPlayingEpisodeId

            if (isNowPlaying && index == 0) {
                NowPlayingLabel()
            }

            QueueItemCard(
                item = item,
                isFirst = index == 0,
                isLast = index == items.lastIndex,
                isNowPlaying = isNowPlaying,
                onPlay = { onPlayItem(item.episodeId) },
                onRemove = { onRemoveItem(item.episodeId) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) }
            )
        }
    }
}

@Composable
internal fun NowPlayingLabel() {
    Text(
        text = "Now Playing",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
internal fun QueueItemCard(
    item: QueueItemUi,
    isFirst: Boolean,
    isLast: Boolean,
    isNowPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isNowPlaying) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "queueItemContainerColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode artwork
            AsyncImage(
                model = item.artworkUrl.ifBlank { null },
                contentDescription = item.episodeTitle,
                placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                error = rememberVectorPainter(Icons.Default.Podcasts),
                fallback = rememberVectorPainter(Icons.Default.Podcasts),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Episode title + podcast title + duration
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.episodeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.durationSeconds > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDuration(item.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Reorder up/down buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (!isFirst) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (!isLast) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Formats a duration in seconds to a human-readable string (e.g. "1h 23m" or "45 min").
 */
internal fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "${totalSeconds}s"
    }
}
