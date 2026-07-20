package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object CinemaThemes {

    private val lightsaberScheme = darkColorScheme(
        primary = Color(0xFF2E9BFF),
        onPrimary = Color(0xFF001B33),
        primaryContainer = Color(0xFF13456E),
        onPrimaryContainer = Color(0xFFCDE5FF),
        secondary = Color(0xFF39D353),
        onSecondary = Color(0xFF00230A),
        secondaryContainer = Color(0xFF10521F),
        onSecondaryContainer = Color(0xFFB6F5C1),
        tertiary = Color(0xFFF03A3A),
        onTertiary = Color(0xFF330404),
        tertiaryContainer = Color(0xFF611514),
        onTertiaryContainer = Color(0xFFFFD5D2),
        background = Color(0xFF05070B),
        onBackground = Color(0xFFE6EAF0),
        surface = Color(0xFF0A0D13),
        onSurface = Color(0xFFE6EAF0),
        surfaceVariant = Color(0xFF161A22),
        onSurfaceVariant = Color(0xFFB4BAC6),
        outline = Color(0xFF5A6270),
        outlineVariant = Color(0xFF2A2F39),
    )

    private val xenomorphScheme = darkColorScheme(
        primary = Color(0xFF6FBF3F),
        onPrimary = Color(0xFF0D1F03),
        primaryContainer = Color(0xFF2E4A16),
        onPrimaryContainer = Color(0xFFCDEBAB),
        secondary = Color(0xFF7E8A8F),
        onSecondary = Color(0xFF14191B),
        secondaryContainer = Color(0xFF37403F),
        onSecondaryContainer = Color(0xFFD4DBDE),
        tertiary = Color(0xFFD8D24A),
        onTertiary = Color(0xFF272601),
        tertiaryContainer = Color(0xFF4F4C13),
        onTertiaryContainer = Color(0xFFF3EFB6),
        background = Color(0xFF0B0F0B),
        onBackground = Color(0xFFDDE4DA),
        surface = Color(0xFF10150F),
        onSurface = Color(0xFFDDE4DA),
        surfaceVariant = Color(0xFF1C221A),
        onSurfaceVariant = Color(0xFFB0B8AC),
        outline = Color(0xFF565E52),
        outlineVariant = Color(0xFF2C332A),
    )

    private val gridRiderScheme = darkColorScheme(
        primary = Color(0xFF35E0F0),
        onPrimary = Color(0xFF002429),
        primaryContainer = Color(0xFF08525C),
        onPrimaryContainer = Color(0xFFB4F4FC),
        secondary = Color(0xFFFF7A18),
        onSecondary = Color(0xFF2E1400),
        secondaryContainer = Color(0xFF6E3505),
        onSecondaryContainer = Color(0xFFFFD9BC),
        tertiary = Color(0xFFCFE8FF),
        onTertiary = Color(0xFF0A1F30),
        tertiaryContainer = Color(0xFF274050),
        onTertiaryContainer = Color(0xFFDDF0FF),
        background = Color(0xFF03060A),
        onBackground = Color(0xFFDDECF2),
        surface = Color(0xFF080C12),
        onSurface = Color(0xFFDDECF2),
        surfaceVariant = Color(0xFF12181F),
        onSurfaceVariant = Color(0xFFA8B6C0),
        outline = Color(0xFF4C5A64),
        outlineVariant = Color(0xFF232B32),
    )

    private val redEyeScheme = darkColorScheme(
        primary = Color(0xFFFF2A1A),
        onPrimary = Color(0xFF2E0300),
        primaryContainer = Color(0xFF6A0F08),
        onPrimaryContainer = Color(0xFFFFD3CD),
        secondary = Color(0xFF9AA0A6),
        onSecondary = Color(0xFF16181A),
        secondaryContainer = Color(0xFF33373B),
        onSecondaryContainer = Color(0xFFDCE0E4),
        tertiary = Color(0xFF7C848C),
        onTertiary = Color(0xFF121518),
        tertiaryContainer = Color(0xFF2C3136),
        onTertiaryContainer = Color(0xFFD2D7DC),
        background = Color(0xFF060506),
        onBackground = Color(0xFFDBDDDF),
        surface = Color(0xFF0A090A),
        onSurface = Color(0xFFDBDDDF),
        surfaceVariant = Color(0xFF161618),
        onSurfaceVariant = Color(0xFFAFB2B6),
        outline = Color(0xFF54575B),
        outlineVariant = Color(0xFF282A2C),
    )

    private val spicePlanetScheme = darkColorScheme(
        primary = Color(0xFFE8892B),
        onPrimary = Color(0xFF2E1600),
        primaryContainer = Color(0xFF743F0C),
        onPrimaryContainer = Color(0xFFFFDCBC),
        secondary = Color(0xFF3AA0D9),
        onSecondary = Color(0xFF002234),
        secondaryContainer = Color(0xFF0F4C6C),
        onSecondaryContainer = Color(0xFFC3E7FA),
        tertiary = Color(0xFFD9C3A0),
        onTertiary = Color(0xFF352815),
        tertiaryContainer = Color(0xFF5A4A31),
        onTertiaryContainer = Color(0xFFF3E7D3),
        background = Color(0xFF2A1E10),
        onBackground = Color(0xFFF2E6D6),
        surface = Color(0xFF312414),
        onSurface = Color(0xFFF2E6D6),
        surfaceVariant = Color(0xFF40311E),
        onSurfaceVariant = Color(0xFFCDBBA2),
        outline = Color(0xFF8A755A),
        outlineVariant = Color(0xFF4B3B27),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.LIGHTSABER, "Lightsaber", ThemeCategory.CINEMA, lightsaberScheme),
        ThemeSpec(AppTheme.XENOMORPH, "Xenomorph", ThemeCategory.CINEMA, xenomorphScheme),
        ThemeSpec(AppTheme.GRID_RIDER, "Grid Rider", ThemeCategory.CINEMA, gridRiderScheme),
        ThemeSpec(AppTheme.RED_EYE, "Red Eye", ThemeCategory.CINEMA, redEyeScheme),
        ThemeSpec(AppTheme.SPICE_PLANET, "Spice Planet", ThemeCategory.CINEMA, spicePlanetScheme),
    )
}
