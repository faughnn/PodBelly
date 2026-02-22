package com.podbelly.feature.podcast

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.podbelly.core.common.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    viewModel: EpisodeDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPodcast: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val episodeProgress = downloadProgress[uiState.episodeId]
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Artwork with colored glow shadow
            AsyncImage(
                model = uiState.artworkUrl.ifBlank { null },
                contentDescription = "${uiState.title} artwork",
                placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                error = rememberVectorPainter(Icons.Default.Podcasts),
                fallback = rememberVectorPainter(Icons.Default.Podcasts),
                modifier = Modifier
                    .size(200.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Episode title
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Podcast name (clickable)
            if (uiState.podcastTitle.isNotBlank()) {
                Text(
                    text = uiState.podcastTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        if (uiState.podcastId > 0L) {
                            onNavigateToPodcast(uiState.podcastId)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row: date + duration
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.publicationDate > 0) {
                    Text(
                        text = DateUtils.relativeDate(uiState.publicationDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.durationSeconds > 0) {
                    Text(
                        text = DateUtils.formatDuration(uiState.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.played) {
                    Text(
                        text = "Played",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Playback progress bar
            if (uiState.playbackPosition > 0 && uiState.durationSeconds > 0) {
                val progress = (uiState.playbackPosition.toFloat() / 1000f) /
                    uiState.durationSeconds.toFloat()

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
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

                val remainingSeconds = uiState.durationSeconds - (uiState.playbackPosition / 1000).toInt()
                if (remainingSeconds > 0) {
                    Text(
                        text = "${DateUtils.formatDuration(remainingSeconds)} remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download / Play button — download-first pattern
            FilledTonalButton(
                onClick = {
                    when {
                        uiState.isDownloaded -> viewModel.playEpisode()
                        episodeProgress != null -> { /* downloading, do nothing */ }
                        else -> viewModel.downloadEpisode()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    episodeProgress != null -> {
                        CircularProgressIndicator(
                            progress = { episodeProgress ?: 0f },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Downloading")
                    }
                    uiState.isDownloaded -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }

            // Share button
            FilledTonalButton(
                onClick = {
                    val shareText = buildString {
                        append(uiState.title)
                        if (uiState.podcastTitle.isNotBlank()) {
                            append(" - ${uiState.podcastTitle}")
                        }
                        if (uiState.audioUrl.isNotBlank()) {
                            append("\n${uiState.audioUrl}")
                        }
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Episode"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description / show notes
            if (uiState.description.isNotBlank()) {
                var isExpanded by remember { mutableStateOf(false) }

                Text(
                    text = "Show Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val notesTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val notesLinkColor = MaterialTheme.colorScheme.primary.toArgb()
                val notesMaxLines = if (isExpanded) Int.MAX_VALUE else 8

                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            setTextColor(notesTextColor)
                            setLinkTextColor(notesLinkColor)
                            textSize = 14f
                            maxLines = notesMaxLines
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        textView.text = android.text.Html.fromHtml(
                            uiState.description,
                            android.text.Html.FROM_HTML_MODE_COMPACT,
                        )
                        textView.maxLines = notesMaxLines
                        textView.setTextColor(notesTextColor)
                        textView.setLinkTextColor(notesLinkColor)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                )

                if (uiState.description.length > 200) {
                    Text(
                        text = if (isExpanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
