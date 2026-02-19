package com.podbelly.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.SortByAlpha
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
import com.podbelly.core.common.LibrarySortOrder
import com.podbelly.core.database.entity.PodcastEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToPodcast: (podcastId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LibraryTopBar(
                sortOrder = uiState.sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
            )
        }
    ) { paddingValues ->
        if (uiState.podcasts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "No subscriptions yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Subscribe to podcasts to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = uiState.podcasts,
                    key = { it.id }
                ) { podcast ->
                    PodcastGridItem(
                        podcast = podcast,
                        onClick = { onNavigateToPodcast(podcast.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    sortOrder: LibrarySortOrder,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "Library",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = "Sort podcasts",
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortMenuItem(
                        label = "Name (A-Z)",
                        selected = sortOrder == LibrarySortOrder.NAME_A_TO_Z,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(LibrarySortOrder.NAME_A_TO_Z)
                        }
                    )
                    SortMenuItem(
                        label = "Recently Added",
                        selected = sortOrder == LibrarySortOrder.RECENTLY_ADDED,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(LibrarySortOrder.RECENTLY_ADDED)
                        }
                    )
                    SortMenuItem(
                        label = "Episode Count",
                        selected = sortOrder == LibrarySortOrder.EPISODE_COUNT,
                        onClick = {
                            showSortMenu = false
                            onSortOrderChange(LibrarySortOrder.EPISODE_COUNT)
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
private fun SortMenuItem(
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
private fun PodcastGridItem(
    podcast: PodcastEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = podcast.artworkUrl.ifBlank { null },
            contentDescription = podcast.title,
            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
            error = rememberVectorPainter(Icons.Default.Podcasts),
            fallback = rememberVectorPainter(Icons.Default.Podcasts),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
