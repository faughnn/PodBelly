package com.podbelly.core.common.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.podbelly.core.common.theme.LocalAppTheme
import com.podbelly.core.common.theme.ThemeCatalog

/**
 * The square "share this episode" card that gets rendered to an image and
 * attached to a share. It reads the ambient [MaterialTheme] — so its surround,
 * accent and typography (Outfit) are exactly the user's current theme — and
 * names that theme in the corner via [LocalAppTheme].
 *
 * All text is clamped with maxLines + ellipsis and sits inside the padded card,
 * so nothing overflows regardless of title length.
 */
@Composable
fun EpisodeShareCard(
    episodeTitle: String,
    podcastTitle: String,
    metaLine: String,
    artwork: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val themeName = ThemeCatalog.specFor(LocalAppTheme.current).displayName

    Column(
        modifier = modifier
            .width(360.dp)
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Wordmark: "pod" + "belly" (belly in the accent), matching the app.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("pod", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Text("belly", style = MaterialTheme.typography.headlineSmall, color = colors.primary)
        }

        Spacer(Modifier.height(16.dp))

        // Artwork (episode art, falling back to series art upstream).
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            } else {
                Text(
                    text = podcastTitle.take(1).uppercase(),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = episodeTitle,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = podcastTitle,
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (metaLine.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // The card wraps its content (fixed width, natural height), so nothing
        // ever clips; a fixed gap sits above the footer row.
        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PodBelly",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = themeName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(180.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}
