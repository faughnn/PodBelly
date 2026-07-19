package com.podbelly.core.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_ALWAYS
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_SMART

private data class AutoDownloadOption(
    val mode: Int,
    val title: String,
    val description: String,
)

private val OPTIONS = listOf(
    AutoDownloadOption(
        mode = AUTO_DOWNLOAD_SMART,
        title = "Smart",
        description = "Follow the smart auto-download setting: download new episodes only while you're listening to this show",
    ),
    AutoDownloadOption(
        mode = AUTO_DOWNLOAD_ALWAYS,
        title = "Always",
        description = "Download every new episode of this show, even when smart auto-download is off",
    ),
    AutoDownloadOption(
        mode = AUTO_DOWNLOAD_NEVER,
        title = "Never",
        description = "Never auto-download this show",
    ),
)

/**
 * Per-podcast auto-download override (Pocket Casts' per-podcast auto-download
 * setting). Selecting an option persists immediately and closes the dialog.
 */
@Composable
fun AutoDownloadModeDialog(
    currentMode: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-download") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentMode == option.mode,
                                onClick = {
                                    onSelect(option.mode)
                                    onDismiss()
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentMode == option.mode,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
