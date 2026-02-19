package com.podbelly.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.podbelly.core.playback.PlaybackState

/**
 * A compact 64dp mini-player bar intended to sit at the bottom of the screen,
 * above the navigation bar. It shows the currently playing episode with artwork,
 * title, and a play/pause button. Tapping the bar navigates to the full
 * [PlayerScreen].
 *
 * The bar animates in when playback starts and animates out when nothing is
 * loaded.
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show the mini player whenever an episode is loaded (playing or paused).
    val isVisible = playbackState.episodeId != 0L &&
        playbackState.episodeTitle.isNotBlank()

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) + fadeOut(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Thin progress indicator at the very top edge
                val progress = if (playbackState.duration > 0L) {
                    (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Content row  --  total height ~62dp (64dp minus 2dp indicator)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Small artwork (48dp, rounded corners)
                    AsyncImage(
                        model = playbackState.artworkUrl,
                        contentDescription = "Episode artwork",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Episode title (single line, ellipsis)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.episodeTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = playbackState.podcastTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play / Pause button
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
