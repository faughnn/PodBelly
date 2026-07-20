package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object ArcadeThemes {
    // ── Arcade ──
    private val arcade = darkColorScheme(
        primary = Color(0xFFFFF000),
        onPrimary = Color(0xFF14140A),
        primaryContainer = Color(0xFF3D3A00),
        onPrimaryContainer = Color(0xFFFFF69C),
        secondary = Color(0xFF3B5BFF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF16209E),
        onSecondaryContainer = Color(0xFFC2CCFF),
        tertiary = Color(0xFFFF9CCE),
        onTertiary = Color(0xFF3D0022),
        tertiaryContainer = Color(0xFF6B0040),
        onTertiaryContainer = Color(0xFFFFD1E8),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF5F5F5),
        surface = Color(0xFF0A0A18),
        onSurface = Color(0xFFF5F5F5),
        surfaceVariant = Color(0xFF14142B),
        onSurfaceVariant = Color(0xFFB0B0C8),
        outline = Color(0xFF3B5BFF),
        outlineVariant = Color(0xFF14142B),
    )

    // ── Space Invaders ──
    private val spaceInvaders = darkColorScheme(
        primary = Color(0xFF32FF32),
        onPrimary = Color(0xFF002200),
        primaryContainer = Color(0xFF0C3D0C),
        onPrimaryContainer = Color(0xFFAEFFAE),
        secondary = Color(0xFF29E0E0),
        onSecondary = Color(0xFF002424),
        secondaryContainer = Color(0xFF0A4747),
        onSecondaryContainer = Color(0xFFB0FBFB),
        tertiary = Color(0xFFFF2D2D),
        onTertiary = Color(0xFF2E0000),
        tertiaryContainer = Color(0xFF5E0505),
        onTertiaryContainer = Color(0xFFFFC7C7),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE8FFE8),
        surface = Color(0xFF050A05),
        onSurface = Color(0xFFE8FFE8),
        surfaceVariant = Color(0xFF0E1A0E),
        onSurfaceVariant = Color(0xFF9CC49C),
        outline = Color(0xFF32FF32),
        outlineVariant = Color(0xFF143314),
    )

    // ── Tetris ──
    private val tetris = darkColorScheme(
        primary = Color(0xFF00B0F0),
        onPrimary = Color(0xFF001A26),
        primaryContainer = Color(0xFF033A50),
        onPrimaryContainer = Color(0xFFB4E6FA),
        secondary = Color(0xFFF0C000),
        onSecondary = Color(0xFF261E00),
        secondaryContainer = Color(0xFF4D3F00),
        onSecondaryContainer = Color(0xFFFBEBA0),
        tertiary = Color(0xFFA000F0),
        onTertiary = Color(0xFF22002E),
        tertiaryContainer = Color(0xFF48046B),
        onTertiaryContainer = Color(0xFFE9C2FB),
        background = Color(0xFF05060F),
        onBackground = Color(0xFFF0F2FA),
        surface = Color(0xFF0A0C1A),
        onSurface = Color(0xFFF0F2FA),
        surfaceVariant = Color(0xFF14172E),
        onSurfaceVariant = Color(0xFFAAB0CC),
        outline = Color(0xFF00B0F0),
        outlineVariant = Color(0xFF14172E),
    )

    // ── Donkey Kong ──
    private val donkeyKong = darkColorScheme(
        primary = Color(0xFFE81E1E),
        onPrimary = Color(0xFF2E0000),
        primaryContainer = Color(0xFF5C0808),
        onPrimaryContainer = Color(0xFFFFC5C5),
        secondary = Color(0xFFD9A066),
        onSecondary = Color(0xFF2E1B08),
        secondaryContainer = Color(0xFF4D3418),
        onSecondaryContainer = Color(0xFFF6DEBF),
        tertiary = Color(0xFFF8D000),
        onTertiary = Color(0xFF2A2300),
        tertiaryContainer = Color(0xFF524500),
        onTertiaryContainer = Color(0xFFFBEEA0),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF7EDE4),
        surface = Color(0xFF0F0A08),
        onSurface = Color(0xFFF7EDE4),
        surfaceVariant = Color(0xFF241813),
        onSurfaceVariant = Color(0xFFC7AC9A),
        outline = Color(0xFFD9A066),
        outlineVariant = Color(0xFF241813),
    )

    // ── Neon Cabinet ──
    private val neonCabinet = darkColorScheme(
        primary = Color(0xFFFF10F0),
        onPrimary = Color(0xFF2E002B),
        primaryContainer = Color(0xFF5C0056),
        onPrimaryContainer = Color(0xFFFFC0F9),
        secondary = Color(0xFF10F0FF),
        onSecondary = Color(0xFF002A2E),
        secondaryContainer = Color(0xFF00565C),
        onSecondaryContainer = Color(0xFFB6F8FF),
        tertiary = Color(0xFFEFFF12),
        onTertiary = Color(0xFF2A2E00),
        tertiaryContainer = Color(0xFF545C00),
        onTertiaryContainer = Color(0xFFEFFBB0),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF6F0FA),
        surface = Color(0xFF0B0710),
        onSurface = Color(0xFFF6F0FA),
        surfaceVariant = Color(0xFF1C1226),
        onSurfaceVariant = Color(0xFFC0AAD0),
        outline = Color(0xFFFF10F0),
        outlineVariant = Color(0xFF1C1226),
    )

    // ── Frogger ──
    private val frogger = darkColorScheme(
        primary = Color(0xFF00C000),
        onPrimary = Color(0xFF002200),
        primaryContainer = Color(0xFF0A3D0A),
        onPrimaryContainer = Color(0xFFAEF0AE),
        secondary = Color(0xFF3BB0D9),
        onSecondary = Color(0xFF002733),
        secondaryContainer = Color(0xFF08475C),
        onSecondaryContainer = Color(0xFFBEE8F6),
        tertiary = Color(0xFFE85AAD),
        onTertiary = Color(0xFF3D0022),
        tertiaryContainer = Color(0xFF66103F),
        onTertiaryContainer = Color(0xFFFAC7E4),
        background = Color(0xFF0A1A0A),
        onBackground = Color(0xFFE8F5E8),
        surface = Color(0xFF0D200D),
        onSurface = Color(0xFFE8F5E8),
        surfaceVariant = Color(0xFF163016),
        onSurfaceVariant = Color(0xFFA0C4A0),
        outline = Color(0xFF3BB0D9),
        outlineVariant = Color(0xFF163016),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.ARCADE, "Arcade", ThemeCategory.ARCADE, arcade),
        ThemeSpec(AppTheme.SPACE_INVADERS, "Space Invaders", ThemeCategory.ARCADE, spaceInvaders),
        ThemeSpec(AppTheme.TETRIS, "Tetris", ThemeCategory.ARCADE, tetris),
        ThemeSpec(AppTheme.DONKEY_KONG, "Donkey Kong", ThemeCategory.ARCADE, donkeyKong),
        ThemeSpec(AppTheme.NEON_CABINET, "Neon Cabinet", ThemeCategory.ARCADE, neonCabinet),
        ThemeSpec(AppTheme.FROGGER, "Frogger", ThemeCategory.ARCADE, frogger),
    )
}
