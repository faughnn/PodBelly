package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

/**
 * The baseline themes: system-following, Jukebox light and dark, a pure-black
 * OLED variant, and a high-contrast scheme.
 *
 * [light] and [dark] are exposed because the system-following entry resolves
 * onto them at render time (see ThemeCatalog.colorSchemeFor).
 */
object ClassicThemes {

    // ── Jukebox Light ──────────────────────────────────────────────
    val light = lightColorScheme(
        primary = Color(0xFFE84848),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE0E0),
        onPrimaryContainer = Color(0xFF5A0000),
        secondary = Color(0xFF2AAA9F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD0F5F2),
        onSecondaryContainer = Color(0xFF003D38),
        tertiary = Color(0xFFE08800),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE0B0),
        onTertiaryContainer = Color(0xFF3D2200),
        error = Color(0xFFE84848),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE0E0),
        onErrorContainer = Color(0xFF5A0000),
        background = Color(0xFFFAFBFF),
        onBackground = Color(0xFF0F1218),
        surface = Color(0xFFFAFBFF),
        onSurface = Color(0xFF0F1218),
        surfaceVariant = Color(0xFFF0F2FA),
        onSurfaceVariant = Color(0xFF5A6480),
        outline = Color(0xFFC0C8D8),
        outlineVariant = Color(0xFFE0E4F0),
        inverseSurface = Color(0xFF0F1218),
        inverseOnSurface = Color(0xFFFAFBFF),
        inversePrimary = Color(0xFFFF8080),
    )

    // ── Jukebox Dark ───────────────────────────────────────────────
    val dark = darkColorScheme(
        primary = Color(0xFFFF6B6B),
        onPrimary = Color(0xFF0F1218),
        primaryContainer = Color(0xFF3D1A1A),
        onPrimaryContainer = Color(0xFFFFB4B4),
        secondary = Color(0xFF4ECDC4),
        onSecondary = Color(0xFF0A1210),
        secondaryContainer = Color(0xFF1A3D3A),
        onSecondaryContainer = Color(0xFFA8E8E4),
        tertiary = Color(0xFFFFA502),
        onTertiary = Color(0xFF1A0E00),
        tertiaryContainer = Color(0xFF3D2800),
        onTertiaryContainer = Color(0xFFFFD080),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF0F1218),
        errorContainer = Color(0xFF3D1A1A),
        onErrorContainer = Color(0xFFFFB4B4),
        background = Color(0xFF0A0A12),
        onBackground = Color(0xFFE0E4F0),
        surface = Color(0xFF0A0A12),
        onSurface = Color(0xFFE0E4F0),
        surfaceVariant = Color(0xFF1A1F2E),
        onSurfaceVariant = Color(0xFF8892A8),
        outline = Color(0xFF3A4060),
        outlineVariant = Color(0xFF252B3D),
        inverseSurface = Color(0xFFE0E4F0),
        inverseOnSurface = Color(0xFF0F1218),
        inversePrimary = Color(0xFFC62828),
    )

    // ── OLED Dark — pure black backgrounds, same accents ───────────
    private val oled = darkColorScheme(
        primary = Color(0xFFFF6B6B),
        onPrimary = Color(0xFF0F1218),
        primaryContainer = Color(0xFF3D1A1A),
        onPrimaryContainer = Color(0xFFFFB4B4),
        secondary = Color(0xFF4ECDC4),
        onSecondary = Color(0xFF0A1210),
        secondaryContainer = Color(0xFF1A3D3A),
        onSecondaryContainer = Color(0xFFA8E8E4),
        tertiary = Color(0xFFFFA502),
        onTertiary = Color(0xFF1A0E00),
        tertiaryContainer = Color(0xFF3D2800),
        onTertiaryContainer = Color(0xFFFFD080),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF0F1218),
        errorContainer = Color(0xFF3D1A1A),
        onErrorContainer = Color(0xFFFFB4B4),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE0E4F0),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFE0E4F0),
        surfaceVariant = Color(0xFF0A0A12),
        onSurfaceVariant = Color(0xFF8892A8),
        outline = Color(0xFF3A4060),
        outlineVariant = Color(0xFF252B3D),
        inverseSurface = Color(0xFFE0E4F0),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFFC62828),
    )

    // ── High Contrast — brighter colors, maximum readability ───────
    private val highContrast = darkColorScheme(
        primary = Color(0xFFFF8080),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFFF6B6B),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF70E8E0),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF4ECDC4),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFFFBB40),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFFFFA502),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFFFF8080),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFFFF6B6B),
        onErrorContainer = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1A1F2E),
        onSurfaceVariant = Color(0xFFFFFFFF),
        outline = Color(0xFF8892A8),
        outlineVariant = Color(0xFF5A6480),
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFFC62828),
    )

    val specs: List<ThemeSpec> = listOf(
        // System-following: colorScheme is null; resolved at render time.
        ThemeSpec(AppTheme.SYSTEM, "System default", ThemeCategory.CLASSIC, null),
        ThemeSpec(AppTheme.LIGHT, "Light", ThemeCategory.CLASSIC, light),
        ThemeSpec(AppTheme.DARK, "Dark", ThemeCategory.CLASSIC, dark),
        ThemeSpec(AppTheme.OLED_DARK, "OLED Dark", ThemeCategory.CLASSIC, oled),
        ThemeSpec(AppTheme.HIGH_CONTRAST, "High Contrast", ThemeCategory.CLASSIC, highContrast),
    )
}
