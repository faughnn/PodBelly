package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object PastelThemes {

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
        ThemeSpec(AppTheme.BUBBLEGUM, "Bubblegum", ThemeCategory.PASTEL, bubblegumScheme),
        ThemeSpec(AppTheme.MINT, "Mint", ThemeCategory.PASTEL, mintScheme),
    )
}
