package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object PastelThemes {

    private val cottonCandyScheme = lightColorScheme(
        primary = Color(0xFFE86AA6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD6E8),
        onPrimaryContainer = Color(0xFF5C1338),
        secondary = Color(0xFF6AB8E8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD3ECFB),
        onSecondaryContainer = Color(0xFF0F3B52),
        tertiary = Color(0xFFB98CE0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEBDCFA),
        onTertiaryContainer = Color(0xFF3D2159),
        background = Color(0xFFFFF4FA),
        onBackground = Color(0xFF3A2530),
        surface = Color(0xFFFFF4FA),
        onSurface = Color(0xFF3A2530),
        surfaceVariant = Color(0xFFF6E1EC),
        onSurfaceVariant = Color(0xFF6E5560),
        outline = Color(0xFFA98795),
        outlineVariant = Color(0xFFEACAD9),
    )

    private val catppuccinScheme = darkColorScheme(
        primary = Color(0xFFCBA6F7),
        onPrimary = Color(0xFF1E1E2E),
        primaryContainer = Color(0xFF4B3A6B),
        onPrimaryContainer = Color(0xFFEBDDFB),
        secondary = Color(0xFFF5C2E7),
        onSecondary = Color(0xFF1E1E2E),
        secondaryContainer = Color(0xFF5C3E52),
        onSecondaryContainer = Color(0xFFFBE0F3),
        tertiary = Color(0xFF94E2D5),
        onTertiary = Color(0xFF1E1E2E),
        tertiaryContainer = Color(0xFF2F5A52),
        onTertiaryContainer = Color(0xFFD3F5EE),
        background = Color(0xFF1E1E2E),
        onBackground = Color(0xFFCDD6F4),
        surface = Color(0xFF313244),
        onSurface = Color(0xFFCDD6F4),
        surfaceVariant = Color(0xFF45475A),
        onSurfaceVariant = Color(0xFFA6ADC8),
        outline = Color(0xFF6C7086),
        outlineVariant = Color(0xFF585B70),
    )

    private val bubblegumScheme = lightColorScheme(
        primary = Color(0xFFEC4899),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD4E8),
        onPrimaryContainer = Color(0xFF63072F),
        secondary = Color(0xFF34D399),
        onSecondary = Color(0xFF04331F),
        secondaryContainer = Color(0xFFC5F5E1),
        onSecondaryContainer = Color(0xFF08402A),
        tertiary = Color(0xFF38BDF8),
        onTertiary = Color(0xFF042A3D),
        tertiaryContainer = Color(0xFFCDEEFB),
        onTertiaryContainer = Color(0xFF0A3B52),
        background = Color(0xFFFFF0F6),
        onBackground = Color(0xFF3B1D2C),
        surface = Color(0xFFFFF0F6),
        onSurface = Color(0xFF3B1D2C),
        surfaceVariant = Color(0xFFF7DCE8),
        onSurfaceVariant = Color(0xFF71515F),
        outline = Color(0xFFAE8290),
        outlineVariant = Color(0xFFEEC6D6),
    )

    private val pastelGothScheme = darkColorScheme(
        primary = Color(0xFFC9A7EB),
        onPrimary = Color(0xFF25193A),
        primaryContainer = Color(0xFF3E2F57),
        onPrimaryContainer = Color(0xFFE9DBF7),
        secondary = Color(0xFFE59BC4),
        onSecondary = Color(0xFF3A1B2C),
        secondaryContainer = Color(0xFF522F42),
        onSecondaryContainer = Color(0xFFF7DBEA),
        tertiary = Color(0xFF9BE5C4),
        onTertiary = Color(0xFF0E3324),
        tertiaryContainer = Color(0xFF2C4E3F),
        onTertiaryContainer = Color(0xFFD5F5E6),
        background = Color(0xFF1C1822),
        onBackground = Color(0xFFE6DFEC),
        surface = Color(0xFF272231),
        onSurface = Color(0xFFE6DFEC),
        surfaceVariant = Color(0xFF3A3346),
        onSurfaceVariant = Color(0xFFB6ACC2),
        outline = Color(0xFF7C7289),
        outlineVariant = Color(0xFF4C4459),
    )

    private val mintScheme = lightColorScheme(
        primary = Color(0xFF2FB388),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC6F0DF),
        onPrimaryContainer = Color(0xFF073B2A),
        secondary = Color(0xFF2E9EA8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC7EEF1),
        onSecondaryContainer = Color(0xFF06373B),
        tertiary = Color(0xFFE6C34A),
        onTertiary = Color(0xFF3D3005),
        tertiaryContainer = Color(0xFFF8ECC0),
        onTertiaryContainer = Color(0xFF463800),
        background = Color(0xFFF0FBF6),
        onBackground = Color(0xFF17352A),
        surface = Color(0xFFF0FBF6),
        onSurface = Color(0xFF17352A),
        surfaceVariant = Color(0xFFDCEEE6),
        onSurfaceVariant = Color(0xFF4F675D),
        outline = Color(0xFF7F988D),
        outlineVariant = Color(0xFFC5DDD3),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.COTTON_CANDY, "Cotton Candy", ThemeCategory.PASTEL, cottonCandyScheme),
        ThemeSpec(AppTheme.CATPPUCCIN, "Catppuccin", ThemeCategory.PASTEL, catppuccinScheme),
        ThemeSpec(AppTheme.BUBBLEGUM, "Bubblegum", ThemeCategory.PASTEL, bubblegumScheme),
        ThemeSpec(AppTheme.PASTEL_GOTH, "Pastel Goth", ThemeCategory.PASTEL, pastelGothScheme),
        ThemeSpec(AppTheme.MINT, "Mint", ThemeCategory.PASTEL, mintScheme),
    )
}
