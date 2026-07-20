package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object RetroConsoleThemes {

    // ── Game Boy ──
    private val gameBoy = lightColorScheme(
        primary = Color(0xFF0F380F),
        onPrimary = Color(0xFF9BBC0F),
        primaryContainer = Color(0xFF306230),
        onPrimaryContainer = Color(0xFF9BBC0F),
        secondary = Color(0xFF306230),
        onSecondary = Color(0xFF9BBC0F),
        secondaryContainer = Color(0xFF8BAC0F),
        onSecondaryContainer = Color(0xFF0F380F),
        tertiary = Color(0xFF306230),
        onTertiary = Color(0xFF9BBC0F),
        tertiaryContainer = Color(0xFF306230),
        onTertiaryContainer = Color(0xFF9BBC0F),
        background = Color(0xFF9BBC0F),
        onBackground = Color(0xFF0F380F),
        surface = Color(0xFF8BAC0F),
        onSurface = Color(0xFF0F380F),
        surfaceVariant = Color(0xFF8BAC0F),
        onSurfaceVariant = Color(0xFF0F380F),
        outline = Color(0xFF306230),
        outlineVariant = Color(0xFF306230),
    )

    // ── Game Boy Color ──
    private val gameBoyColor = darkColorScheme(
        primary = Color(0xFFE040FB),
        onPrimary = Color(0xFF23023A),
        primaryContainer = Color(0xFF5C1B7A),
        onPrimaryContainer = Color(0xFFF7C6FF),
        secondary = Color(0xFF29E0E0),
        onSecondary = Color(0xFF00201F),
        secondaryContainer = Color(0xFF104F52),
        onSecondaryContainer = Color(0xFFAEF8FA),
        tertiary = Color(0xFFFFD740),
        onTertiary = Color(0xFF2E2400),
        tertiaryContainer = Color(0xFF6B5300),
        onTertiaryContainer = Color(0xFFFFEDA0),
        background = Color(0xFF1E0A2E),
        onBackground = Color(0xFFF2E4FA),
        surface = Color(0xFF29113D),
        onSurface = Color(0xFFF2E4FA),
        surfaceVariant = Color(0xFF3A1D50),
        onSurfaceVariant = Color(0xFFCBAEDC),
        outline = Color(0xFF8A5AA6),
        outlineVariant = Color(0xFF432259),
    )

    // ── Nintendo (NES) ──
    private val nes = darkColorScheme(
        primary = Color(0xFFE4000F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF5A0006),
        onPrimaryContainer = Color(0xFFFFB3B3),
        secondary = Color(0xFFB8B8B8),
        onSecondary = Color(0xFF1A1A1A),
        secondaryContainer = Color(0xFF3D3D3D),
        onSecondaryContainer = Color(0xFFE0E0E0),
        tertiary = Color(0xFF7C7C7C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF2B2B2B),
        onTertiaryContainer = Color(0xFFE0E0E0),
        background = Color(0xFF161616),
        onBackground = Color(0xFFE8E8E8),
        surface = Color(0xFF1F1F1F),
        onSurface = Color(0xFFE8E8E8),
        surfaceVariant = Color(0xFF2B2B2B),
        onSurfaceVariant = Color(0xFFB0B0B0),
        outline = Color(0xFF585858),
        outlineVariant = Color(0xFF333333),
    )

    // ── Super Nintendo ──
    private val snes = darkColorScheme(
        primary = Color(0xFF9B7EDE),
        onPrimary = Color(0xFF1B1526),
        primaryContainer = Color(0xFF4B3A73),
        onPrimaryContainer = Color(0xFFDCCEFF),
        secondary = Color(0xFFB8A5E8),
        onSecondary = Color(0xFF1B1526),
        secondaryContainer = Color(0xFF3E3160),
        onSecondaryContainer = Color(0xFFE3DAFF),
        tertiary = Color(0xFF7C6DB0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF3E3160),
        onTertiaryContainer = Color(0xFFE3DAFF),
        background = Color(0xFF16121F),
        onBackground = Color(0xFFE7E0F5),
        surface = Color(0xFF1E1830),
        onSurface = Color(0xFFE7E0F5),
        surfaceVariant = Color(0xFF2C2440),
        onSurfaceVariant = Color(0xFFB9AED4),
        outline = Color(0xFF6C5B9E),
        outlineVariant = Color(0xFF352B4D),
    )

    // ── Sega Genesis ──
    private val segaGenesis = darkColorScheme(
        primary = Color(0xFFE60012),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6E0009),
        onPrimaryContainer = Color(0xFFFFC2C4),
        secondary = Color(0xFF2E7DE0),
        onSecondary = Color(0xFF001B3D),
        secondaryContainer = Color(0xFF123A6B),
        onSecondaryContainer = Color(0xFFBFD9FF),
        tertiary = Color(0xFFB0B4BA),
        onTertiary = Color(0xFF1A1C1F),
        tertiaryContainer = Color(0xFF33373D),
        onTertiaryContainer = Color(0xFFDDE1E7),
        background = Color(0xFF0C0C0D),
        onBackground = Color(0xFFEDEEF0),
        surface = Color(0xFF151517),
        onSurface = Color(0xFFEDEEF0),
        surfaceVariant = Color(0xFF232427),
        onSurfaceVariant = Color(0xFFB6B8BC),
        outline = Color(0xFF565860),
        outlineVariant = Color(0xFF2C2D30),
    )

    // ── Virtual Boy ──
    private val virtualBoy = darkColorScheme(
        primary = Color(0xFFFF1A1A),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF700000),
        onPrimaryContainer = Color(0xFFFF9999),
        secondary = Color(0xFFCC0000),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF4D0000),
        onSecondaryContainer = Color(0xFFFF8080),
        tertiary = Color(0xFFFF6666),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF5C0000),
        onTertiaryContainer = Color(0xFFFFB3B3),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFF1A1A),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFF1A1A),
        surfaceVariant = Color(0xFF1A0000),
        onSurfaceVariant = Color(0xFFE60000),
        outline = Color(0xFF990000),
        outlineVariant = Color(0xFF330000),
    )

    // ── Atari 2600 ──
    private val atari2600 = darkColorScheme(
        primary = Color(0xFFE87722),
        onPrimary = Color(0xFF201000),
        primaryContainer = Color(0xFF6E3608),
        onPrimaryContainer = Color(0xFFFFD5B0),
        secondary = Color(0xFFD64545),
        onSecondary = Color(0xFF200404),
        secondaryContainer = Color(0xFF5C1414),
        onSecondaryContainer = Color(0xFFFFC7C7),
        tertiary = Color(0xFF2F9C9C),
        onTertiary = Color(0xFF002020),
        tertiaryContainer = Color(0xFF0E4C4C),
        onTertiaryContainer = Color(0xFFAEF0F0),
        background = Color(0xFF1E1409),
        onBackground = Color(0xFFF0E4D0),
        surface = Color(0xFF2A1D0F),
        onSurface = Color(0xFFF0E4D0),
        surfaceVariant = Color(0xFF3A2A18),
        onSurfaceVariant = Color(0xFFD1BFA3),
        outline = Color(0xFF8A6A44),
        outlineVariant = Color(0xFF44321E),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.GAME_BOY, "Game Boy", ThemeCategory.RETRO_CONSOLES, gameBoy),
        ThemeSpec(AppTheme.GAME_BOY_COLOR, "Game Boy Color", ThemeCategory.RETRO_CONSOLES, gameBoyColor),
        ThemeSpec(AppTheme.NES, "Nintendo (NES)", ThemeCategory.RETRO_CONSOLES, nes),
        ThemeSpec(AppTheme.SNES, "Super Nintendo", ThemeCategory.RETRO_CONSOLES, snes),
        ThemeSpec(AppTheme.SEGA_GENESIS, "Sega Genesis", ThemeCategory.RETRO_CONSOLES, segaGenesis),
        ThemeSpec(AppTheme.VIRTUAL_BOY, "Virtual Boy", ThemeCategory.RETRO_CONSOLES, virtualBoy),
        ThemeSpec(AppTheme.ATARI_2600, "Atari 2600", ThemeCategory.RETRO_CONSOLES, atari2600),
    )
}
