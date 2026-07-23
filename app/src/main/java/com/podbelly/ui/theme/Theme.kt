package com.podbelly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.LocalAppTheme
import com.podbelly.core.common.theme.ThemeCatalog

@Composable
fun PodbellTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = ThemeCatalog.colorSchemeFor(appTheme, isSystemInDarkTheme())

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PodbellTypography,
            content = content
        )
    }
}

/** Backward-compatible overload. */
@Composable
fun PodbellTheme(
    darkThemeMode: AppTheme = AppTheme.SYSTEM,
    @Suppress("UNUSED_PARAMETER") useDarkThemeMode: Boolean = true,
    content: @Composable () -> Unit
) {
    PodbellTheme(appTheme = darkThemeMode, content = content)
}
