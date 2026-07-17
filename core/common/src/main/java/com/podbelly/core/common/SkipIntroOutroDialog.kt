package com.podbelly.core.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Per-podcast "Skip intro" / "Skip outro" settings (AntennaPod's per-feed skip
 * pattern, including its free-form seconds input). Values persist when Save is
 * tapped. Shared by the podcast page menu and the Now Playing screen menu.
 *
 * When opened from the player, [currentPositionSeconds] and [remainingSeconds]
 * enable one-tap shortcuts: pause where the intro ads end and "Up to now" fills
 * the intro with the current position; pause where the closing ads begin and
 * "After now" fills the outro with the time remaining.
 */
@Composable
fun SkipIntroOutroDialog(
    skipIntroSeconds: Int,
    skipOutroSeconds: Int,
    onSetSkipIntro: (Int) -> Unit,
    onSetSkipOutro: (Int) -> Unit,
    onDismiss: () -> Unit,
    currentPositionSeconds: Int? = null,
    remainingSeconds: Int? = null,
) {
    // Keyed on the incoming values so settings that finish loading after the
    // dialog opens still pre-fill the fields.
    var introText by remember(skipIntroSeconds) {
        mutableStateOf(if (skipIntroSeconds > 0) skipIntroSeconds.toString() else "")
    }
    var outroText by remember(skipOutroSeconds) {
        mutableStateOf(if (skipOutroSeconds > 0) skipOutroSeconds.toString() else "")
    }

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

                SkipSecondsField(
                    label = "Skip intro",
                    value = introText,
                    onValueChange = { introText = it },
                    fillLabel = currentPositionSeconds?.let { "Up to now (${formatSeconds(it)})" },
                    onFill = { currentPositionSeconds?.let { introText = it.toString() } },
                )

                Spacer(modifier = Modifier.height(12.dp))

                SkipSecondsField(
                    label = "Skip outro",
                    value = outroText,
                    onValueChange = { outroText = it },
                    fillLabel = remainingSeconds?.let { "After now (${formatSeconds(it)})" },
                    onFill = { remainingSeconds?.let { outroText = it.toString() } },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSetSkipIntro(introText.toIntOrNull() ?: 0)
                    onSetSkipOutro(outroText.toIntOrNull() ?: 0)
                    onDismiss()
                },
            ) {
                Text("Save")
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
private fun SkipSecondsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    fillLabel: String?,
    onFill: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            // Digits only, capped well past any plausible intro/outro length.
            onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(5)) },
            modifier = Modifier.width(110.dp),
            singleLine = true,
            placeholder = { Text("0") },
            suffix = { Text("s") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (fillLabel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onFill) {
                Text(fillLabel)
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
