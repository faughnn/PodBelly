package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

/**
 * Mood-oriented theme catalog. Each theme is designed to serve a purpose:
 * calming, energizing, focusing, cozying, or easing night-time reading.
 */
object MoodThemes {

    // 2. Energize — bright, vivid, high-spirited light palette.
    private val energizeScheme = lightColorScheme(
        primary = Color(0xFFFF6A1A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC9),
        onPrimaryContainer = Color(0xFF4A1B00),
        secondary = Color(0xFFF5B800),
        onSecondary = Color(0xFF3A2C00),
        secondaryContainer = Color(0xFFFFEFB8),
        onSecondaryContainer = Color(0xFF453400),
        tertiary = Color(0xFFF0417E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD5E1),
        onTertiaryContainer = Color(0xFF500723),
        background = Color(0xFFFFFBF4),
        onBackground = Color(0xFF251A10),
        surface = Color(0xFFFFFDF9),
        onSurface = Color(0xFF251A10),
        surfaceVariant = Color(0xFFF3E6D6),
        onSurfaceVariant = Color(0xFF62513E),
        outline = Color(0xFF94806A),
        outlineVariant = Color(0xFFE4D3BF),
    )

    // 5. Night Shift — warm, low-blue-light dark palette for night reading.
    private val nightShiftScheme = darkColorScheme(
        primary = Color(0xFFE0A458),
        onPrimary = Color(0xFF3A2708),
        primaryContainer = Color(0xFF57411D),
        onPrimaryContainer = Color(0xFFF8E1BE),
        secondary = Color(0xFFD98A5B),
        onSecondary = Color(0xFF3B200D),
        secondaryContainer = Color(0xFF563620),
        onSecondaryContainer = Color(0xFFF8DAC6),
        tertiary = Color(0xFFC97A6A),
        onTertiary = Color(0xFF381712),
        tertiaryContainer = Color(0xFF522A22),
        onTertiaryContainer = Color(0xFFF6D2C8),
        background = Color(0xFF1A1410),
        onBackground = Color(0xFFEDDCC8),
        surface = Color(0xFF201911),
        onSurface = Color(0xFFEDDCC8),
        surfaceVariant = Color(0xFF33291D),
        onSurfaceVariant = Color(0xFFC7B49C),
        outline = Color(0xFF80705A),
        outlineVariant = Color(0xFF453829),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.ENERGIZE, "Energize", ThemeCategory.MOOD, energizeScheme),
        ThemeSpec(AppTheme.NIGHT_SHIFT, "Night Shift", ThemeCategory.MOOD, nightShiftScheme),
    )
}
