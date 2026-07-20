package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object ModernConsoleThemes {

    // 1. PlayStation — deep PS blue, near-black-blue background, shape-button accents.
    private val playStationScheme = darkColorScheme(
        primary = Color(0xFF1E6FD9),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF15406F),
        onPrimaryContainer = Color(0xFFBBD6FF),
        secondary = Color(0xFF4FA99B),
        onSecondary = Color(0xFF00201B),
        secondaryContainer = Color(0xFF1E4A43),
        onSecondaryContainer = Color(0xFFB4E7DC),
        tertiary = Color(0xFFE86A9E),
        onTertiary = Color(0xFF3B0A22),
        tertiaryContainer = Color(0xFF5E2340),
        onTertiaryContainer = Color(0xFFFFD3E4),
        background = Color(0xFF070A12),
        onBackground = Color(0xFFE3E7F0),
        surface = Color(0xFF0C111C),
        onSurface = Color(0xFFE3E7F0),
        surfaceVariant = Color(0xFF19202E),
        onSurfaceVariant = Color(0xFFA9B3C6),
        outline = Color(0xFF5A6478),
        outlineVariant = Color(0xFF2A3242),
    )

    // 2. Xbox — Xbox green on graphite/near-black, light-gray text, lighter green secondary.
    private val xboxScheme = darkColorScheme(
        primary = Color(0xFF107C10),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF0A4A0A),
        onPrimaryContainer = Color(0xFFAEEBAE),
        secondary = Color(0xFF5DC15D),
        onSecondary = Color(0xFF03260A),
        secondaryContainer = Color(0xFF1E4A22),
        onSecondaryContainer = Color(0xFFC6EFC6),
        tertiary = Color(0xFF9ADB9A),
        onTertiary = Color(0xFF0A2E10),
        tertiaryContainer = Color(0xFF2E5233),
        onTertiaryContainer = Color(0xFFD6F5D6),
        background = Color(0xFF0D0F0D),
        onBackground = Color(0xFFE4E7E4),
        surface = Color(0xFF141714),
        onSurface = Color(0xFFE4E7E4),
        surfaceVariant = Color(0xFF232823),
        onSurfaceVariant = Color(0xFFAEB5AE),
        outline = Color(0xFF5E655E),
        outlineVariant = Color(0xFF333833),
    )

    // 3. Nintendo Switch — Joy-Con neon red primary, neon blue secondary, dark charcoal.
    private val nintendoSwitchScheme = darkColorScheme(
        primary = Color(0xFFE60012),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF7A0410),
        onPrimaryContainer = Color(0xFFFFD3D2),
        secondary = Color(0xFF00C3E3),
        onSecondary = Color(0xFF00272E),
        secondaryContainer = Color(0xFF0A4E5C),
        onSecondaryContainer = Color(0xFFAEEEFB),
        tertiary = Color(0xFFF0F0F0),
        onTertiary = Color(0xFF1A1A1A),
        tertiaryContainer = Color(0xFF3D3D3D),
        onTertiaryContainer = Color(0xFFEDEDED),
        background = Color(0xFF121212),
        onBackground = Color(0xFFEAEAEA),
        surface = Color(0xFF1A1A1A),
        onSurface = Color(0xFFEAEAEA),
        surfaceVariant = Color(0xFF2A2A2A),
        onSurfaceVariant = Color(0xFFB4B4B4),
        outline = Color(0xFF666666),
        outlineVariant = Color(0xFF383838),
    )

    // 4. Dreamcast — clean white background, swirl orange primary, blue secondary.
    private val dreamcastScheme = lightColorScheme(
        primary = Color(0xFFF08200),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDDB8),
        onPrimaryContainer = Color(0xFF4A2800),
        secondary = Color(0xFF0090D2),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFBDE6FB),
        onSecondaryContainer = Color(0xFF002F44),
        tertiary = Color(0xFFC22030),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDAD8),
        onTertiaryContainer = Color(0xFF44070C),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1B1B1B),
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF1B1B1B),
        surfaceVariant = Color(0xFFEDEBE7),
        onSurfaceVariant = Color(0xFF4A4844),
        outline = Color(0xFF7A7873),
        outlineVariant = Color(0xFFCDCBC6),
    )

    // 5. GameCube — indigo cube: purple primary, deep indigo background, green A, red accents.
    private val gameCubeScheme = darkColorScheme(
        primary = Color(0xFF6B5BD1),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF3A2E85),
        onPrimaryContainer = Color(0xFFD7CFFF),
        secondary = Color(0xFF43B02A),
        onSecondary = Color(0xFF042600),
        secondaryContainer = Color(0xFF1E4E16),
        onSecondaryContainer = Color(0xFFB6EFA6),
        tertiary = Color(0xFFE24A4A),
        onTertiary = Color(0xFF3D0606),
        tertiaryContainer = Color(0xFF5E1E1E),
        onTertiaryContainer = Color(0xFFFFD1D1),
        background = Color(0xFF17123A),
        onBackground = Color(0xFFE5E1F5),
        surface = Color(0xFF1E1848),
        onSurface = Color(0xFFE5E1F5),
        surfaceVariant = Color(0xFF2C2560),
        onSurfaceVariant = Color(0xFFB2ABD1),
        outline = Color(0xFF6A6296),
        outlineVariant = Color(0xFF39316E),
    )

    // 6. Steam Deck — graphite-navy background, Steam blue primary, lighter slate secondary.
    private val steamDeckScheme = darkColorScheme(
        primary = Color(0xFF66C0F4),
        onPrimary = Color(0xFF00344D),
        primaryContainer = Color(0xFF0E4A6B),
        onPrimaryContainer = Color(0xFFC5E7FB),
        secondary = Color(0xFF8F98A0),
        onSecondary = Color(0xFF14181C),
        secondaryContainer = Color(0xFF33404A),
        onSecondaryContainer = Color(0xFFD3DAE0),
        tertiary = Color(0xFF4B9DD6),
        onTertiary = Color(0xFF00243A),
        tertiaryContainer = Color(0xFF1B3D57),
        onTertiaryContainer = Color(0xFFC0DEF3),
        background = Color(0xFF1B2838),
        onBackground = Color(0xFFDCE3EB),
        surface = Color(0xFF223245),
        onSurface = Color(0xFFDCE3EB),
        surfaceVariant = Color(0xFF2E4056),
        onSurfaceVariant = Color(0xFFAAB6C4),
        outline = Color(0xFF5E6E7E),
        outlineVariant = Color(0xFF35485E),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.PLAYSTATION, "PlayStation", ThemeCategory.MODERN_CONSOLES, playStationScheme),
        ThemeSpec(AppTheme.XBOX, "Xbox", ThemeCategory.MODERN_CONSOLES, xboxScheme),
        ThemeSpec(AppTheme.NINTENDO_SWITCH, "Nintendo Switch", ThemeCategory.MODERN_CONSOLES, nintendoSwitchScheme),
        ThemeSpec(AppTheme.DREAMCAST, "Dreamcast", ThemeCategory.MODERN_CONSOLES, dreamcastScheme),
        ThemeSpec(AppTheme.GAMECUBE, "GameCube", ThemeCategory.MODERN_CONSOLES, gameCubeScheme),
        ThemeSpec(AppTheme.STEAM_DECK, "Steam Deck", ThemeCategory.MODERN_CONSOLES, steamDeckScheme),
    )
}
