package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object NatureThemes {

    private val forest = darkColorScheme(
        primary = Color(0xFF7CC96A),
        onPrimary = Color(0xFF0C2008),
        primaryContainer = Color(0xFF2E4A26),
        onPrimaryContainer = Color(0xFFC8E7BC),
        secondary = Color(0xFF9CCB6A),
        onSecondary = Color(0xFF1B2A0B),
        secondaryContainer = Color(0xFF3A4C25),
        onSecondaryContainer = Color(0xFFD8ECBE),
        tertiary = Color(0xFFB08D57),
        onTertiary = Color(0xFF2A1E0C),
        tertiaryContainer = Color(0xFF4A3A22),
        onTertiaryContainer = Color(0xFFEAD6B4),
        background = Color(0xFF0E1A10),
        onBackground = Color(0xFFDDEAD4),
        surface = Color(0xFF16261A),
        onSurface = Color(0xFFDDEAD4),
        surfaceVariant = Color(0xFF213426),
        onSurfaceVariant = Color(0xFFAFC2A4),
        outline = Color(0xFF788A6E),
        outlineVariant = Color(0xFF3B4C36),
    )

    private val deepOcean = darkColorScheme(
        primary = Color(0xFF35C4D8),
        onPrimary = Color(0xFF032128),
        primaryContainer = Color(0xFF13454F),
        onPrimaryContainer = Color(0xFFB6E9F2),
        secondary = Color(0xFF2E8B9E),
        onSecondary = Color(0xFF03222A),
        secondaryContainer = Color(0xFF184A55),
        onSecondaryContainer = Color(0xFFB2E3EC),
        tertiary = Color(0xFFFF7E67),
        onTertiary = Color(0xFF3A0F07),
        tertiaryContainer = Color(0xFF5E2419),
        onTertiaryContainer = Color(0xFFFFD6CC),
        background = Color(0xFF03141F),
        onBackground = Color(0xFFCDE8EF),
        surface = Color(0xFF07202E),
        onSurface = Color(0xFFCDE8EF),
        surfaceVariant = Color(0xFF123141),
        onSurfaceVariant = Color(0xFF9CC0CD),
        outline = Color(0xFF5E8593),
        outlineVariant = Color(0xFF2A4855),
    )

    private val sunset = darkColorScheme(
        primary = Color(0xFFFF7B54),
        onPrimary = Color(0xFF3A1105),
        primaryContainer = Color(0xFF5E2617),
        onPrimaryContainer = Color(0xFFFFD5C6),
        secondary = Color(0xFFFF5F8D),
        onSecondary = Color(0xFF3A0819),
        secondaryContainer = Color(0xFF5E1E33),
        onSecondaryContainer = Color(0xFFFFCEDC),
        tertiary = Color(0xFFFFC15E),
        onTertiary = Color(0xFF3A2807),
        tertiaryContainer = Color(0xFF5E4419),
        onTertiaryContainer = Color(0xFFFFE6C1),
        background = Color(0xFF2A1220),
        onBackground = Color(0xFFFFE8DC),
        surface = Color(0xFF3A1A2C),
        onSurface = Color(0xFFFFE8DC),
        surfaceVariant = Color(0xFF4C2A3B),
        onSurfaceVariant = Color(0xFFD9B4C2),
        outline = Color(0xFFA07C8A),
        outlineVariant = Color(0xFF5C3A4A),
    )

    private val aurora = darkColorScheme(
        primary = Color(0xFF4DE6A8),
        onPrimary = Color(0xFF033522),
        primaryContainer = Color(0xFF11553A),
        onPrimaryContainer = Color(0xFFB9F5DA),
        secondary = Color(0xFF48C6EF),
        onSecondary = Color(0xFF032F3E),
        secondaryContainer = Color(0xFF11485C),
        onSecondaryContainer = Color(0xFFBDEAF9),
        tertiary = Color(0xFFB48EF0),
        onTertiary = Color(0xFF230D42),
        tertiaryContainer = Color(0xFF3B2560),
        onTertiaryContainer = Color(0xFFE4D5FB),
        background = Color(0xFF07101F),
        onBackground = Color(0xFFDCE6F5),
        surface = Color(0xFF0D1A2E),
        onSurface = Color(0xFFDCE6F5),
        surfaceVariant = Color(0xFF1A2840),
        onSurfaceVariant = Color(0xFFAEBED6),
        outline = Color(0xFF6C7F9C),
        outlineVariant = Color(0xFF32425C),
    )

    private val cherryBlossom = lightColorScheme(
        primary = Color(0xFFE8709E),
        onPrimary = Color(0xFFFFFBFC),
        primaryContainer = Color(0xFFFAD0DF),
        onPrimaryContainer = Color(0xFF5C1E36),
        secondary = Color(0xFFD98CB0),
        onSecondary = Color(0xFFFFFBFC),
        secondaryContainer = Color(0xFFF6D8E5),
        onSecondaryContainer = Color(0xFF55283C),
        tertiary = Color(0xFF7FA86B),
        onTertiary = Color(0xFFFCFEFA),
        tertiaryContainer = Color(0xFFDDEBD1),
        onTertiaryContainer = Color(0xFF2E4222),
        background = Color(0xFFFFF5F8),
        onBackground = Color(0xFF4A2B38),
        surface = Color(0xFFFDEAF0),
        onSurface = Color(0xFF4A2B38),
        surfaceVariant = Color(0xFFF3DBE3),
        onSurfaceVariant = Color(0xFF7A5765),
        outline = Color(0xFFB08D99),
        outlineVariant = Color(0xFFE5C7D1),
    )

    private val desert = lightColorScheme(
        primary = Color(0xFFC96F3B),
        onPrimary = Color(0xFFFFF8F3),
        primaryContainer = Color(0xFFF2C9AC),
        onPrimaryContainer = Color(0xFF4A2411),
        secondary = Color(0xFF7A8B4F),
        onSecondary = Color(0xFFFAFBF4),
        secondaryContainer = Color(0xFFDCE3C1),
        onSecondaryContainer = Color(0xFF2E3618),
        tertiary = Color(0xFF9B6A8C),
        onTertiary = Color(0xFFFDF9FB),
        tertiaryContainer = Color(0xFFE7CEDF),
        onTertiaryContainer = Color(0xFF3C2434),
        background = Color(0xFFF6E7C9),
        onBackground = Color(0xFF3E2C1A),
        surface = Color(0xFFEFDBB0),
        onSurface = Color(0xFF3E2C1A),
        surfaceVariant = Color(0xFFE4CD9E),
        onSurfaceVariant = Color(0xFF6E5A3C),
        outline = Color(0xFFA68B5E),
        outlineVariant = Color(0xFFD3BC8C),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.FOREST, "Forest", ThemeCategory.NATURE, forest),
        ThemeSpec(AppTheme.DEEP_OCEAN, "Deep Ocean", ThemeCategory.NATURE, deepOcean),
        ThemeSpec(AppTheme.SUNSET, "Sunset", ThemeCategory.NATURE, sunset),
        ThemeSpec(AppTheme.AURORA, "Aurora", ThemeCategory.NATURE, aurora),
        ThemeSpec(AppTheme.CHERRY_BLOSSOM, "Cherry Blossom", ThemeCategory.NATURE, cherryBlossom),
        ThemeSpec(AppTheme.DESERT, "Desert", ThemeCategory.NATURE, desert),
    )
}
