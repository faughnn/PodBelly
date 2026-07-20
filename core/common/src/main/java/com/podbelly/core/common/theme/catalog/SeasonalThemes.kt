package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object SeasonalThemes {

    // 1. Halloween — spooky night (dark)
    private val halloweenScheme = darkColorScheme(
        primary = Color(0xFFF07A1A),
        onPrimary = Color(0xFF231000),
        primaryContainer = Color(0xFF5C3208),
        onPrimaryContainer = Color(0xFFFFD9B0),
        secondary = Color(0xFF8A3FD1),
        onSecondary = Color(0xFF1C0733),
        secondaryContainer = Color(0xFF3A1961),
        onSecondaryContainer = Color(0xFFE7CDFF),
        tertiary = Color(0xFF5FD35A),
        onTertiary = Color(0xFF08260A),
        tertiaryContainer = Color(0xFF1D4A1F),
        onTertiaryContainer = Color(0xFFC6F5C2),
        background = Color(0xFF0D0A0F),
        onBackground = Color(0xFFEDE6F0),
        surface = Color(0xFF0D0A0F),
        onSurface = Color(0xFFEDE6F0),
        surfaceVariant = Color(0xFF241B2A),
        onSurfaceVariant = Color(0xFFC3B4CC),
        outline = Color(0xFF8C7C96),
        outlineVariant = Color(0xFF3D3245),
    )

    // 2. Christmas — festive evergreen (dark)
    private val christmasScheme = darkColorScheme(
        primary = Color(0xFFE23A3A),
        onPrimary = Color(0xFF2E0404),
        primaryContainer = Color(0xFF7A1212),
        onPrimaryContainer = Color(0xFFFFD5D2),
        secondary = Color(0xFF3FB05A),
        onSecondary = Color(0xFF042910),
        secondaryContainer = Color(0xFF1B5A2C),
        onSecondaryContainer = Color(0xFFC5F3CF),
        tertiary = Color(0xFFE8C34A),
        onTertiary = Color(0xFF2C2200),
        tertiaryContainer = Color(0xFF5E4A0E),
        onTertiaryContainer = Color(0xFFFBEBB2),
        background = Color(0xFF0E1A12),
        onBackground = Color(0xFFE6F1E8),
        surface = Color(0xFF0E1A12),
        onSurface = Color(0xFFE6F1E8),
        surfaceVariant = Color(0xFF1E2C22),
        onSurfaceVariant = Color(0xFFB6C7BB),
        outline = Color(0xFF7E9084),
        outlineVariant = Color(0xFF334037),
    )

    // 3. Autumn — fall foliage (light)
    private val autumnScheme = lightColorScheme(
        primary = Color(0xFFC96A2E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAD9BF),
        onPrimaryContainer = Color(0xFF3E1C06),
        secondary = Color(0xFFA83A1E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF7CFC2),
        onSecondaryContainer = Color(0xFF3B0F04),
        tertiary = Color(0xFFD99A2E),
        onTertiary = Color(0xFF3A2600),
        tertiaryContainer = Color(0xFFF8E4BB),
        onTertiaryContainer = Color(0xFF412C00),
        background = Color(0xFFF7EEDC),
        onBackground = Color(0xFF3B2A18),
        surface = Color(0xFFF7EEDC),
        onSurface = Color(0xFF3B2A18),
        surfaceVariant = Color(0xFFEADBC3),
        onSurfaceVariant = Color(0xFF6E5A42),
        outline = Color(0xFF9C8467),
        outlineVariant = Color(0xFFD8C4A6),
    )

    // 4. Winter Frost — icy calm (light)
    private val winterFrostScheme = lightColorScheme(
        primary = Color(0xFF3F8FD6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCDE4F8),
        onPrimaryContainer = Color(0xFF07263F),
        secondary = Color(0xFF6E8CA6),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDBE6F0),
        onSecondaryContainer = Color(0xFF17222E),
        tertiary = Color(0xFF3AA6A6),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCCECEC),
        onTertiaryContainer = Color(0xFF042A2A),
        background = Color(0xFFF0F6FB),
        onBackground = Color(0xFF1E2A33),
        surface = Color(0xFFF0F6FB),
        onSurface = Color(0xFF1E2A33),
        surfaceVariant = Color(0xFFDEE7EF),
        onSurfaceVariant = Color(0xFF4E5C68),
        outline = Color(0xFF7E8C99),
        outlineVariant = Color(0xFFC5D0DA),
    )

    // 5. Spring Bloom — fresh florals (light)
    private val springBloomScheme = lightColorScheme(
        primary = Color(0xFFE86AA0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAD3E4),
        onPrimaryContainer = Color(0xFF43102A),
        secondary = Color(0xFF6BBF5A),
        onSecondary = Color(0xFF08290A),
        secondaryContainer = Color(0xFFD5F0CD),
        onSecondaryContainer = Color(0xFF143808),
        tertiary = Color(0xFF4FA8E0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCEE9F8),
        onTertiaryContainer = Color(0xFF042C42),
        background = Color(0xFFF4FBF2),
        onBackground = Color(0xFF1F2A1E),
        surface = Color(0xFFF4FBF2),
        onSurface = Color(0xFF1F2A1E),
        surfaceVariant = Color(0xFFE3EEDF),
        onSurfaceVariant = Color(0xFF52604F),
        outline = Color(0xFF849181),
        outlineVariant = Color(0xFFCBD8C6),
    )

    // 6. Valentine — romantic (light)
    private val valentineScheme = lightColorScheme(
        primary = Color(0xFFE0325A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFACDD8),
        onPrimaryContainer = Color(0xFF43061A),
        secondary = Color(0xFFE889A8),
        onSecondary = Color(0xFF3E0A1D),
        secondaryContainer = Color(0xFFFBDCE7),
        onSecondaryContainer = Color(0xFF470E24),
        tertiary = Color(0xFFC98A6B),
        onTertiary = Color(0xFF3A1C0C),
        tertiaryContainer = Color(0xFFF6DECF),
        onTertiaryContainer = Color(0xFF3E2010),
        background = Color(0xFFFFF0F3),
        onBackground = Color(0xFF3A1424),
        surface = Color(0xFFFFF0F3),
        onSurface = Color(0xFF3A1424),
        surfaceVariant = Color(0xFFF3DBE1),
        onSurfaceVariant = Color(0xFF6E4E58),
        outline = Color(0xFFA57C86),
        outlineVariant = Color(0xFFE3C4CD),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.HALLOWEEN, "Halloween", ThemeCategory.SEASONAL, halloweenScheme),
        ThemeSpec(AppTheme.CHRISTMAS, "Christmas", ThemeCategory.SEASONAL, christmasScheme),
        ThemeSpec(AppTheme.AUTUMN, "Autumn", ThemeCategory.SEASONAL, autumnScheme),
        ThemeSpec(AppTheme.WINTER_FROST, "Winter Frost", ThemeCategory.SEASONAL, winterFrostScheme),
        ThemeSpec(AppTheme.SPRING_BLOOM, "Spring Bloom", ThemeCategory.SEASONAL, springBloomScheme),
        ThemeSpec(AppTheme.VALENTINE, "Valentine", ThemeCategory.SEASONAL, valentineScheme),
    )
}
