package com.podbelly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun PodbellTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.OLED_DARK -> OledDarkColorScheme
        AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        else -> {
            val darkTheme = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                else -> isSystemInDarkTheme()
            }
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
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
