package com.podbelly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
)

private val OledDarkColorScheme = darkColorScheme(
    primary = md_theme_oled_primary,
    onPrimary = md_theme_oled_onPrimary,
    primaryContainer = md_theme_oled_primaryContainer,
    onPrimaryContainer = md_theme_oled_onPrimaryContainer,
    secondary = md_theme_oled_secondary,
    onSecondary = md_theme_oled_onSecondary,
    secondaryContainer = md_theme_oled_secondaryContainer,
    onSecondaryContainer = md_theme_oled_onSecondaryContainer,
    tertiary = md_theme_oled_tertiary,
    onTertiary = md_theme_oled_onTertiary,
    tertiaryContainer = md_theme_oled_tertiaryContainer,
    onTertiaryContainer = md_theme_oled_onTertiaryContainer,
    error = md_theme_oled_error,
    onError = md_theme_oled_onError,
    errorContainer = md_theme_oled_errorContainer,
    onErrorContainer = md_theme_oled_onErrorContainer,
    background = md_theme_oled_background,
    onBackground = md_theme_oled_onBackground,
    surface = md_theme_oled_surface,
    onSurface = md_theme_oled_onSurface,
    surfaceVariant = md_theme_oled_surfaceVariant,
    onSurfaceVariant = md_theme_oled_onSurfaceVariant,
    outline = md_theme_oled_outline,
    outlineVariant = md_theme_oled_outlineVariant,
    inverseSurface = md_theme_oled_inverseSurface,
    inverseOnSurface = md_theme_oled_inverseOnSurface,
    inversePrimary = md_theme_oled_inversePrimary,
)

private val HighContrastColorScheme = darkColorScheme(
    primary = md_theme_hc_primary,
    onPrimary = md_theme_hc_onPrimary,
    primaryContainer = md_theme_hc_primaryContainer,
    onPrimaryContainer = md_theme_hc_onPrimaryContainer,
    secondary = md_theme_hc_secondary,
    onSecondary = md_theme_hc_onSecondary,
    secondaryContainer = md_theme_hc_secondaryContainer,
    onSecondaryContainer = md_theme_hc_onSecondaryContainer,
    tertiary = md_theme_hc_tertiary,
    onTertiary = md_theme_hc_onTertiary,
    tertiaryContainer = md_theme_hc_tertiaryContainer,
    onTertiaryContainer = md_theme_hc_onTertiaryContainer,
    error = md_theme_hc_error,
    onError = md_theme_hc_onError,
    errorContainer = md_theme_hc_errorContainer,
    onErrorContainer = md_theme_hc_onErrorContainer,
    background = md_theme_hc_background,
    onBackground = md_theme_hc_onBackground,
    surface = md_theme_hc_surface,
    onSurface = md_theme_hc_onSurface,
    surfaceVariant = md_theme_hc_surfaceVariant,
    onSurfaceVariant = md_theme_hc_onSurfaceVariant,
    outline = md_theme_hc_outline,
    outlineVariant = md_theme_hc_outlineVariant,
    inverseSurface = md_theme_hc_inverseSurface,
    inverseOnSurface = md_theme_hc_inverseOnSurface,
    inversePrimary = md_theme_hc_inversePrimary,
)

// ── Retro & video-game themes ─────────────────────────────────────────
// Each palette is tuned so on* colors keep readable contrast against their
// base. Unspecified Material roles fall back to the darkColorScheme/
// lightColorScheme defaults, which are close enough for those secondary roles.

// Game Boy — the classic DMG four-shade green LCD (light theme).
private val GameBoyColorScheme = lightColorScheme(
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
    background = Color(0xFF9BBC0F),
    onBackground = Color(0xFF0F380F),
    surface = Color(0xFF8BAC0F),
    onSurface = Color(0xFF0F380F),
    surfaceVariant = Color(0xFF8BAC0F),
    onSurfaceVariant = Color(0xFF0F380F),
    outline = Color(0xFF306230),
    outlineVariant = Color(0xFF306230),
)

// NES — near-black console body with Nintendo red accents.
private val NesColorScheme = darkColorScheme(
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
    background = Color(0xFF161616),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF585858),
    outlineVariant = Color(0xFF333333),
)

// Super Nintendo — the console's purple/lavender button palette.
private val SnesColorScheme = darkColorScheme(
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
    background = Color(0xFF16121F),
    onBackground = Color(0xFFE7E0F5),
    surface = Color(0xFF1E1830),
    onSurface = Color(0xFFE7E0F5),
    surfaceVariant = Color(0xFF2C2440),
    onSurfaceVariant = Color(0xFFB9AED4),
    outline = Color(0xFF6C5B9E),
    outlineVariant = Color(0xFF352B4D),
)

