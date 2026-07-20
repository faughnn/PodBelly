package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object SpaceThemes {

    private val nebulaScheme = darkColorScheme(
        primary = Color(0xFFE05BD0),
        onPrimary = Color(0xFF2C0426),
        primaryContainer = Color(0xFF5C1252),
        onPrimaryContainer = Color(0xFFFAD3F2),
        secondary = Color(0xFF9B6BFF),
        onSecondary = Color(0xFF1E0A45),
        secondaryContainer = Color(0xFF3A1C77),
        onSecondaryContainer = Color(0xFFE3D3FF),
        tertiary = Color(0xFF3FD0C6),
        onTertiary = Color(0xFF00302C),
        tertiaryContainer = Color(0xFF0E4E49),
        onTertiaryContainer = Color(0xFFB8F5EF),
        background = Color(0xFF120A24),
        onBackground = Color(0xFFEBE2F5),
        surface = Color(0xFF160D2B),
        onSurface = Color(0xFFEBE2F5),
        surfaceVariant = Color(0xFF241A3D),
        onSurfaceVariant = Color(0xFFBCAFCE),
        outline = Color(0xFF6E5E85),
        outlineVariant = Color(0xFF3A2E52),
    )

    private val marsScheme = darkColorScheme(
        primary = Color(0xFFC1440E),
        onPrimary = Color(0xFF2A0C02),
        primaryContainer = Color(0xFF67230A),
        onPrimaryContainer = Color(0xFFFFD3C0),
        secondary = Color(0xFFD98B4A),
        onSecondary = Color(0xFF321A05),
        secondaryContainer = Color(0xFF6B3E13),
        onSecondaryContainer = Color(0xFFFEE0C4),
        tertiary = Color(0xFFA8C7E0),
        onTertiary = Color(0xFF0A1E2C),
        tertiaryContainer = Color(0xFF2E4759),
        onTertiaryContainer = Color(0xFFD6E7F4),
        background = Color(0xFF1E0F0A),
        onBackground = Color(0xFFF3E0D8),
        surface = Color(0xFF25130C),
        onSurface = Color(0xFFF3E0D8),
        surfaceVariant = Color(0xFF3A2018),
        onSurfaceVariant = Color(0xFFD1B0A2),
        outline = Color(0xFF8A5F4E),
        outlineVariant = Color(0xFF4E2E22),
    )

    private val galaxyScheme = darkColorScheme(
        primary = Color(0xFF7C8CFF),
        onPrimary = Color(0xFF0A1147),
        primaryContainer = Color(0xFF25307A),
        onPrimaryContainer = Color(0xFFD7DCFF),
        secondary = Color(0xFFB98BFF),
        onSecondary = Color(0xFF260A47),
        secondaryContainer = Color(0xFF442078),
        onSecondaryContainer = Color(0xFFEBD9FF),
        tertiary = Color(0xFFFFE9A8),
        onTertiary = Color(0xFF352A00),
        tertiaryContainer = Color(0xFF574700),
        onTertiaryContainer = Color(0xFFFFF3CE),
        background = Color(0xFF0B0A26),
        onBackground = Color(0xFFE6E5F7),
        surface = Color(0xFF11102F),
        onSurface = Color(0xFFE6E5F7),
        surfaceVariant = Color(0xFF20204A),
        onSurfaceVariant = Color(0xFFB6B5D6),
        outline = Color(0xFF63639A),
        outlineVariant = Color(0xFF32325E),
    )

    private val deepSpaceScheme = darkColorScheme(
        primary = Color(0xFF4FC3F7),
        onPrimary = Color(0xFF002435),
        primaryContainer = Color(0xFF0E4358),
        onPrimaryContainer = Color(0xFFC4EBFB),
        secondary = Color(0xFF6C8CB0),
        onSecondary = Color(0xFF0A1826),
        secondaryContainer = Color(0xFF283C52),
        onSecondaryContainer = Color(0xFFCEDCEC),
        tertiary = Color(0xFF9E88D0),
        onTertiary = Color(0xFF1B0F3A),
        tertiaryContainer = Color(0xFF352763),
        onTertiaryContainer = Color(0xFFE1D6F6),
        background = Color(0xFF05070D),
        onBackground = Color(0xFFDDE3ED),
        surface = Color(0xFF0A0D16),
        onSurface = Color(0xFFDDE3ED),
        surfaceVariant = Color(0xFF171C28),
        onSurfaceVariant = Color(0xFFAAB4C4),
        outline = Color(0xFF5A6577),
        outlineVariant = Color(0xFF2A313F),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.NEBULA, "Nebula", ThemeCategory.SPACE, nebulaScheme),
        ThemeSpec(AppTheme.MARS, "Mars", ThemeCategory.SPACE, marsScheme),
        ThemeSpec(AppTheme.GALAXY, "Galaxy", ThemeCategory.SPACE, galaxyScheme),
        ThemeSpec(AppTheme.DEEP_SPACE, "Deep Space", ThemeCategory.SPACE, deepSpaceScheme),
    )
}
