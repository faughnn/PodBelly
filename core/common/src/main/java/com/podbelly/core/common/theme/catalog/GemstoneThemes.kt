package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object GemstoneThemes {

    private val AmethystScheme = darkColorScheme(
        primary = Color(0xFFB06BE8),
        onPrimary = Color(0xFF250B3A),
        primaryContainer = Color(0xFF4A2A6E),
        onPrimaryContainer = Color(0xFFECD6FF),
        secondary = Color(0xFFB79BE0),
        onSecondary = Color(0xFF2A1745),
        secondaryContainer = Color(0xFF3E2B5C),
        onSecondaryContainer = Color(0xFFE7DAFB),
        tertiary = Color(0xFFD3AEF0),
        onTertiary = Color(0xFF321A4A),
        tertiaryContainer = Color(0xFF4C3268),
        onTertiaryContainer = Color(0xFFF1E0FC),
        background = Color(0xFF1A1226),
        onBackground = Color(0xFFEBE1F5),
        surface = Color(0xFF1A1226),
        onSurface = Color(0xFFEBE1F5),
        surfaceVariant = Color(0xFF2C2140),
        onSurfaceVariant = Color(0xFFC4B4D8),
        outline = Color(0xFF8A78A6),
        outlineVariant = Color(0xFF473859),
    )

    private val SapphireScheme = darkColorScheme(
        primary = Color(0xFF3A7BE8),
        onPrimary = Color(0xFF06132E),
        primaryContainer = Color(0xFF1E3F72),
        onPrimaryContainer = Color(0xFFD5E3FF),
        secondary = Color(0xFF74B0F0),
        onSecondary = Color(0xFF0A1B38),
        secondaryContainer = Color(0xFF203552),
        onSecondaryContainer = Color(0xFFDCEBFC),
        tertiary = Color(0xFF4FC5C9),
        onTertiary = Color(0xFF042724),
        tertiaryContainer = Color(0xFF1A4746),
        onTertiaryContainer = Color(0xFFCFF6F4),
        background = Color(0xFF0B1430),
        onBackground = Color(0xFFDEE6F5),
        surface = Color(0xFF0B1430),
        onSurface = Color(0xFFDEE6F5),
        surfaceVariant = Color(0xFF1A2544),
        onSurfaceVariant = Color(0xFFAFBFDB),
        outline = Color(0xFF6C82A8),
        outlineVariant = Color(0xFF33415E),
    )

    private val RubyScheme = darkColorScheme(
        primary = Color(0xFFE0294E),
        onPrimary = Color(0xFF33020C),
        primaryContainer = Color(0xFF72132A),
        onPrimaryContainer = Color(0xFFFFD9DF),
        secondary = Color(0xFFF07898),
        onSecondary = Color(0xFF3A0A1A),
        secondaryContainer = Color(0xFF5C1F30),
        onSecondaryContainer = Color(0xFFFFDCE4),
        tertiary = Color(0xFFF0A0B4),
        onTertiary = Color(0xFF400E1E),
        tertiaryContainer = Color(0xFF632A38),
        onTertiaryContainer = Color(0xFFFFE1E8),
        background = Color(0xFF1E0A0E),
        onBackground = Color(0xFFF5DEE2),
        surface = Color(0xFF1E0A0E),
        onSurface = Color(0xFFF5DEE2),
        surfaceVariant = Color(0xFF3A1A20),
        onSurfaceVariant = Color(0xFFD8AFB6),
        outline = Color(0xFFA6787F),
        outlineVariant = Color(0xFF593238),
    )

    private val JadeScheme = darkColorScheme(
        primary = Color(0xFF2EB88A),
        onPrimary = Color(0xFF03251A),
        primaryContainer = Color(0xFF135840),
        onPrimaryContainer = Color(0xFFCFF6E6),
        secondary = Color(0xFF74E0B4),
        onSecondary = Color(0xFF03301F),
        secondaryContainer = Color(0xFF1A4C39),
        onSecondaryContainer = Color(0xFFD6F7E9),
        tertiary = Color(0xFFE6CF7A),
        onTertiary = Color(0xFF352A05),
        tertiaryContainer = Color(0xFF52451A),
        onTertiaryContainer = Color(0xFFFAEFC4),
        background = Color(0xFF0C1A14),
        onBackground = Color(0xFFDEEEE6),
        surface = Color(0xFF0C1A14),
        onSurface = Color(0xFFDEEEE6),
        surfaceVariant = Color(0xFF1A2E26),
        onSurfaceVariant = Color(0xFFAECABE),
        outline = Color(0xFF6E8C80),
        outlineVariant = Color(0xFF334740),
    )

    private val OpalScheme = lightColorScheme(
        primary = Color(0xFF2FA8B0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBFEEF1),
        onPrimaryContainer = Color(0xFF043336),
        secondary = Color(0xFFE07AA8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFAD5E5),
        onSecondaryContainer = Color(0xFF44112A),
        tertiary = Color(0xFF8C7BE0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE1DBFA),
        onTertiaryContainer = Color(0xFF231A52),
        background = Color(0xFFF3F6F8),
        onBackground = Color(0xFF1A2124),
        surface = Color(0xFFF3F6F8),
        onSurface = Color(0xFF1A2124),
        surfaceVariant = Color(0xFFDDE7EA),
        onSurfaceVariant = Color(0xFF465155),
        outline = Color(0xFF77868A),
        outlineVariant = Color(0xFFC5D2D6),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.AMETHYST, "Amethyst", ThemeCategory.GEMSTONES, AmethystScheme),
        ThemeSpec(AppTheme.SAPPHIRE, "Sapphire", ThemeCategory.GEMSTONES, SapphireScheme),
        ThemeSpec(AppTheme.RUBY, "Ruby", ThemeCategory.GEMSTONES, RubyScheme),
        ThemeSpec(AppTheme.JADE, "Jade", ThemeCategory.GEMSTONES, JadeScheme),
        ThemeSpec(AppTheme.OPAL, "Opal", ThemeCategory.GEMSTONES, OpalScheme),
    )
}
