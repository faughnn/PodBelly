package com.podbelly.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import com.podbelly.core.common.DarkThemeMode
import org.junit.Rule
import org.junit.Test

class SettingsScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun defaultSettings() {
        paparazzi.snapshot {
            MaterialTheme {
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
                    }
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
                        item { SectionHeader(title = "Appearance") }

                        item {
                            SettingsCard {
                                ThemePickerRow(
                                    selectedMode = DarkThemeMode.SYSTEM,
                                    onModeSelected = {},
                                )
                            }
                        }

                        item { SectionHeader(title = "Playback") }

                        item {
                            SettingsCard {
                                PlaybackSpeedRow(
                                    currentSpeed = 1.0f,
                                    onSpeedChanged = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Skip silence",
                                    subtitle = "Automatically skip silent sections",
                                    checked = false,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Volume boost",
                                    subtitle = "Increase volume for quiet audio",
                                    checked = false,
                                    onCheckedChange = {},
                                )
                            }
                        }

                        item { SectionHeader(title = "Downloads") }

                        item {
                            SettingsCard {
                                SwitchRow(
                                    title = "Auto-download new episodes",
                                    subtitle = "Automatically download new episodes when they arrive",
                                    checked = false,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Download over Wi-Fi only",
                                    subtitle = "Prevent downloads on mobile data",
                                    checked = true,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Auto-delete played episodes",
                                    subtitle = "Remove downloaded files after playback completes",
                                    checked = false,
                                    onCheckedChange = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun customizedSettings() {
        paparazzi.snapshot {
            MaterialTheme {
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
                    }
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
                        item { SectionHeader(title = "Appearance") }

                        item {
                            SettingsCard {
                                ThemePickerRow(
                                    selectedMode = DarkThemeMode.DARK,
                                    onModeSelected = {},
                                )
                            }
                        }

                        item { SectionHeader(title = "Playback") }

                        item {
                            SettingsCard {
                                PlaybackSpeedRow(
                                    currentSpeed = 1.5f,
                                    onSpeedChanged = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Skip silence",
                                    subtitle = "Automatically skip silent sections",
                                    checked = true,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Volume boost",
                                    subtitle = "Increase volume for quiet audio",
                                    checked = true,
                                    onCheckedChange = {},
                                )
                            }
                        }

                        item { SectionHeader(title = "Downloads") }

                        item {
                            SettingsCard {
                                SwitchRow(
                                    title = "Auto-download new episodes",
                                    subtitle = "Automatically download new episodes when they arrive",
                                    checked = true,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                DropdownRow(
                                    title = "Auto-download count",
                                    selectedValue = "5",
                                    options = listOf("1", "3", "5", "10"),
                                    onOptionSelected = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Download over Wi-Fi only",
                                    subtitle = "Prevent downloads on mobile data",
                                    checked = true,
                                    onCheckedChange = {},
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                SwitchRow(
                                    title = "Auto-delete played episodes",
                                    subtitle = "Remove downloaded files after playback completes",
                                    checked = true,
                                    onCheckedChange = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun importExportSection() {
        paparazzi.snapshot {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SectionHeader(title = "Import / Export")

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
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "Import OPML")
                                }

                                Button(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "Export OPML")
                                }
                            }
                        }
                    }

                    SectionHeader(title = "About")

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
                        }
                    }
                }
            }
        }
    }
}
