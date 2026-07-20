package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object Y2kThemes {

    private val AquaGelScheme = lightColorScheme(
        primary = Color(0xFF12B5D6),
        onPrimary = Color(0xFF00272E),
        primaryContainer = Color(0xFFB6EEF9),
        onPrimaryContainer = Color(0xFF00363F),
        secondary = Color(0xFF7BD634),
        onSecondary = Color(0xFF12290A),
        secondaryContainer = Color(0xFFDCF5BF),
        onSecondaryContainer = Color(0xFF203612),
        tertiary = Color(0xFF4AA8F0),
        onTertiary = Color(0xFF002338),
        tertiaryContainer = Color(0xFFC9E6FC),
        onTertiaryContainer = Color(0xFF0A314C),
        background = Color(0xFFF0FBFF),
        onBackground = Color(0xFF0D2A31),
        surface = Color(0xFFF0FBFF),
        onSurface = Color(0xFF0D2A31),
        surfaceVariant = Color(0xFFDCEEF3),
        onSurfaceVariant = Color(0xFF41626B),
        outline = Color(0xFF6F9199),
        outlineVariant = Color(0xFFBFDCE3),
    )

    private val LimeGlossScheme = lightColorScheme(
        primary = Color(0xFF6FBF12),
        onPrimary = Color(0xFF152A00),
        primaryContainer = Color(0xFFD4F0AE),
        onPrimaryContainer = Color(0xFF243B00),
        secondary = Color(0xFF19C6C6),
        onSecondary = Color(0xFF002B2B),
        secondaryContainer = Color(0xFFB8F0EF),
        onSecondaryContainer = Color(0xFF003838),
        tertiary = Color(0xFF9BC22E),
        onTertiary = Color(0xFF1E2A00),
        tertiaryContainer = Color(0xFFE2F2B6),
        onTertiaryContainer = Color(0xFF303F00),
        background = Color(0xFFF4FBE8),
        onBackground = Color(0xFF232B16),
        surface = Color(0xFFF4FBE8),
        onSurface = Color(0xFF232B16),
        surfaceVariant = Color(0xFFE4EED4),
        onSurfaceVariant = Color(0xFF56603F),
        outline = Color(0xFF838E6D),
        outlineVariant = Color(0xFFCCD8B6),
    )

    private val ChromeScheme = lightColorScheme(
        primary = Color(0xFF5A7A9E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E1F1),
        onPrimaryContainer = Color(0xFF16324B),
        secondary = Color(0xFF8A99A6),
        onSecondary = Color(0xFF1C2932),
        secondaryContainer = Color(0xFFDDE5EC),
        onSecondaryContainer = Color(0xFF2C3A44),
        tertiary = Color(0xFF3F98A8),
        onTertiary = Color(0xFF00252C),
        tertiaryContainer = Color(0xFFC4E7ED),
        onTertiaryContainer = Color(0xFF0A353E),
        background = Color(0xFFEDF1F5),
        onBackground = Color(0xFF1D242B),
        surface = Color(0xFFEDF1F5),
        onSurface = Color(0xFF1D242B),
        surfaceVariant = Color(0xFFDCE2E8),
        onSurfaceVariant = Color(0xFF4A535C),
        outline = Color(0xFF79828B),
        outlineVariant = Color(0xFFC5CCD3),
    )

    private val BubbleBlueScheme = lightColorScheme(
        primary = Color(0xFF2E9BF0),
        onPrimary = Color(0xFF002138),
        primaryContainer = Color(0xFFC5E4FC),
        onPrimaryContainer = Color(0xFF063050),
        secondary = Color(0xFF19C6E0),
        onSecondary = Color(0xFF002A32),
        secondaryContainer = Color(0xFFB6EEF7),
        onSecondaryContainer = Color(0xFF003842),
        tertiary = Color(0xFF8A7BE0),
        onTertiary = Color(0xFF16093F),
        tertiaryContainer = Color(0xFFE0DAFA),
        onTertiaryContainer = Color(0xFF241663),
        background = Color(0xFFF2F8FF),
        onBackground = Color(0xFF15283A),
        surface = Color(0xFFF2F8FF),
        onSurface = Color(0xFF15283A),
        surfaceVariant = Color(0xFFDCE8F5),
        onSurfaceVariant = Color(0xFF425466),
        outline = Color(0xFF718499),
        outlineVariant = Color(0xFFC1D3E4),
    )

    private val FrostGlassScheme = lightColorScheme(
        primary = Color(0xFF5B8DEF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8E4FD),
        onPrimaryContainer = Color(0xFF11305C),
        secondary = Color(0xFF4FB0C6),
        onSecondary = Color(0xFF002A33),
        secondaryContainer = Color(0xFFC9EAF1),
        onSecondaryContainer = Color(0xFF0A3742),
        tertiary = Color(0xFF9A7FD1),
        onTertiary = Color(0xFF23103F),
        tertiaryContainer = Color(0xFFE7DCF7),
        onTertiaryContainer = Color(0xFF361E5E),
        background = Color(0xFFF4F7FC),
        onBackground = Color(0xFF1B2430),
        surface = Color(0xFFF4F7FC),
        onSurface = Color(0xFF1B2430),
        surfaceVariant = Color(0xFFE1E7F0),
        onSurfaceVariant = Color(0xFF484F5B),
        outline = Color(0xFF7A828F),
        outlineVariant = Color(0xFFC7CEDA),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.AQUA_GEL, "Aqua Gel", ThemeCategory.Y2K, AquaGelScheme),
        ThemeSpec(AppTheme.LIME_GLOSS, "Lime Gloss", ThemeCategory.Y2K, LimeGlossScheme),
        ThemeSpec(AppTheme.CHROME, "Chrome", ThemeCategory.Y2K, ChromeScheme),
        ThemeSpec(AppTheme.BUBBLE_BLUE, "Bubble Blue", ThemeCategory.Y2K, BubbleBlueScheme),
        ThemeSpec(AppTheme.FROST_GLASS, "Frost Glass", ThemeCategory.Y2K, FrostGlassScheme),
    )
}
