package com.podbelly.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import coil.request.SuccessResult
import com.podbelly.core.playback.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playback = uiState.playbackState
    val sleepTimerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Extract dominant color from artwork for background tinting
    val dominantColor = rememberDominantColor(
        imageUrl = playback.artworkUrl,
        defaultColor = MaterialTheme.colorScheme.primaryContainer,
    )
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        label = "dominantColor",
    )

    // Save playback position when leaving the screen
    DisposableEffect(Unit) {
        onDispose { viewModel.savePosition() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->

        // Artwork-tinted gradient background
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedDominant.copy(alpha = 0.3f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(modifier = Modifier.height(8.dp))

                // ── Large artwork ────────────────────────────────────────
                AsyncImage(
                    model = playback.artworkUrl.ifBlank { null },
                    contentDescription = "Episode artwork",
                    placeholder = rememberVectorPainter(Icons.Default.Podcasts),
                    error = rememberVectorPainter(Icons.Default.Podcasts),
                    fallback = rememberVectorPainter(Icons.Default.Podcasts),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Episode title ────────────────────────────────────────
                Text(
                    text = playback.episodeTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE),
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Podcast title ────────────────────────────────────────
                Text(
                    text = playback.podcastTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Current chapter title (if chapters exist) ─────────
                if (playback.chapters.isNotEmpty() && playback.currentChapterIndex >= 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playback.chapters[playback.currentChapterIndex].title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showChaptersList() },
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Seek bar ─────────────────────────────────────────────
                SeekBar(
                    currentPosition = playback.currentPosition,
                    duration = playback.duration,
                    onSeek = { viewModel.seekTo(it) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Main transport controls ──────────────────────────────
                MainControls(
                    isPlaying = playback.isPlaying,
                    hasChapters = playback.chapters.isNotEmpty(),
                    hasPreviousChapter = playback.currentChapterIndex > 0,
                    hasNextChapter = playback.currentChapterIndex < playback.chapters.size - 1,
                    onSkipBack = { viewModel.skipBack() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipForward = { viewModel.skipForward() },
                    onPreviousChapter = { viewModel.seekToPreviousChapter() },
                    onNextChapter = { viewModel.seekToNextChapter() },
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Secondary controls row ───────────────────────────────
                SecondaryControls(
                    playbackSpeed = uiState.playbackSpeed,
                    sleepTimerRemaining = uiState.sleepTimerRemaining,
                    isSleepTimerActive = uiState.isSleepTimerActive,
                    skipSilence = uiState.skipSilence,
                    volumeBoost = uiState.volumeBoost,
                    hasChapters = playback.chapters.isNotEmpty(),
                    onSpeedClick = { viewModel.showSpeedPicker() },
                    onSleepTimerClick = { viewModel.showSleepTimerPicker() },
                    onToggleSkipSilence = { viewModel.toggleSkipSilence() },
                    onToggleVolumeBoost = { viewModel.toggleVolumeBoost() },
                    onChaptersClick = { viewModel.showChaptersList() },
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // ── Speed picker bottom sheet ────────────────────────────────────────
    if (uiState.showSpeedPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSpeedPicker() },
            sheetState = speedPickerSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            SpeedPickerContent(
                currentSpeed = uiState.playbackSpeed,
                podcastTitle = uiState.playbackState.podcastTitle,
                onSelect = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { viewModel.hideSpeedPicker() },
            )
        }
    }

    // ── Sleep timer picker bottom sheet ──────────────────────────────────
    if (uiState.showSleepTimerPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSleepTimerPicker() },
            sheetState = sleepTimerSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            SleepTimerPickerContent(
                isSleepTimerActive = uiState.isSleepTimerActive,
                sleepTimerRemaining = uiState.sleepTimerRemaining,
                onSelect = { viewModel.startSleepTimer(it) },
                onEndOfEpisode = { viewModel.startSleepTimerEndOfEpisode() },
                onCancel = {
                    viewModel.cancelSleepTimer()
                    viewModel.hideSleepTimerPicker()
                },
                onDismiss = { viewModel.hideSleepTimerPicker() },
            )
        }
    }

    // ── Chapters list bottom sheet ────────────────────────────────────
    if (uiState.showChaptersList && playback.chapters.isNotEmpty()) {
        val chaptersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideChaptersList() },
            sheetState = chaptersSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            ChaptersListContent(
                chapters = playback.chapters,
                currentChapterIndex = playback.currentChapterIndex,
                onChapterClick = { index ->
                    viewModel.seekToChapter(index)
                    viewModel.hideChaptersList()
                },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Seek bar
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    // While dragging we show the local slider value; otherwise follow the actual position.
    var localSlider by remember { mutableFloatStateOf(0f) }
    val sliderPosition = if (duration > 0L) {
        if (isDragging) localSlider else currentPosition.toFloat() / duration.toFloat()
    } else {
        0f
    }

    // Animated thumb size: grows when dragging for easier interaction
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 24.dp else 14.dp,
        label = "thumbSize",
    )

    // The time shown in the tooltip while dragging
    val displayPosition = if (isDragging) {
        (localSlider * duration).toLong()
    } else {
        currentPosition
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Floating time tooltip that appears above the slider when dragging
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                // Position the tooltip based on slider fraction
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Offset the chip horizontally based on slider position
                    val offsetFraction = sliderPosition - 0.5f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(
                                start = if (offsetFraction > 0f) (offsetFraction * 200).dp.coerceAtMost(120.dp) else 0.dp,
                                end = if (offsetFraction < 0f) (-offsetFraction * 200).dp.coerceAtMost(120.dp) else 0.dp,
                            ),
                        contentAlignment = if (offsetFraction < -0.15f) Alignment.CenterStart
                        else if (offsetFraction > 0.15f) Alignment.CenterEnd
                        else Alignment.Center,
                    ) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shadowElevation = 4.dp,
                        ) {
                            Text(
                                text = formatMillis(displayPosition),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { fraction ->
                localSlider = fraction
                onSeek((fraction * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(
                            elevation = if (isDragging) 6.dp else 2.dp,
                            shape = CircleShape,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Current position
            Text(
                text = formatMillis(currentPosition),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Remaining time
            Text(
                text = "-${formatMillis((duration - currentPosition).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Main transport controls
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun MainControls(
    isPlaying: Boolean,
    hasChapters: Boolean,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onSkipBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous chapter button (only when chapters exist)
        if (hasChapters) {
            IconButton(
                onClick = onPreviousChapter,
                enabled = hasPreviousChapter,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous chapter",
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Skip back 10s
        FilledTonalIconButton(
            onClick = onSkipBack,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = "Skip back 10 seconds",
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play / Pause -- large filled circle (64dp)
        FilledIconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onTogglePlayPause()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Skip forward 30s
        FilledTonalIconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward30,
                contentDescription = "Skip forward 30 seconds",
                modifier = Modifier.size(28.dp),
            )
        }

        // Next chapter button (only when chapters exist)
        if (hasChapters) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onNextChapter,
                enabled = hasNextChapter,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next chapter",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Secondary controls row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun SecondaryControls(
    playbackSpeed: Float,
    sleepTimerRemaining: Long,
    isSleepTimerActive: Boolean,
    skipSilence: Boolean,
    volumeBoost: Boolean,
    hasChapters: Boolean,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onToggleSkipSilence: () -> Unit,
    onToggleVolumeBoost: () -> Unit,
    onChaptersClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Playback speed chip
        FilledTonalButton(
            onClick = onSpeedClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatSpeed(playbackSpeed),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Sleep timer chip
        FilledTonalButton(
            onClick = onSleepTimerClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = if (isSleepTimerActive) Icons.Filled.Bedtime else Icons.Filled.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSleepTimerActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isSleepTimerActive) formatMillis(sleepTimerRemaining) else "Sleep",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSleepTimerActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }

        // Skip silence toggle
        val skipSilenceColor by animateColorAsState(
            targetValue = if (skipSilence) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            label = "skipSilenceColor",
        )
        FilledIconToggleButton(
            checked = skipSilence,
            onCheckedChange = { onToggleSkipSilence() },
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = if (skipSilence) "Disable skip silence" else "Enable skip silence",
                tint = skipSilenceColor,
                modifier = Modifier.size(20.dp),
            )
        }

        // Chapters list button (only when chapters exist)
        if (hasChapters) {
            FilledTonalButton(
                onClick = onChaptersClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ch.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Volume boost toggle
        val volumeBoostColor by animateColorAsState(
            targetValue = if (volumeBoost) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            label = "volumeBoostColor",
        )
        FilledIconToggleButton(
            checked = volumeBoost,
            onCheckedChange = { onToggleVolumeBoost() },
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (volumeBoost) "Disable volume boost" else "Enable volume boost",
                tint = volumeBoostColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Speed picker bottom sheet content
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun SpeedPickerContent(
    currentSpeed: Float,
    podcastTitle: String = "",
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 2.5f, 3.0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = if (podcastTitle.isNotBlank()) "Speed for $podcastTitle" else "Playback Speed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(speeds) { speed ->
                val isSelected = speed == currentSpeed
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    label = "speedChipColor",
                )
                FilledTonalButton(
                    onClick = {
                        onSelect(speed)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        text = formatSpeed(speed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Sleep timer picker bottom sheet content
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun SleepTimerPickerContent(
    isSleepTimerActive: Boolean,
    sleepTimerRemaining: Long,
    onSelect: (Int) -> Unit,
    onEndOfEpisode: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = "Sleep Timer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (isSleepTimerActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatMillis(sleepTimerRemaining)} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        val timerOptions = listOf(
            "5 minutes" to 5,
            "10 minutes" to 10,
            "15 minutes" to 15,
            "30 minutes" to 30,
            "45 minutes" to 45,
            "1 hour" to 60,
        )

        timerOptions.forEach { (label, minutes) ->
            TextButton(
                onClick = { onSelect(minutes) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // "End of episode" option -- monitors playback and pauses at track end
        TextButton(
            onClick = onEndOfEpisode,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(
                text = "End of episode",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // "Off" / Cancel option
        if (isSleepTimerActive) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.TimerOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Turn off timer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    text = "Off",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Chapters list bottom sheet content
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun ChaptersListContent(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = "Chapters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(chapters) { index, chapter ->
                val isCurrent = index == currentChapterIndex
                val bgColor = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onChapterClick(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatMillis(chapter.startTimeMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Utility formatters
// ═════════════════════════════════════════════════════════════════════════════

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toLong().toFloat()) {
        "${speed.toInt()}.0x"
    } else {
        "${speed}x"
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Artwork color extraction
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun rememberDominantColor(imageUrl: String, defaultColor: Color): Color {
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val context = LocalContext.current

    LaunchedEffect(imageUrl) {
        if (imageUrl.isBlank()) {
            dominantColor = defaultColor
            return@LaunchedEffect
        }
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)
                ?.drawable
                ?.let { it as? BitmapDrawable }
                ?.bitmap
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.darkMutedSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                if (swatch != null) {
                    dominantColor = Color(swatch.rgb)
                }
            }
        } catch (_: Exception) {
            dominantColor = defaultColor
        }
    }

    return dominantColor
}
