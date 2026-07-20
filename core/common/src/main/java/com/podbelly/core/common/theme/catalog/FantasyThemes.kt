package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object FantasyThemes {

    private val DungeonScheme = darkColorScheme(
        primary = Color(0xFFE0822E),
        onPrimary = Color(0xFF3A1D00),
        primaryContainer = Color(0xFF5C3308),
        onPrimaryContainer = Color(0xFFFFD9B0),
        secondary = Color(0xFF6B8A3F),
        onSecondary = Color(0xFF14210A),
        secondaryContainer = Color(0xFF2E3E18),
        onSecondaryContainer = Color(0xFFD3E5B0),
        tertiary = Color(0xFF9AA0A6),
        onTertiary = Color(0xFF1C1E20),
        tertiaryContainer = Color(0xFF3A3D40),
        onTertiaryContainer = Color(0xFFDDE0E3),
        background = Color(0xFF17150F),
        onBackground = Color(0xFFEAE4D6),
        surface = Color(0xFF1F1C14),
        onSurface = Color(0xFFEAE4D6),
        surfaceVariant = Color(0xFF2B271D),
        onSurfaceVariant = Color(0xFFC2B79E),
        outline = Color(0xFF7A6E58),
        outlineVariant = Color(0xFF423B2C)
    )

    private val PotionScheme = darkColorScheme(
        primary = Color(0xFFA24EE0),
        onPrimary = Color(0xFF250840),
        primaryContainer = Color(0xFF451C6B),
        onPrimaryContainer = Color(0xFFE8CCFA),
        secondary = Color(0xFF5FD35A),
        onSecondary = Color(0xFF082607),
        secondaryContainer = Color(0xFF1C4718),
        onSecondaryContainer = Color(0xFFBFF0BB),
        tertiary = Color(0xFFE86AC0),
        onTertiary = Color(0xFF3A0A2C),
        tertiaryContainer = Color(0xFF611A4C),
        onTertiaryContainer = Color(0xFFFFCFEE),
        background = Color(0xFF160E22),
        onBackground = Color(0xFFE9DFF4),
        surface = Color(0xFF1F162E),
        onSurface = Color(0xFFE9DFF4),
        surfaceVariant = Color(0xFF2C2240),
        onSurfaceVariant = Color(0xFFBCACD0),
        outline = Color(0xFF806E9A),
        outlineVariant = Color(0xFF3E3355)
    )

    private val ManaBlueScheme = darkColorScheme(
        primary = Color(0xFF3A9BE0),
        onPrimary = Color(0xFF03233A),
        primaryContainer = Color(0xFF0E3E63),
        onPrimaryContainer = Color(0xFFBFE0FA),
        secondary = Color(0xFF9B6BFF),
        onSecondary = Color(0xFF1E0940),
        secondaryContainer = Color(0xFF391C6B),
        onSecondaryContainer = Color(0xFFDCCBFF),
        tertiary = Color(0xFFC0CAE0),
        onTertiary = Color(0xFF1A2130),
        tertiaryContainer = Color(0xFF333C4E),
        onTertiaryContainer = Color(0xFFDEE4F2),
        background = Color(0xFF0A1836),
        onBackground = Color(0xFFDDE6F6),
        surface = Color(0xFF102043),
        onSurface = Color(0xFFDDE6F6),
        surfaceVariant = Color(0xFF1B2C52),
        onSurfaceVariant = Color(0xFFAABAD6),
        outline = Color(0xFF64789E),
        outlineVariant = Color(0xFF2C3E63)
    )

    private val NecromancerScheme = darkColorScheme(
        primary = Color(0xFF6FD36A),
        onPrimary = Color(0xFF07260A),
        primaryContainer = Color(0xFF184718),
        onPrimaryContainer = Color(0xFFC5F0BF),
        secondary = Color(0xFFDDD8C8),
        onSecondary = Color(0xFF26241A),
        secondaryContainer = Color(0xFF3C3A2E),
        onSecondaryContainer = Color(0xFFF0ECDF),
        tertiary = Color(0xFF7A3FB0),
        onTertiary = Color(0xFF1F0838),
        tertiaryContainer = Color(0xFF3A1A5C),
        onTertiaryContainer = Color(0xFFE0C9F5),
        background = Color(0xFF0C0E0C),
        onBackground = Color(0xFFDDE2DA),
        surface = Color(0xFF141613),
        onSurface = Color(0xFFDDE2DA),
        surfaceVariant = Color(0xFF20241E),
        onSurfaceVariant = Color(0xFFAAB4A4),
        outline = Color(0xFF6C7668),
        outlineVariant = Color(0xFF333A30)
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.DUNGEON, "Dungeon", ThemeCategory.FANTASY, DungeonScheme),
        ThemeSpec(AppTheme.POTION, "Potion", ThemeCategory.FANTASY, PotionScheme),
        ThemeSpec(AppTheme.MANA_BLUE, "Mana Blue", ThemeCategory.FANTASY, ManaBlueScheme),
        ThemeSpec(AppTheme.NECROMANCER, "Necromancer", ThemeCategory.FANTASY, NecromancerScheme)
    )
}
