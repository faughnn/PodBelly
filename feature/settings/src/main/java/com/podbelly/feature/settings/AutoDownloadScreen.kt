package com.podbelly.feature.settings

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.podbelly.core.common.shouldAutoDownload
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_ALWAYS
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_SMART

/**
 * The per-show auto-download manager (Pocket Casts' per-podcast auto-download
 * screen, informed by listening stats): every subscription with its listening
 * history and a Smart / Always / Never picker, most recently listened first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDownloadScreen(
    viewModel: AutoDownloadViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Auto-download",
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = if (uiState.smartEnabled) {
                        "Smart auto-download is on: shows you've listened to in the " +
                            "last ${uiState.windowDays} days download new episodes " +
                            "automatically. Override any show below."
                    } else {
                        "Smart auto-download is off (Settings > Downloads), so only " +
                            "shows set to Always download new episodes automatically."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.shows.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "No subscriptions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }

            items(
                items = uiState.shows,
                key = { it.podcastId },
            ) { show ->
                AutoDownloadShowCard(
                    show = show,
                    smartEnabled = uiState.smartEnabled,
                    onModeSelected = { viewModel.setMode(show.podcastId, it) },
                )
            }
        }
    }
}

@Composable
internal fun AutoDownloadShowCard(
    show: AutoDownloadShow,
    smartEnabled: Boolean,
    onModeSelected: (Int) -> Unit = {},
) {
    val podcastsFallback = rememberVectorPainter(Icons.Default.Podcasts)
    val downloads = shouldAutoDownload(
        autoDownloadMode = show.autoDownloadMode,
        smartEnabled = smartEnabled,
        engaged = show.engaged,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = show.artworkUrl.ifBlank { null },
                    contentDescription = "${show.title} artwork",
                    placeholder = podcastsFallback,
                    error = podcastsFallback,
                    fallback = podcastsFallback,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = show.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (show.lastListenedAt > 0L) {
                            "Listened ${formatListenedMs(show.totalListenedMs)} · " +
                                "last ${relativeListenTime(show.lastListenedAt)}"
                        } else {
                            "Never listened"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (downloads) "Downloads new episodes" else "Not auto-downloading",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (downloads) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AutoDownloadModeChips(
                selectedMode = show.autoDownloadMode,
                onModeSelected = onModeSelected,
            )
        }
    }
}

/** The Smart / Always / Never picker, shared with the Stats Podcasts tab. */
@Composable
internal fun AutoDownloadModeChips(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            AUTO_DOWNLOAD_SMART to "Smart",
            AUTO_DOWNLOAD_ALWAYS to "Always",
            AUTO_DOWNLOAD_NEVER to "Never",
        ).forEach { (mode, label) ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(label) },
            )
        }
    }
}

private fun relativeListenTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