// Synthwave — neon sunset: hot pink, cyan and orange on deep indigo.
private val SynthwaveColorScheme = darkColorScheme(
    primary = Color(0xFFFF2E97),
    onPrimary = Color(0xFF1A0B2E),
    primaryContainer = Color(0xFF5C0A3D),
    onPrimaryContainer = Color(0xFFFFB3DC),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF001F26),
    secondaryContainer = Color(0xFF0A3D47),
    onSecondaryContainer = Color(0xFF9CF3FF),
    tertiary = Color(0xFFFF9E00),
    onTertiary = Color(0xFF241400),
    tertiaryContainer = Color(0xFF5C3A00),
    onTertiaryContainer = Color(0xFFFFDDA8),
    background = Color(0xFF1A0B2E),
    onBackground = Color(0xFFF0E6FF),
    surface = Color(0xFF241245),
    onSurface = Color(0xFFF0E6FF),
    surfaceVariant = Color(0xFF33195C),
    onSurfaceVariant = Color(0xFFC7A8F0),
    outline = Color(0xFFFF2E97),
    outlineVariant = Color(0xFF3D1F5C),
)

// Vaporwave — dreamy pastel pink and blue on muted purple.
private val VaporwaveColorScheme = darkColorScheme(
    primary = Color(0xFFFF6AD5),
    onPrimary = Color(0xFF2B1B3D),
    primaryContainer = Color(0xFF6B2E5C),
    onPrimaryContainer = Color(0xFFFFD1F0),
    secondary = Color(0xFF94D0FF),
    onSecondary = Color(0xFF0A2138),
    secondaryContainer = Color(0xFF2E4A6B),
    onSecondaryContainer = Color(0xFFD1E8FF),
    tertiary = Color(0xFFC774E8),
    onTertiary = Color(0xFF2B1B3D),
    background = Color(0xFF2B1B3D),
    onBackground = Color(0xFFF5E6FF),
    surface = Color(0xFF352447),
    onSurface = Color(0xFFF5E6FF),
    surfaceVariant = Color(0xFF43305A),
    onSurfaceVariant = Color(0xFFCDB3E0),
    outline = Color(0xFF94D0FF),
    outlineVariant = Color(0xFF43305A),
)

// Commodore 64 — the iconic light-blue-on-blue boot screen.
private val Commodore64ColorScheme = darkColorScheme(
    primary = Color(0xFFA599E9),
    onPrimary = Color(0xFF211862),
    primaryContainer = Color(0xFF352A8C),
    onPrimaryContainer = Color(0xFFCDC5FF),
    secondary = Color(0xFF8B7EE0),
    onSecondary = Color(0xFF211862),
    secondaryContainer = Color(0xFF3B2F96),
    onSecondaryContainer = Color(0xFFCDC5FF),
    tertiary = Color(0xFFB5AAF0),
    onTertiary = Color(0xFF211862),
    background = Color(0xFF40318D),
    onBackground = Color(0xFFB5AAF0),
    surface = Color(0xFF483BA0),
    onSurface = Color(0xFFB5AAF0),
    surfaceVariant = Color(0xFF524495),
    onSurfaceVariant = Color(0xFFB5AAF0),
    outline = Color(0xFF8B7EE0),
    outlineVariant = Color(0xFF524495),
)

// Terminal Green — CRT phosphor green on black.
private val TerminalGreenColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF001A00),
    primaryContainer = Color(0xFF003D14),
    onPrimaryContainer = Color(0xFF7CFFA0),
    secondary = Color(0xFF00CC33),
    onSecondary = Color(0xFF001A00),
    secondaryContainer = Color(0xFF00330D),
    onSecondaryContainer = Color(0xFF7CFFA0),
    tertiary = Color(0xFF33FF66),
    onTertiary = Color(0xFF001A00),
    background = Color(0xFF000D00),
    onBackground = Color(0xFF33FF66),
    surface = Color(0xFF001500),
    onSurface = Color(0xFF33FF66),
    surfaceVariant = Color(0xFF002200),
    onSurfaceVariant = Color(0xFF00CC33),
    outline = Color(0xFF008F26),
    outlineVariant = Color(0xFF002200),
)

