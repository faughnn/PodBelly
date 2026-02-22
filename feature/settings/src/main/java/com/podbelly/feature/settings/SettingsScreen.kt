package com.podbelly.feature.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podbelly.core.common.AppTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToStats: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // File picker for OPML import
    val opmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = readTextFromUri(context, it)
            if (content != null) {
                viewModel.importOpml(content)
            }
        }
    }

    // Show snackbar when importExportMessage changes
    LaunchedEffect(uiState.importExportMessage) {
        uiState.importExportMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance ────────────────────────────────────────────

            item { SectionHeader(title = "Appearance") }

            item {
                SettingsCard {
                    ThemePickerRow(
                        selectedMode = uiState.appTheme,
                        onModeSelected = { viewModel.setAppTheme(it) },
                    )
                }
            }

            // ── Playback ──────────────────────────────────────────────

            item { SectionHeader(title = "Playback") }

            item {
                SettingsCard {
                    PlaybackSpeedRow(
                        currentSpeed = uiState.defaultPlaybackSpeed,
                        onSpeedChanged = { viewModel.setDefaultPlaybackSpeed(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchRow(
                        title = "Skip silence",
                        subtitle = "Automatically skip silent sections",
                        checked = uiState.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchRow(
                        title = "Volume boost",
                        subtitle = "Increase volume for quiet audio",
                        checked = uiState.volumeBoost,
                        onCheckedChange = { viewModel.setVolumeBoost(it) },
                    )
                }
            }

            // ── Downloads ─────────────────────────────────────────────

            item { SectionHeader(title = "Downloads") }

            item {
                SettingsCard {
                    SwitchRow(
                        title = "Auto-download new episodes",
                        subtitle = "Automatically download new episodes when they arrive",
                        checked = uiState.autoDownloadEnabled,
                        onCheckedChange = { viewModel.setAutoDownload(it) },
                    )

                    AnimatedVisibility(visible = uiState.autoDownloadEnabled) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            DropdownRow(
                                title = "Auto-download count",
                                selectedValue = uiState.autoDownloadEpisodeCount.toString(),
                                options = listOf("1", "3", "5", "10"),
                                onOptionSelected = { viewModel.setAutoDownloadEpisodeCount(it.toInt()) },
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchRow(
                        title = "Download over Wi-Fi only",
                        subtitle = "Prevent downloads on mobile data",
                        checked = uiState.downloadOnWifiOnly,
                        onCheckedChange = { viewModel.setDownloadOnWifiOnly(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchRow(
                        title = "Auto-delete played episodes",
                        subtitle = "Remove downloaded files after playback completes",
                        checked = uiState.autoDeletePlayed,
                        onCheckedChange = { viewModel.setAutoDeletePlayed(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    DeleteAllDownloadsRow(
                        onConfirm = { viewModel.deleteAllDownloads() },
                    )
                }
            }

            // ── Feed Management ───────────────────────────────────────

            item { SectionHeader(title = "Feed Management") }

            item {
                SettingsCard {
                    DropdownRow(
                        title = "Refresh interval",
                        selectedValue = formatRefreshInterval(uiState.feedRefreshIntervalMinutes),
                        options = listOf(
                            "15 min",
                            "30 min",
                            "1 hour",
                            "2 hours",
                            "4 hours",
                            "12 hours",
                            "Manual only",
                        ),
                        onOptionSelected = { option ->
                            viewModel.setFeedRefreshInterval(parseRefreshInterval(option))
                        },
                    )
                }
            }

            // ── Import / Export ───────────────────────────────────────

            item { SectionHeader(title = "Import / Export") }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Transfer your podcast subscriptions using OPML files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { opmlPickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "Import OPML")
                            }

                            Button(
                                onClick = {
                                    viewModel.exportOpml { xml ->
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/xml"
                                            putExtra(Intent.EXTRA_TEXT, xml)
                                            putExtra(Intent.EXTRA_SUBJECT, "Podbelly Subscriptions")
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Share OPML")
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "Export OPML")
                            }
                        }
                    }
                }
            }

            // ── Statistics ─────────────────────────────────────────────

            item { SectionHeader(title = "Statistics") }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToStats() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Listening Statistics",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "View your listening history and stats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────

            item { SectionHeader(title = "About") }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Podbelly",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Podbelly - Ad-free podcast player",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// Reusable setting components
// =====================================================================

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        content()
    }
}

// ── Switch row ────────────────────────────────────────────────────────

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ── Dropdown row ──────────────────────────────────────────────────────

@Composable
internal fun DropdownRow(
    title: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = selectedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Theme picker row ──────────────────────────────────────────────────

@Composable
internal fun ThemePickerRow(
    selectedMode: AppTheme,
    onModeSelected: (AppTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.selectableGroup()) {
            AppTheme.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMode == mode,
                            onClick = { onModeSelected(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (mode) {
                            AppTheme.SYSTEM -> "System default"
                            AppTheme.LIGHT -> "Light"
                            AppTheme.DARK -> "Dark"
                            AppTheme.OLED_DARK -> "OLED Dark"
                            AppTheme.HIGH_CONTRAST -> "High Contrast"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ── Delete all downloads row ──────────────────────────────────────────

@Composable
internal fun DeleteAllDownloadsRow(
    onConfirm: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Delete all downloads",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Remove all downloaded episode files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete all downloads?") },
            text = { Text("This will remove all downloaded episode files from your device. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onConfirm()
                    }
                ) {
                    Text(
                        "Delete All",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ── Playback speed row ────────────────────────────────────────────────

@Composable
internal fun PlaybackSpeedRow(
    currentSpeed: Float,
    onSpeedChanged: (Float) -> Unit,
) {
    // Internal state for the slider so it feels responsive while dragging.
    var sliderValue by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Default playback speed",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = formatSpeed(sliderValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                // Snap to 0.1 increments.
                sliderValue = (newValue * 10).roundToInt() / 10f
            },
            onValueChangeFinished = { onSpeedChanged(sliderValue) },
            valueRange = 0.5f..3.0f,
            steps = 24, // (3.0 - 0.5) / 0.1 - 1 = 24 steps between
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0.5x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "3.0x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =====================================================================
// Utility functions
// =====================================================================

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}.0x"
    } else {
        "${"%.1f".format(speed)}x"
    }
}

private fun readTextFromUri(context: Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatRefreshInterval(minutes: Int): String {
    return when (minutes) {
        15 -> "15 min"
        30 -> "30 min"
        60 -> "1 hour"
        120 -> "2 hours"
        240 -> "4 hours"
        720 -> "12 hours"
        0 -> "Manual only"
        else -> "$minutes min"
    }
}

private fun parseRefreshInterval(option: String): Int {
    return when (option) {
        "15 min" -> 15
        "30 min" -> 30
        "1 hour" -> 60
        "2 hours" -> 120
        "4 hours" -> 240
        "12 hours" -> 720
        "Manual only" -> 0
        else -> 60
    }
}
