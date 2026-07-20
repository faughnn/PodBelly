package com.podbelly.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The complete release history: every What's New entry ever shipped, newest
 * first. Reached from the About card on the You tab — the once-per-update
 * dialog shows only what changed since the last install, this keeps the rest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val currentVersion = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            info.versionName.orEmpty() to info.versionCode
        } catch (_: Exception) {
            "" to 0
        }
    }
    val releases = remember { WhatsNew.changelog.entries.sortedByDescending { it.key } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Version history",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = releases,
                key = { it.key },
            ) { (versionCode, changes) ->
                VersionCard(
                    title = versionTitle(
                        versionCode = versionCode,
                        currentVersionName = currentVersion.first,
                        currentVersionCode = currentVersion.second,
                    ),
                    changes = changes,
                )
            }
        }
    }
}

/**
 * Header for one release. Only the installed version's marketing name is known
 * at runtime — historical entries are keyed by version code alone.
 */
internal fun versionTitle(
    versionCode: Int,
    currentVersionName: String,
    currentVersionCode: Int,
): String = if (versionCode == currentVersionCode && currentVersionName.isNotBlank()) {
    "v$currentVersionName · current"
} else {
    "Version $versionCode"
}

@Composable
private fun VersionCard(
    title: String,
    changes: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            changes.forEach { change ->
                Text(
                    text = "•  $change",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
