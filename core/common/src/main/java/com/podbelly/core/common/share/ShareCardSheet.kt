package com.podbelly.core.common.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Bottom sheet that previews the [EpisodeShareCard] and, on tap, renders it to
 * an image and opens the system share sheet with [shareText] attached.
 *
 * [artwork] should be pre-loaded by the caller (software bitmap) so the capture
 * is deterministic and includes the cover art.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCardSheet(
    episodeTitle: String,
    podcastTitle: String,
    metaLine: String,
    artwork: ImageBitmap?,
    shareText: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Share episode", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

            // Draw the card into a graphics layer so we can snapshot it to a bitmap.
            Column(
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                },
            ) {
                EpisodeShareCard(
                    episodeTitle = episodeTitle,
                    podcastTitle = podcastTitle,
                    metaLine = metaLine,
                    artwork = artwork,
                )
            }

            Button(
                enabled = !isSharing,
                onClick = {
                    scope.launch {
                        isSharing = true
                        val bitmap = graphicsLayer.toImageBitmap()
                        shareCardImage(context, bitmap, shareText, "Share episode")
                        isSharing = false
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isSharing) "Preparing…" else "Share")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
