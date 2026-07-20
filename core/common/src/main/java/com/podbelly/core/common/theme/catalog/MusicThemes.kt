package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object MusicThemes {

    private val lofiScheme = darkColorScheme(
        primary = Color(0xFFE0A3C0),
        onPrimary = Color(0xFF3A162A),
        primaryContainer = Color(0xFF5A3348),
        onPrimaryContainer = Color(0xFFF7D3E4),
        secondary = Color(0xFFA99BD1),
        onSecondary = Color(0xFF241638),
        secondaryContainer = Color(0xFF423658),
        onSecondaryContainer = Color(0xFFE0D6F3),
        tertiary = Color(0xFFD8B78C),
        onTertiary = Color(0xFF332107),
        tertiaryContainer = Color(0xFF554123),
        onTertiaryContainer = Color(0xFFF4DEC3),
        background = Color(0xFF241E2E),
        onBackground = Color(0xFFE9E1F0),
        surface = Color(0xFF241E2E),
        onSurface = Color(0xFFE9E1F0),
        surfaceVariant = Color(0xFF322A3E),
        onSurfaceVariant = Color(0xFFB6ABC4),
        outline = Color(0xFF837791),
        outlineVariant = Color(0xFF453B52),
    )

    private val punkScheme = darkColorScheme(
        primary = Color(0xFFFF1F6B),
        onPrimary = Color(0xFF33000F),
        primaryContainer = Color(0xFF7A0A31),
        onPrimaryContainer = Color(0xFFFFCADB),
        secondary = Color(0xFFF5E63D),
        onSecondary = Color(0xFF2E2A00),
        secondaryContainer = Color(0xFF5C5400),
        onSecondaryContainer = Color(0xFFFBF4A6),
        tertiary = Color(0xFF2CE0E0),
        onTertiary = Color(0xFF00302F),
        tertiaryContainer = Color(0xFF00524F),
        onTertiaryContainer = Color(0xFFACF6F3),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF161616),
        onSurfaceVariant = Color(0xFFB8B8B8),
        outline = Color(0xFF7A7A7A),
        outlineVariant = Color(0xFF2C2C2C),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.LOFI, "Lo-fi", ThemeCategory.MUSIC, lofiScheme),
        ThemeSpec(AppTheme.PUNK, "Punk", ThemeCategory.MUSIC, punkScheme),
    )
}
