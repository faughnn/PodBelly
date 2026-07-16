package com.podbelly.feature.discover

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onPodcastClick: (podcastId: Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRssDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToPodcast.collect { podcastId ->
            onPodcastClick(podcastId)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearMessage()
        }
    }

    if (showRssDialog) {
        RssUrlDialog(
            feedUrl = uiState.feedUrlInput,
            onFeedUrlChange = viewModel::updateFeedUrl,
            onSubscribe = viewModel::subscribeByUrl,
            onDismiss = { showRssDialog = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchSection(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::search,
                onAddByRss = { showRssDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            when {
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.searchResults.isEmpty() && uiState.searchQuery.isBlank() -> {
                    ChartsSection(
                        categories = uiState.chartCategories,
                        selectedGenreId = uiState.selectedChartGenreId,
                        onCategorySelected = viewModel::selectChartCategory,
                        results = uiState.chartResults,
                        isLoading = uiState.isLoadingChart,
                        error = uiState.chartError,
                        onRetry = viewModel::retryChart,
                        subscribingFeedUrls = uiState.subscribingFeedUrls,
                        onSubscribe = viewModel::subscribeToPodcast,
                        onPodcastClick = viewModel::onPodcastClick,
                        regions = uiState.chartRegions,
                        selectedCountry = uiState.selectedChartCountry,
                        onRegionSelected = viewModel::selectChartRegion,
                        modifier = Modifier.weight(1f),
                    )
                }

                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    SearchResultsList(
                        results = uiState.searchResults,
                        subscribingFeedUrls = uiState.subscribingFeedUrls,
                        onSubscribe = viewModel::subscribeToPodcast,
                        onPodcastClick = viewModel::onPodcastClick,
                        regions = uiState.chartRegions,
                        selectedCountry = uiState.selectedChartCountry,
                        onRegionSelected = viewModel::selectChartRegion,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAddByRss: () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.weight(1f),
        placeholder = { Text("Search podcasts...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch(query)
                keyboardController?.hide()
            },
        ),
        )

        IconButton(onClick = onAddByRss) {
            Icon(
                imageVector = Icons.Outlined.RssFeed,
                contentDescription = "Add podcast by RSS URL",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun RssUrlDialog(
    feedUrl: String,
    onFeedUrlChange: (String) -> Unit,
    onSubscribe: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Subscribing continues in the ViewModel after the dialog closes; the
    // existing snackbar reports success or failure.
    fun submit() {
        onSubscribe(feedUrl)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add by RSS URL") },
        text = {
            OutlinedTextField(
                value = feedUrl,
                onValueChange = onFeedUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com/feed.xml") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.RssFeed,
                        contentDescription = null,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (feedUrl.isNotBlank()) submit() },
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = ::submit,
                enabled = feedUrl.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Subscribe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun ChartsSection(
    categories: List<ChartCategory>,
    selectedGenreId: Int,
    onCategorySelected: (Int) -> Unit,
    results: List<DiscoverPodcastItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    subscribingFeedUrls: Set<String>,
    onSubscribe: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
    regions: List<ChartRegion> = emptyList(),
    selectedCountry: String = "",
    onRegionSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showRegionDialog by rememberSaveable { mutableStateOf(false) }

    if (showRegionDialog) {
        ChartRegionDialog(
            regions = regions,
            selectedCountry = selectedCountry,
            onRegionSelected = {
                onRegionSelected(it)
                showRegionDialog = false
            },
            onDismiss = { showRegionDialog = false },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = categories,
                    key = { it.genreId },
                ) { category ->
                    FilterChip(
                        selected = category.genreId == selectedGenreId,
                        onClick = { onCategorySelected(category.genreId) },
                        label = { Text(category.label) },
                    )
                }
            }

            if (selectedCountry.isNotBlank()) {
                FilterChip(
                    selected = false,
                    onClick = { showRegionDialog = true },
                    label = { Text(selectedCountry.uppercase()) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Change chart region",
                        )
                    },
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                SearchResultsList(
                    results = results,
                    subscribingFeedUrls = subscribingFeedUrls,
                    onSubscribe = onSubscribe,
                    onPodcastClick = onPodcastClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun ChartRegionDialog(
    regions: List<ChartRegion>,
    selectedCountry: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chart region") },
        text = {
            LazyColumn {
                items(
                    items = regions,
                    key = { it.code },
                ) { region ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRegionSelected(region.code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = region.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (region.code == selectedCountry) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun SearchResultsList(
    results: List<DiscoverPodcastItem>,
    subscribingFeedUrls: Set<String>,
    onSubscribe: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = results,
            key = { it.feedUrl },
        ) { item ->
            SearchResultItem(
                item = item,
                isSubscribing = item.feedUrl in subscribingFeedUrls,
                onSubscribe = { onSubscribe(item.feedUrl) },
                onClick = { onPodcastClick(item.feedUrl) },
            )
        }
    }
}

@Composable
internal fun SearchResultItem(
    item: DiscoverPodcastItem,
    isSubscribing: Boolean,
    onSubscribe: () -> Unit,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.artworkUrl.ifBlank { null },
            contentDescription = "${item.title} artwork",
            placeholder = rememberVectorPainter(Icons.Default.Podcasts),
            error = rememberVectorPainter(Icons.Default.Podcasts),
            fallback = rememberVectorPainter(Icons.Default.Podcasts),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.author.isNotBlank()) {
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (item.isSubscribed) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Subscribed")
            }
        } else {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSubscribe()
                },
                enabled = !isSubscribing,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Subscribe")
            }
        }
    }
}

@Composable
internal fun DiscoverEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Text(
                text = "Search for podcasts or add an RSS feed URL",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
