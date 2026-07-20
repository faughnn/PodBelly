package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object MonochromeThemes {

    // 1. Grayscale — dark, pure neutral grays, no hue.
    private val grayscaleScheme = darkColorScheme(
        primary = Color(0xFFCFCFCF),
        onPrimary = Color(0xFF1E1E1E),
        primaryContainer = Color(0xFF3A3A3A),
        onPrimaryContainer = Color(0xFFE6E6E6),
        secondary = Color(0xFF9E9E9E),
        onSecondary = Color(0xFF1E1E1E),
        secondaryContainer = Color(0xFF333333),
        onSecondaryContainer = Color(0xFFDADADA),
        tertiary = Color(0xFFB0B0B0),
        onTertiary = Color(0xFF1E1E1E),
        tertiaryContainer = Color(0xFF2E2E2E),
        onTertiaryContainer = Color(0xFFD4D4D4),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE6E6E6),
        surface = Color(0xFF181818),
        onSurface = Color(0xFFE6E6E6),
        surfaceVariant = Color(0xFF262626),
        onSurfaceVariant = Color(0xFFB3B3B3),
        outline = Color(0xFF6E6E6E),
        outlineVariant = Color(0xFF3C3C3C),
    )

    // 2. Sepia — light, warm aged paper.
    private val sepiaScheme = lightColorScheme(
        primary = Color(0xFF8A6A3B),
        onPrimary = Color(0xFFFBF3E2),
        primaryContainer = Color(0xFFE4D2AF),
        onPrimaryContainer = Color(0xFF3A2C15),
        secondary = Color(0xFFA98C5F),
        onSecondary = Color(0xFF33260F),
        secondaryContainer = Color(0xFFE8DABA),
        onSecondaryContainer = Color(0xFF3E301A),
        tertiary = Color(0xFF7A7B4A),
        onTertiary = Color(0xFFF7F1DE),
        tertiaryContainer = Color(0xFFDCDCB6),
        onTertiaryContainer = Color(0xFF33341A),
        background = Color(0xFFF3E9D2),
        onBackground = Color(0xFF43341F),
        surface = Color(0xFFF7EEDA),
        onSurface = Color(0xFF43341F),
        surfaceVariant = Color(0xFFE7DBC0),
        onSurfaceVariant = Color(0xFF6E5C3F),
        outline = Color(0xFF938264),
        outlineVariant = Color(0xFFCEBE9E),
    )

    // 3. E-Ink — light, e-reader paper, ultra minimal.
    private val eInkScheme = lightColorScheme(
        primary = Color(0xFF2E2E2E),
        onPrimary = Color(0xFFF7F7F4),
        primaryContainer = Color(0xFFDCDCD8),
        onPrimaryContainer = Color(0xFF1A1A1A),
        secondary = Color(0xFF5A5A5A),
        onSecondary = Color(0xFFF7F7F4),
        secondaryContainer = Color(0xFFE3E3DF),
        onSecondaryContainer = Color(0xFF262626),
        tertiary = Color(0xFF707070),
        onTertiary = Color(0xFFF7F7F4),
        tertiaryContainer = Color(0xFFE8E8E4),
        onTertiaryContainer = Color(0xFF2A2A2A),
        background = Color(0xFFF7F7F4),
        onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFCFCFA),
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFE9E9E5),
        onSurfaceVariant = Color(0xFF565656),
        outline = Color(0xFF8C8C8C),
        outlineVariant = Color(0xFFCBCBC6),
    )

    // 4. Newspaper — light newsprint, black ink, one classic-red accent (tertiary).
    private val newspaperScheme = lightColorScheme(
        primary = Color(0xFF1C1A17),
        onPrimary = Color(0xFFF2EEE6),
        primaryContainer = Color(0xFFD9D4C8),
        onPrimaryContainer = Color(0xFF1C1A17),
        secondary = Color(0xFF5C574E),
        onSecondary = Color(0xFFF2EEE6),
        secondaryContainer = Color(0xFFE1DCD1),
        onSecondaryContainer = Color(0xFF2A2620),
        tertiary = Color(0xFFB22222),
        onTertiary = Color(0xFFF6ECEC),
        tertiaryContainer = Color(0xFFF0CFCF),
        onTertiaryContainer = Color(0xFF5C1010),
        background = Color(0xFFF2EEE6),
        onBackground = Color(0xFF1C1A17),
        surface = Color(0xFFF7F3EC),
        onSurface = Color(0xFF1C1A17),
        surfaceVariant = Color(0xFFE4DFD5),
        onSurfaceVariant = Color(0xFF57534B),
        outline = Color(0xFF8A857A),
        outlineVariant = Color(0xFFC9C3B7),
    )

    // 5. Blueprint — dark, draughtsman's blueprint, white ink, light-cyan accents.
    private val blueprintScheme = darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF0D2A5C),
        primaryContainer = Color(0xFF1C4287),
        onPrimaryContainer = Color(0xFFEAF0FF),
        secondary = Color(0xFF8FD0FF),
        onSecondary = Color(0xFF0A2247),
        secondaryContainer = Color(0xFF16386F),
        onSecondaryContainer = Color(0xFFCFE6FF),
        tertiary = Color(0xFFB9D3FF),
        onTertiary = Color(0xFF0A2247),
        tertiaryContainer = Color(0xFF163465),
        onTertiaryContainer = Color(0xFFDCE8FF),
        background = Color(0xFF0D2A5C),
        onBackground = Color(0xFFEAF0FF),
        surface = Color(0xFF102F63),
        onSurface = Color(0xFFEAF0FF),
        surfaceVariant = Color(0xFF193C78),
        onSurfaceVariant = Color(0xFFAFC4E8),
        outline = Color(0xFF6E8DC4),
        outlineVariant = Color(0xFF2A4A85),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.GRAYSCALE, "Grayscale", ThemeCategory.MONOCHROME, grayscaleScheme),
        ThemeSpec(AppTheme.SEPIA, "Sepia", ThemeCategory.MONOCHROME, sepiaScheme),
        ThemeSpec(AppTheme.E_INK, "E-Ink", ThemeCategory.MONOCHROME, eInkScheme),
        ThemeSpec(AppTheme.NEWSPAPER, "Newspaper", ThemeCategory.MONOCHROME, newspaperScheme),
        ThemeSpec(AppTheme.BLUEPRINT, "Blueprint", ThemeCategory.MONOCHROME, blueprintScheme),
    )
}
