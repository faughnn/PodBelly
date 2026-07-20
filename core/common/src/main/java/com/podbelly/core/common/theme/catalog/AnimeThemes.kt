package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object AnimeThemes {

    private val ShonenScheme = darkColorScheme(
        primary = Color(0xFFFF6A1A),
        onPrimary = Color(0xFF2A1000),
        primaryContainer = Color(0xFF7A2F00),
        onPrimaryContainer = Color(0xFFFFD9C2),
        secondary = Color(0xFF2E7FE0),
        onSecondary = Color(0xFF001A33),
        secondaryContainer = Color(0xFF10375E),
        onSecondaryContainer = Color(0xFFCFE2FF),
        tertiary = Color(0xFFF2C438),
        onTertiary = Color(0xFF332800),
        tertiaryContainer = Color(0xFF5C4A00),
        onTertiaryContainer = Color(0xFFFFF0C2),
        background = Color(0xFF14110D),
        onBackground = Color(0xFFF2EAE0),
        surface = Color(0xFF1B1712),
        onSurface = Color(0xFFF2EAE0),
        surfaceVariant = Color(0xFF2A241D),
        onSurfaceVariant = Color(0xFFC9BEB0),
        outline = Color(0xFF8A7F70),
        outlineVariant = Color(0xFF453E34),
    )

    private val MechaScheme = darkColorScheme(
        primary = Color(0xFF2E5BD0),
        onPrimary = Color(0xFFEAF0FF),
        primaryContainer = Color(0xFF12296B),
        onPrimaryContainer = Color(0xFFC9D8FF),
        secondary = Color(0xFFD93A3A),
        onSecondary = Color(0xFF2E0000),
        secondaryContainer = Color(0xFF6B1414),
        onSecondaryContainer = Color(0xFFFFD3D0),
        tertiary = Color(0xFFF2C438),
        onTertiary = Color(0xFF332800),
        tertiaryContainer = Color(0xFF5C4A00),
        onTertiaryContainer = Color(0xFFFFF0C2),
        background = Color(0xFF101318),
        onBackground = Color(0xFFE3E7EE),
        surface = Color(0xFF171B22),
        onSurface = Color(0xFFE3E7EE),
        surfaceVariant = Color(0xFF262C36),
        onSurfaceVariant = Color(0xFFBAC2CE),
        outline = Color(0xFF7C8593),
        outlineVariant = Color(0xFF3B424D),
    )

    private val CyberRoninScheme = darkColorScheme(
        primary = Color(0xFFFF2E5E),
        onPrimary = Color(0xFF33020F),
        primaryContainer = Color(0xFF7A0A29),
        onPrimaryContainer = Color(0xFFFFD3DE),
        secondary = Color(0xFF2EE0E0),
        onSecondary = Color(0xFF002A2A),
        secondaryContainer = Color(0xFF0A4D4D),
        onSecondaryContainer = Color(0xFFC2FBFB),
        tertiary = Color(0xFFE8C34A),
        onTertiary = Color(0xFF332700),
        tertiaryContainer = Color(0xFF5C4700),
        onTertiaryContainer = Color(0xFFFFEFC0),
        background = Color(0xFF14122A),
        onBackground = Color(0xFFE6E3F5),
        surface = Color(0xFF1B1836),
        onSurface = Color(0xFFE6E3F5),
        surfaceVariant = Color(0xFF2A2648),
        onSurfaceVariant = Color(0xFFBBB6D6),
        outline = Color(0xFF7C77A0),
        outlineVariant = Color(0xFF3D3960),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.SHONEN, "Shonen", ThemeCategory.ANIME, ShonenScheme),
        ThemeSpec(AppTheme.MECHA, "Mecha", ThemeCategory.ANIME, MechaScheme),
        ThemeSpec(AppTheme.CYBER_RONIN, "Cyber Ronin", ThemeCategory.ANIME, CyberRoninScheme),
    )
}