// Terminal Amber — the other classic CRT phosphor, warm amber on black.
private val TerminalAmberColorScheme = darkColorScheme(
    primary = Color(0xFFFFB000),
    onPrimary = Color(0xFF1A0F00),
    primaryContainer = Color(0xFF3D2900),
    onPrimaryContainer = Color(0xFFFFD980),
    secondary = Color(0xFFCC8800),
    onSecondary = Color(0xFF1A0F00),
    secondaryContainer = Color(0xFF332100),
    onSecondaryContainer = Color(0xFFFFD980),
    tertiary = Color(0xFFFFC94D),
    onTertiary = Color(0xFF1A0F00),
    background = Color(0xFF0D0800),
    onBackground = Color(0xFFFFB000),
    surface = Color(0xFF150E00),
    onSurface = Color(0xFFFFB000),
    surfaceVariant = Color(0xFF221800),
    onSurfaceVariant = Color(0xFFCC8800),
    outline = Color(0xFF8F6200),
    outlineVariant = Color(0xFF221800),
)

// Cyberpunk — neon yellow and cyan over near-black steel.
private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFFCEE0A),
    onPrimary = Color(0xFF14160A),
    primaryContainer = Color(0xFF3D3A00),
    onPrimaryContainer = Color(0xFFFFF69C),
    secondary = Color(0xFF00F0FF),
    onSecondary = Color(0xFF001F24),
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = Color(0xFF9CF6FF),
    tertiary = Color(0xFFFF003C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF5C0016),
    onTertiaryContainer = Color(0xFFFFB3C2),
    background = Color(0xFF0A0E12),
    onBackground = Color(0xFFE6F7FA),
    surface = Color(0xFF12181F),
    onSurface = Color(0xFFE6F7FA),
    surfaceVariant = Color(0xFF1C242D),
    onSurfaceVariant = Color(0xFF8FA5B0),
    outline = Color(0xFF00F0FF),
    outlineVariant = Color(0xFF1C242D),
)

// Arcade — the black cabinet screen with Pac-Man yellow, maze blue and pink.
private val ArcadeColorScheme = darkColorScheme(
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

// Dracula — the popular developer palette.
private val DraculaColorScheme = darkColorScheme(
    primary = Color(0xFFBD93F9),
    onPrimary = Color(0xFF21222C),
    primaryContainer = Color(0xFF44396B),
    onPrimaryContainer = Color(0xFFE4D6FF),
    secondary = Color(0xFFFF79C6),
    onSecondary = Color(0xFF21222C),
    secondaryContainer = Color(0xFF6B2E52),
    onSecondaryContainer = Color(0xFFFFD1EC),
    tertiary = Color(0xFF8BE9FD),
    onTertiary = Color(0xFF21222C),
    tertiaryContainer = Color(0xFF1E4A54),
    onTertiaryContainer = Color(0xFFC2F5FF),
    error = Color(0xFFFF5555),
    onError = Color(0xFF21222C),
    errorContainer = Color(0xFF6B1F1F),
    onErrorContainer = Color(0xFFFFC2C2),
    background = Color(0xFF282A36),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF282A36),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF44475A),
    onSurfaceVariant = Color(0xFFC7C9D9),
    outline = Color(0xFF6272A4),
    outlineVariant = Color(0xFF44475A),
)

@Composable
fun PodbellTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.OLED_DARK -> OledDarkColorScheme
        AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.GAME_BOY -> GameBoyColorScheme
        AppTheme.NES -> NesColorScheme
        AppTheme.SNES -> SnesColorScheme
        AppTheme.SYNTHWAVE -> SynthwaveColorScheme
        AppTheme.VAPORWAVE -> VaporwaveColorScheme
        AppTheme.COMMODORE_64 -> Commodore64ColorScheme
        AppTheme.TERMINAL_GREEN -> TerminalGreenColorScheme
        AppTheme.TERMINAL_AMBER -> TerminalAmberColorScheme
        AppTheme.CYBERPUNK -> CyberpunkColorScheme
        AppTheme.ARCADE -> ArcadeColorScheme
        AppTheme.DRACULA -> DraculaColorScheme
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PodbellTypography,
        content = content
    )
}

/** Backward-compatible overload. */
@Composable
fun PodbellTheme(
    darkThemeMode: AppTheme = AppTheme.SYSTEM,
    @Suppress("UNUSED_PARAMETER") useDarkThemeMode: Boolean = true,
    content: @Composable () -> Unit
) {
    PodbellTheme(appTheme = darkThemeMode, content = content)
}
