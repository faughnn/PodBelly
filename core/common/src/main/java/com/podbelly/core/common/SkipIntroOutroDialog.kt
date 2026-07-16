package com.podbelly.core.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Per-podcast "Skip intro" / "Skip outro" settings (AntennaPod's per-feed skip
 * pattern). Preset choices persist immediately, like the per-podcast speed presets.
 * Shared by the podcast page menu and the Now Playing screen menu.
 */
@Composable
fun SkipIntroOutroDialog(
    skipIntroSeconds: Int,
    skipOutroSeconds: Int,
    onSetSkipIntro: (Int) -> Unit,
    onSetSkipOutro: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skip intro & outro") },
        text = {
            Column {
                Text(
                    text = "Automatically skip the first and last seconds of every episode of this podcast.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Skip intro",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SkipSecondsChipsRow(
                    selectedSeconds = skipIntroSeconds,
                    onSelect = onSetSkipIntro,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Skip outro",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SkipSecondsChipsRow(
                    selectedSeconds = skipOutroSeconds,
                    onSelect = onSetSkipOutro,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun SkipSecondsChipsRow(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
) {
    val presets = listOf(0, 15, 30, 45, 60)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        presets.forEach { seconds ->
            FilterChip(
                selected = selectedSeconds == seconds,
                onClick = { onSelect(seconds) },
                label = {
                    Text(text = if (seconds == 0) "Off" else "${seconds}s")
                },
            )
        }
    }
}
