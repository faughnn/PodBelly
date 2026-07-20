package com.podbelly.core.common.theme

import androidx.compose.material3.ColorScheme
import com.podbelly.core.common.AppTheme

/**
 * A single selectable appearance theme: its stable identity, the category it
 * belongs to, its display name, and the Material 3 color scheme it applies.
 *
 * [colorScheme] is null only for the system-following entry ([AppTheme.SYSTEM]),
 * which resolves to a light or dark scheme at render time — see
 * [ThemeCatalog.colorSchemeFor].
 */
data class ThemeSpec(
    val id: AppTheme,
    val displayName: String,
    val category: ThemeCategory,
    val colorScheme: ColorScheme?,
)
