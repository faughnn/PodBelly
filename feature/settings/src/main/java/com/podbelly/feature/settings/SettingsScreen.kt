package com.podbelly.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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

/**
 * The settings categories reachable from the Profile tab. Each opens its own
 * small [SettingsScreen] (the hub-and-spoke pattern Pocket Casts and AntennaPod
 * use) instead of one endless scroll.
 */
enum class SettingsSection(val key: String, val title: String, val subtitle: String) {
    PLAYBACK("playback", "Playback", "Speeds, silence, ad chapters, volume"),
    DOWNLOADS("downloads", "Downloads", "Auto-download, cleanup, storage"),
    APPEARANCE("appearance", "Appearance", "Theme"),
    FEEDS("feeds", "Feeds", "Refresh interval"),
    BACKUP("backup", "Import & Export", "Move subscriptions via OPML"),
    DIAGNOSTICS("diagnostics", "Diagnostics", "Crash logs");

    companion object {
        fun fromKey(key: String?): SettingsSection =
            entries.firstOrNull { it.key == key } ?: PLAYBACK
    }
}

/**
 * One settings category as its own screen. The category is picked on the
 * Profile tab; this screen shows just that category's options under a
 * back-arrow app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    section: SettingsSection,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToPlaybackSpeeds: () -> Unit = {},
    onNavigateToAutoDownload: () -> Unit = {},
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

    // Show snackbar for export/delete messages only
    LaunchedEffect(uiState.importExportMessage) {
        uiState.importExportMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // Show persistent dialog for import results
    uiState.importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { viewModel.clearImportResult() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = section.title,
                        fontWeight = FontWeight.Bold,
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
            when (section) {
                SettingsSection.APPEARANCE -> item {
                    SettingsCard {
                        ThemePickerRow(
                            selectedMode = uiState.appTheme,
                            onModeSelected = { viewModel.setAppTheme(it) },
                        )
                    }
                }

                SettingsSection.PLAYBACK -> item {
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPlaybackSpeeds() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Playback speeds",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "Per-podcast speed settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SwitchRow(
                            title = "Skip silence",
                            subtitle = "Automatically skip silent sections",
                            checked = uiState.skipSilence,
                            onCheckedChange = { viewModel.setSkipSilence(it) },
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SwitchRow(
                            title = "Skip ad chapters",
                            subtitle = "Jump over chapters marked as ads or sponsors by the show",
                            checked = uiState.skipAdChapters,
                            onCheckedChange = { viewModel.setSkipAdChapters(it) },
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SwitchRow(
                            title = "Volume boost",
                            subtitle = "Extra-loud mode for noisy environments",
                            checked = uiState.volumeBoost,
                            onCheckedChange = { viewModel.setVolumeBoost(it) },
                        )
                    }
                }

                SettingsSection.DOWNLOADS -> item {
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
                            title = "Smart auto-download",
                            subtitle = "Download new episodes from shows you've listened to recently",
                            checked = uiState.smartAutoDownload,
                            onCheckedChange = { viewModel.setSmartAutoDownload(it) },
                        )

                        AnimatedVisibility(visible = uiState.smartAutoDownload) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                DropdownRow(
                                    title = "Listened within",
                                    selectedValue = formatSmartWindowDays(uiState.smartAutoDownloadWindowDays),
                                    options = listOf("7 days", "14 days", "30 days", "60 days"),
                                    onOptionSelected = {
                                        viewModel.setSmartAutoDownloadWindowDays(parseSmartWindowDays(it))
                                    },
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                DropdownRow(
                                    title = "Keep per show",
                                    selectedValue = formatKeepPerShow(uiState.smartAutoDownloadKeepPerShow),
                                    options = listOf("All", "1 newest", "3 newest", "5 newest", "10 newest"),
                                    onOptionSelected = {
                                        viewModel.setSmartAutoDownloadKeepPerShow(parseKeepPerShow(it))
                                    },
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                SwitchRow(
                                    title = "Only while charging",
                                    subtitle = "Defer auto-downloads until the phone is plugged in",
                                    checked = uiState.smartAutoDownloadChargingOnly,
                                    onCheckedChange = { viewModel.setSmartAutoDownloadChargingOnly(it) },
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAutoDownload() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Auto-download per show",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "Always or never download specific shows, with your listening stats to guide you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        DropdownRow(
                            title = "Auto-delete played downloads",
                            selectedValue = formatAutoDeleteDays(uiState.autoDeletePlayedAfterDays),
                            options = listOf("Off", "After 1 day", "After 3 days", "After 7 days", "After 30 days"),
                            onOptionSelected = { viewModel.setAutoDeletePlayedAfterDays(parseAutoDeleteDays(it)) },
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        DeleteAllDownloadsRow(
                            totalDownloadedBytes = uiState.totalDownloadedBytes,
                            onConfirm = { viewModel.deleteAllDownloads() },
                        )
                    }
                }

                SettingsSection.FEEDS -> item {
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

                SettingsSection.BACKUP -> item {
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

                SettingsSection.DIAGNOSTICS -> item {
                    val versionName = rememberVersionName()
                    SettingsCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "If the app crashes, the stack trace is saved locally. Share it to help diagnose the problem.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearCrashLogs() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "Clear logs")
                                }

                                Button(
                                    onClick = {
                                        viewModel.shareCrashLogs { content ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, content)
                                                putExtra(
                                                    Intent.EXTRA_SUBJECT,
                                                    "Podbelly crash logs (v$versionName)",
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Share crash logs")
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "Share crash logs")
                                }
                            }
                        }
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
                        text = mode.displayName,
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
    totalDownloadedBytes: Long,
    onConfirm: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val hasDownloads = totalDownloadedBytes > 0
    val formattedSize = remember(totalDownloadedBytes) {
        android.text.format.Formatter.formatShortFileSize(context, totalDownloadedBytes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasDownloads) { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Delete all downloads",
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasDownloads) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = if (hasDownloads) {
                    "Remove all downloaded episode files"
                } else {
                    "No downloaded episodes"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formattedSize,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete all downloads?") },
            text = {
                Text("This will remove $formattedSize of downloaded episode files from your device. This cannot be undone.")
            },
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

// ── Import result dialog ──────────────────────────────────────────────

@Composable
internal fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Complete") },
        text = {
            Column {
                Text(
                    text = "Imported ${result.imported} of ${result.total} subscription(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (result.skipped > 0) {
                    Text(
                        text = "(${result.skipped} already existed)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (result.failed.isNotEmpty()) {
                    Text(
                        text = "Failed to import:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    result.failed.forEach { name ->
                        Text(
                            text = "\u2022  $name",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

// =====================================================================
// Utility functions
// =====================================================================

/** The installed app version, read once per composition site. */
@Composable
internal fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
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

internal fun formatSmartWindowDays(days: Int): String = "$days days"

internal fun parseSmartWindowDays(option: String): Int =
    option.removeSuffix(" days").toIntOrNull() ?: 30

internal fun formatKeepPerShow(count: Int): String =
    if (count <= 0) "All" else "$count newest"

internal fun parseKeepPerShow(option: String): Int =
    if (option == "All") 0 else option.removeSuffix(" newest").toIntOrNull() ?: 0

internal fun formatAutoDeleteDays(days: Int): String = when (days) {
    0 -> "Off"
    1 -> "After 1 day"
    else -> "After $days days"
}

internal fun parseAutoDeleteDays(option: String): Int = when (option) {
    "Off" -> 0
    "After 1 day" -> 1
    else -> option.removePrefix("After ").removeSuffix(" days").toIntOrNull() ?: 0
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
