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

    // 1. Calm — soothing spa palette, soft pale blue-green surfaces.
    private val calmScheme = lightColorScheme(
        primary = Color(0xFF4FA5A0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCDEAE7),
        onPrimaryContainer = Color(0xFF0C302E),
        secondary = Color(0xFF5E86A8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5E4F1),
        onSecondaryContainer = Color(0xFF122735),
        tertiary = Color(0xFF7C9A78),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDCE9D8),
        onTertiaryContainer = Color(0xFF1E2C1B),
        background = Color(0xFFEEF5F4),
        onBackground = Color(0xFF1B2A2A),
        surface = Color(0xFFF6FAF9),
        onSurface = Color(0xFF1B2A2A),
        surfaceVariant = Color(0xFFDCE7E5),
        onSurfaceVariant = Color(0xFF49605E),
        outline = Color(0xFF799391),
        outlineVariant = Color(0xFFC3D3D1),
    )

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

    // 3. Deep Focus — minimal, low-distraction dark palette, single steel-blue accent.
    private val deepFocusScheme = darkColorScheme(
        primary = Color(0xFF5B8D9E),
        onPrimary = Color(0xFF071319),
        primaryContainer = Color(0xFF294049),
        onPrimaryContainer = Color(0xFFC5DEE8),
        secondary = Color(0xFF8A929A),
        onSecondary = Color(0xFF14181C),
        secondaryContainer = Color(0xFF333A40),
        onSecondaryContainer = Color(0xFFD3D9DE),
        tertiary = Color(0xFF9AA0A6),
        onTertiary = Color(0xFF14171A),
        tertiaryContainer = Color(0xFF383C40),
        onTertiaryContainer = Color(0xFFDCE0E4),
        background = Color(0xFF12151A),
        onBackground = Color(0xFFDCE1E6),
        surface = Color(0xFF161A20),
        onSurface = Color(0xFFDCE1E6),
        surfaceVariant = Color(0xFF232830),
        onSurfaceVariant = Color(0xFFA6AEB6),
        outline = Color(0xFF5C646C),
        outlineVariant = Color(0xFF31373E),
    )

    // 4. Cozy — warm dim evening dark palette, amber and terracotta.
    private val cozyScheme = darkColorScheme(
        primary = Color(0xFFD9A05B),
        onPrimary = Color(0xFF3A2609),
        primaryContainer = Color(0xFF56401F),
        onPrimaryContainer = Color(0xFFF6DEBB),
        secondary = Color(0xFFCE7F5E),
        onSecondary = Color(0xFF3A1B0C),
        secondaryContainer = Color(0xFF553121),
        onSecondaryContainer = Color(0xFFF7D6C6),
        tertiary = Color(0xFFCBA94F),
        onTertiary = Color(0xFF352A06),
        tertiaryContainer = Color(0xFF4F4118),
        onTertiaryContainer = Color(0xFFF3E2B4),
        background = Color(0xFF1E1712),
        onBackground = Color(0xFFEBDDD0),
        surface = Color(0xFF241C16),
        onSurface = Color(0xFFEBDDD0),
        surfaceVariant = Color(0xFF352A21),
        onSurfaceVariant = Color(0xFFC1AF9E),
        outline = Color(0xFF7C6C5B),
        outlineVariant = Color(0xFF43362B),
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
        ThemeSpec(AppTheme.CALM, "Calm", ThemeCategory.MOOD, calmScheme),
        ThemeSpec(AppTheme.ENERGIZE, "Energize", ThemeCategory.MOOD, energizeScheme),
        ThemeSpec(AppTheme.DEEP_FOCUS, "Deep Focus", ThemeCategory.MOOD, deepFocusScheme),
        ThemeSpec(AppTheme.COZY, "Cozy", ThemeCategory.MOOD, cozyScheme),
        ThemeSpec(AppTheme.NIGHT_SHIFT, "Night Shift", ThemeCategory.MOOD, nightShiftScheme),
    )
}
