package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object WeatherThemes {

    private val stormScheme = darkColorScheme(
        primary = Color(0xFF5B9BD9),
        onPrimary = Color(0xFF06121F),
        primaryContainer = Color(0xFF1E3A54),
        onPrimaryContainer = Color(0xFFCADFF4),
        secondary = Color(0xFF9AA4B0),
        onSecondary = Color(0xFF11151A),
        secondaryContainer = Color(0xFF333A42),
        onSecondaryContainer = Color(0xFFD4DAE1),
        tertiary = Color(0xFFF2D02E),
        onTertiary = Color(0xFF231D02),
        tertiaryContainer = Color(0xFF4A4008),
        onTertiaryContainer = Color(0xFFF8ECB0),
        background = Color(0xFF161A20),
        onBackground = Color(0xFFE3E7ED),
        surface = Color(0xFF161A20),
        onSurface = Color(0xFFE3E7ED),
        surfaceVariant = Color(0xFF262C34),
        onSurfaceVariant = Color(0xFFB4BCC6),
        outline = Color(0xFF707A85),
        outlineVariant = Color(0xFF3A4149),
    )

    private val goldenHourScheme = lightColorScheme(
        primary = Color(0xFFE8912B),
        onPrimary = Color(0xFF2A1700),
        primaryContainer = Color(0xFFFBE0BC),
        onPrimaryContainer = Color(0xFF4A2E05),
        secondary = Color(0xFFE86A7C),
        onSecondary = Color(0xFF2E0710),
        secondaryContainer = Color(0xFFFAD6DC),
        onSecondaryContainer = Color(0xFF4C1420),
        tertiary = Color(0xFFC9A24A),
        onTertiary = Color(0xFF241B02),
        tertiaryContainer = Color(0xFFF5E6BE),
        onTertiaryContainer = Color(0xFF423208),
        background = Color(0xFFFBF1E2),
        onBackground = Color(0xFF3B2A1A),
        surface = Color(0xFFFBF1E2),
        onSurface = Color(0xFF3B2A1A),
        surfaceVariant = Color(0xFFF0E2CF),
        onSurfaceVariant = Color(0xFF6E5C46),
        outline = Color(0xFF9C876C),
        outlineVariant = Color(0xFFDBCAB0),
    )

    private val rainbowScheme = lightColorScheme(
        primary = Color(0xFFE83A3A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAD3D3),
        onPrimaryContainer = Color(0xFF5A0E0E),
        secondary = Color(0xFF2EA84F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCEDD5),
        onSecondaryContainer = Color(0xFF0C3B1B),
        tertiary = Color(0xFF7A3AE0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE3D3FA),
        onTertiaryContainer = Color(0xFF2E0D63),
        background = Color(0xFFFDFDFD),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFDFDFD),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFECECEE),
        onSurfaceVariant = Color(0xFF555459),
        outline = Color(0xFF8A8990),
        outlineVariant = Color(0xFFD5D4D9),
    )

    private val clearNightScheme = darkColorScheme(
        primary = Color(0xFF9DB4D8),
        onPrimary = Color(0xFF0B1830),
        primaryContainer = Color(0xFF243553),
        onPrimaryContainer = Color(0xFFD3DEF2),
        secondary = Color(0xFF7FD4D8),
        onSecondary = Color(0xFF04201F),
        secondaryContainer = Color(0xFF1E3B3D),
        onSecondaryContainer = Color(0xFFC3EAEC),
        tertiary = Color(0xFFE8D9A0),
        onTertiary = Color(0xFF2A2408),
        tertiaryContainer = Color(0xFF463E1C),
        onTertiaryContainer = Color(0xFFF3EBC8),
        background = Color(0xFF0A1428),
        onBackground = Color(0xFFDDE3EF),
        surface = Color(0xFF0A1428),
        onSurface = Color(0xFFDDE3EF),
        surfaceVariant = Color(0xFF1A2740),
        onSurfaceVariant = Color(0xFFAAB6CB),
        outline = Color(0xFF63708A),
        outlineVariant = Color(0xFF2C3B54),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.STORM, "Storm", ThemeCategory.WEATHER, stormScheme),
        ThemeSpec(AppTheme.GOLDEN_HOUR, "Golden Hour", ThemeCategory.WEATHER, goldenHourScheme),
        ThemeSpec(AppTheme.RAINBOW, "Rainbow", ThemeCategory.WEATHER, rainbowScheme),
        ThemeSpec(AppTheme.CLEAR_NIGHT, "Clear Night", ThemeCategory.WEATHER, clearNightScheme),
    )
}
