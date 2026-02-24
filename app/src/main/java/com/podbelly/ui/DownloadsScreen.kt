package com.podbelly.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podbelly.core.common.DownloadsSortOrder
import com.podbelly.core.common.MobileDataWarningDialog
import com.podbelly.core.database.dao.DownloadErrorWithEpisode
import com.podbelly.feature.home.EpisodeCard
import com.podbelly.feature.home.HomeEpisodeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onEpisodeClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showMobileDataWarning by viewModel.showMobileDataWarning.collectAsStateWithLifecycle()

    if (showMobileDataWarning) {
        MobileDataWarningDialog(onDismiss = { viewModel.dismissMobileDataWarning() })
    }

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
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
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
                // Failed downloads section
                if (uiState.downloadErrors.isNotEmpty()) {
                    item(key = "failed_header") {
                        FailedDownloadsHeader(
                            count = uiState.downloadErrors.size,
                            onClearAll = { viewModel.clearAllErrors() },
                        )
                    }
                    items(
                        items = uiState.downloadErrors,
                        key = { "error_${it.id}" }
                    ) { error ->
                        FailedDownloadCard(
                            error = error,
                            onRetry = { viewModel.retryDownload(error.episodeId) },
                        )
                    }
                    item(key = "failed_divider") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(
                    items = uiState.episodes,
                    key = { it.episodeId }
                ) { episode ->
                    val homeItem = HomeEpisodeItem(
                        episodeId = episode.episodeId,
                        title = episode.title,
                        podcastTitle = episode.podcastTitle,
                        artworkUrl = episode.artworkUrl,
                        publicationDate = episode.publicationDate,
                        durationSeconds = episode.durationSeconds,
                        played = episode.played,
                        downloadPath = episode.downloadPath,
                        playbackPosition = episode.playbackPosition,
                    )
                    EpisodeCard(
                        episode = homeItem,
                        downloadProgress = null,
                        onClick = { onEpisodeClick(episode.episodeId) },
                        onPlay = { viewModel.playEpisode(episode.episodeId) },
                        onDownload = { /* already downloaded */ },
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
internal fun FailedDownloadsHeader(
    count: Int,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Failed Downloads ($count)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = onClearAll) {
            Icon(
                imageVector = Icons.Default.ClearAll,
                contentDescription = "Clear all errors",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FailedDownloadCard(
    error: DownloadErrorWithEpisode,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = error.episodeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = error.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry download",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

