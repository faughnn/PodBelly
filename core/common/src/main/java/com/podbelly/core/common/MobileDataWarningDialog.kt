package com.podbelly.core.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun MobileDataWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download not available") },
        text = {
            Text(
                "Wi-Fi only downloading is enabled and you are currently on mobile data. " +
                    "Connect to Wi-Fi or disable this restriction in Settings.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
