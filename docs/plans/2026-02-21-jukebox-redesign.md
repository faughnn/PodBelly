# Jukebox Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform Podbelly's visual identity from stock Material 3 purple to the energetic "Jukebox" direction — coral/teal/amber palette, Outfit font, gradient accents, horizontal card carousels, color-coded duration tags, and polished animations.

**Architecture:** This is a UI-only redesign that does not change data models, ViewModels, or business logic. All changes are in theme files, Composable screen functions, build files (for font + Palette dependency), and navigation chrome. The redesign touches every screen but the pattern is consistent: replace color references with theme tokens, swap FontFamily.Default for Outfit, and update component styling.

**Tech Stack:** Jetpack Compose, Material 3, Outfit Google Font (bundled TTF), AndroidX Palette API, Coil, existing animation APIs (spring, animateXxxAsState).

**Build command:** `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
**Test command:** `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`

---

## Phase 1: Foundation (Theme Layer)

### Task 1: Add Outfit font files and Palette API dependency

**Files:**
- Create: `app/src/main/res/font/outfit_light.ttf`
- Create: `app/src/main/res/font/outfit_regular.ttf`
- Create: `app/src/main/res/font/outfit_medium.ttf`
- Create: `app/src/main/res/font/outfit_semibold.ttf`
- Create: `app/src/main/res/font/outfit_bold.ttf`
- Create: `app/src/main/res/font/outfit_extrabold.ttf`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `feature/player/build.gradle.kts` (Palette API for color extraction)

**Step 1: Download Outfit font files**

Download from Google Fonts (https://fonts.google.com/specimen/Outfit). Need these static TTF weights:
- Light (300), Regular (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800)

Place them in `app/src/main/res/font/` with lowercase filenames:
```
outfit_light.ttf
outfit_regular.ttf
outfit_medium.ttf
outfit_semibold.ttf
outfit_bold.ttf
outfit_extrabold.ttf
```

**Step 2: Add Palette API to version catalog**

In `gradle/libs.versions.toml`, add to `[libraries]`:
```toml
androidx-palette = { module = "androidx.palette:palette", version = "1.0.0" }
```

**Step 3: Add Palette dependency to feature/player/build.gradle.kts**

In the `dependencies` block, add:
```kotlin
implementation(libs.androidx.palette)
```

Also add to `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.androidx.palette)
```

**Step 4: Create res/font directory and verify structure**

Run:
```bash
mkdir -p app/src/main/res/font
ls app/src/main/res/font/
```
Expected: The 6 TTF files listed above.

**Step 5: Build to verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts feature/player/build.gradle.kts app/src/main/res/font/
git commit -m "feat: add Outfit font files and Palette API dependency"
```

---

### Task 2: Replace Color.kt with Jukebox palette

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/theme/Color.kt`

**Step 1: Replace the entire Color.kt**

Replace all color definitions with the Jukebox palette. The file has 4 theme variants (light, dark, OLED, high contrast). Each needs all Material 3 color slots.

```kotlin
package com.podbelly.ui.theme

import androidx.compose.ui.graphics.Color

// Jukebox Dark theme colors
val md_theme_dark_primary = Color(0xFFFF6B6B)
val md_theme_dark_onPrimary = Color(0xFF0F1218)
val md_theme_dark_primaryContainer = Color(0xFF3D1A1A)
val md_theme_dark_onPrimaryContainer = Color(0xFFFFB4B4)
val md_theme_dark_secondary = Color(0xFF4ECDC4)
val md_theme_dark_onSecondary = Color(0xFF0A1210)
val md_theme_dark_secondaryContainer = Color(0xFF1A3D3A)
val md_theme_dark_onSecondaryContainer = Color(0xFFA8E8E4)
val md_theme_dark_tertiary = Color(0xFFFFA502)
val md_theme_dark_onTertiary = Color(0xFF1A0E00)
val md_theme_dark_tertiaryContainer = Color(0xFF3D2800)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD080)
val md_theme_dark_error = Color(0xFFFF6B6B)
val md_theme_dark_onError = Color(0xFF0F1218)
val md_theme_dark_errorContainer = Color(0xFF3D1A1A)
val md_theme_dark_onErrorContainer = Color(0xFFFFB4B4)
val md_theme_dark_background = Color(0xFF0A0A12)
val md_theme_dark_onBackground = Color(0xFFE0E4F0)
val md_theme_dark_surface = Color(0xFF0A0A12)
val md_theme_dark_onSurface = Color(0xFFE0E4F0)
val md_theme_dark_surfaceVariant = Color(0xFF1A1F2E)
val md_theme_dark_onSurfaceVariant = Color(0xFF8892A8)
val md_theme_dark_outline = Color(0xFF3A4060)
val md_theme_dark_outlineVariant = Color(0xFF252B3D)
val md_theme_dark_inverseSurface = Color(0xFFE0E4F0)
val md_theme_dark_inverseOnSurface = Color(0xFF0F1218)
val md_theme_dark_inversePrimary = Color(0xFFC62828)

// Jukebox Light theme colors
val md_theme_light_primary = Color(0xFFE84848)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFFFE0E0)
val md_theme_light_onPrimaryContainer = Color(0xFF5A0000)
val md_theme_light_secondary = Color(0xFF2AAA9F)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD0F5F2)
val md_theme_light_onSecondaryContainer = Color(0xFF003D38)
val md_theme_light_tertiary = Color(0xFFE08800)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFE0B0)
val md_theme_light_onTertiaryContainer = Color(0xFF3D2200)
val md_theme_light_error = Color(0xFFE84848)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFE0E0)
val md_theme_light_onErrorContainer = Color(0xFF5A0000)
val md_theme_light_background = Color(0xFFFAFBFF)
val md_theme_light_onBackground = Color(0xFF0F1218)
val md_theme_light_surface = Color(0xFFFAFBFF)
val md_theme_light_onSurface = Color(0xFF0F1218)
val md_theme_light_surfaceVariant = Color(0xFFF0F2FA)
val md_theme_light_onSurfaceVariant = Color(0xFF5A6480)
val md_theme_light_outline = Color(0xFFC0C8D8)
val md_theme_light_outlineVariant = Color(0xFFE0E4F0)
val md_theme_light_inverseSurface = Color(0xFF0F1218)
val md_theme_light_inverseOnSurface = Color(0xFFFAFBFF)
val md_theme_light_inversePrimary = Color(0xFFFF8080)

// Jukebox OLED Dark theme — pure black backgrounds, same accents
val md_theme_oled_primary = Color(0xFFFF6B6B)
val md_theme_oled_onPrimary = Color(0xFF0F1218)
val md_theme_oled_primaryContainer = Color(0xFF3D1A1A)
val md_theme_oled_onPrimaryContainer = Color(0xFFFFB4B4)
val md_theme_oled_secondary = Color(0xFF4ECDC4)
val md_theme_oled_onSecondary = Color(0xFF0A1210)
val md_theme_oled_secondaryContainer = Color(0xFF1A3D3A)
val md_theme_oled_onSecondaryContainer = Color(0xFFA8E8E4)
val md_theme_oled_tertiary = Color(0xFFFFA502)
val md_theme_oled_onTertiary = Color(0xFF1A0E00)
val md_theme_oled_tertiaryContainer = Color(0xFF3D2800)
val md_theme_oled_onTertiaryContainer = Color(0xFFFFD080)
val md_theme_oled_error = Color(0xFFFF6B6B)
val md_theme_oled_onError = Color(0xFF0F1218)
val md_theme_oled_errorContainer = Color(0xFF3D1A1A)
val md_theme_oled_onErrorContainer = Color(0xFFFFB4B4)
val md_theme_oled_background = Color(0xFF000000)
val md_theme_oled_onBackground = Color(0xFFE0E4F0)
val md_theme_oled_surface = Color(0xFF000000)
val md_theme_oled_onSurface = Color(0xFFE0E4F0)
val md_theme_oled_surfaceVariant = Color(0xFF0A0A12)
val md_theme_oled_onSurfaceVariant = Color(0xFF8892A8)
val md_theme_oled_outline = Color(0xFF3A4060)
val md_theme_oled_outlineVariant = Color(0xFF252B3D)
val md_theme_oled_inverseSurface = Color(0xFFE0E4F0)
val md_theme_oled_inverseOnSurface = Color(0xFF000000)
val md_theme_oled_inversePrimary = Color(0xFFC62828)

// Jukebox High Contrast — brighter colors, maximum readability
val md_theme_hc_primary = Color(0xFFFF8080)
val md_theme_hc_onPrimary = Color(0xFF000000)
val md_theme_hc_primaryContainer = Color(0xFFFF6B6B)
val md_theme_hc_onPrimaryContainer = Color(0xFFFFFFFF)
val md_theme_hc_secondary = Color(0xFF70E8E0)
val md_theme_hc_onSecondary = Color(0xFF000000)
val md_theme_hc_secondaryContainer = Color(0xFF4ECDC4)
val md_theme_hc_onSecondaryContainer = Color(0xFFFFFFFF)
val md_theme_hc_tertiary = Color(0xFFFFBB40)
val md_theme_hc_onTertiary = Color(0xFF000000)
val md_theme_hc_tertiaryContainer = Color(0xFFFFA502)
val md_theme_hc_onTertiaryContainer = Color(0xFFFFFFFF)
val md_theme_hc_error = Color(0xFFFF8080)
val md_theme_hc_onError = Color(0xFF000000)
val md_theme_hc_errorContainer = Color(0xFFFF6B6B)
val md_theme_hc_onErrorContainer = Color(0xFFFFFFFF)
val md_theme_hc_background = Color(0xFF000000)
val md_theme_hc_onBackground = Color(0xFFFFFFFF)
val md_theme_hc_surface = Color(0xFF000000)
val md_theme_hc_onSurface = Color(0xFFFFFFFF)
val md_theme_hc_surfaceVariant = Color(0xFF1A1F2E)
val md_theme_hc_onSurfaceVariant = Color(0xFFFFFFFF)
val md_theme_hc_outline = Color(0xFF8892A8)
val md_theme_hc_outlineVariant = Color(0xFF5A6480)
val md_theme_hc_inverseSurface = Color(0xFFFFFFFF)
val md_theme_hc_inverseOnSurface = Color(0xFF000000)
val md_theme_hc_inversePrimary = Color(0xFFC62828)
```

**Step 2: Build to verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/podbelly/ui/theme/Color.kt
git commit -m "feat: replace purple palette with Jukebox coral/teal/amber"
```

---

### Task 3: Replace Type.kt with Outfit typography

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/theme/Type.kt`

**Step 1: Replace Type.kt with Outfit font definitions**

```kotlin
package com.podbelly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.podbelly.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

val PodbellTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.04).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.03).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.03).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.12.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.sp,
    ),
)
```

**Step 2: Build to verify fonts load correctly**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/podbelly/ui/theme/Type.kt
git commit -m "feat: replace default typography with Outfit font family"
```

---

### Task 4: Update Theme.kt — use Jukebox colors, remove dynamic color override

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/theme/Theme.kt`

**Why remove dynamic color:** Jukebox has a strong brand identity. Dynamic color (Material You) would override our coral/teal palette with the user's wallpaper colors, destroying the brand. We keep our custom palette for all API levels.

**Step 1: Update Theme.kt**

```kotlin
package com.podbelly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
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
```

**Key change:** Removed `dynamicLightColorScheme`/`dynamicDarkColorScheme` calls and the `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` check. Jukebox uses its own palette on all API levels.

**Step 2: Build to verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. The unused `android.os.Build` and dynamic color imports should be removed.

**Step 3: Run existing tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`
Expected: All tests pass (theme change shouldn't break logic tests). Screenshot tests will need baselines updated later.

**Step 4: Commit**

```bash
git add app/src/main/java/com/podbelly/ui/theme/Theme.kt
git commit -m "feat: use Jukebox palette for all themes, remove dynamic color"
```

---

## Phase 2: Navigation Chrome

### Task 5: Restyle bottom navigation with underline indicator

**Files:**
- Modify: `app/src/main/java/com/podbelly/navigation/PodbellNavHost.kt`

**Context:** The bottom nav currently uses Material 3's default `NavigationBar` + `NavigationBarItem`. We want to keep the icon+label but add an animated underline bar under the active tab instead of the default filled pill indicator.

**Step 1: Read `PodbellNavHost.kt` to find the NavigationBar composable section**

Find the section that renders the `NavigationBar` and `NavigationBarItem` composables.

**Step 2: Replace the NavigationBar styling**

Change the `NavigationBar` to use our Jukebox surface color and add a custom indicator. The key changes are:
- `NavigationBar` containerColor = `MaterialTheme.colorScheme.background` (not surface)
- Add a `Box` with a thin underline (`3.dp` height, `16.dp` width, primary color) below the active tab icon
- Active tab icon tint = `MaterialTheme.colorScheme.primary` (#FF6B6B)
- Inactive tab icon tint = `MaterialTheme.colorScheme.outline` (#3A4060)
- Active tab label color = `MaterialTheme.colorScheme.primary`
- Inactive tab label color = `MaterialTheme.colorScheme.outline`

**Step 3: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/podbelly/navigation/PodbellNavHost.kt
git commit -m "feat: restyle bottom nav with Jukebox colors and underline indicator"
```

---

### Task 6: Restyle mini-player with gradient background and glow

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/MiniPlayer.kt`

**Step 1: Update MiniPlayer styling**

Changes to make:
- Surface background → `Brush.linearGradient` from `Color(0xFF1A1F2E)` to `Color(0xFF252B3D)`
- Border radius: 16dp on all corners (not just top)
- Add 8dp horizontal margin from screen edges
- Progress bar: gradient brush from primary (#FF6B6B) to tertiary (#FFA502) instead of solid primary
- Artwork: 48dp with 10dp corner radius (already close, just verify)
- Play button: primary tint

**Step 2: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add feature/player/src/main/java/com/podbelly/feature/player/MiniPlayer.kt
git commit -m "feat: restyle mini-player with gradient background and gradient progress"
```

---

### Task 7: Add splash screen with brand animation

**Files:**
- Modify: `app/src/main/java/com/podbelly/MainActivity.kt`
- Create: `app/src/main/java/com/podbelly/ui/SplashScreen.kt`

**Step 1: Create SplashScreen.kt**

A simple Compose screen that shows the animated "podbelly" brand text, then calls `onFinished()`:
- Background: MaterialTheme background color
- "pod" fades in from alpha 0→1 over 400ms
- "belly" slides in from 20dp right + fades in over 400ms with spring physics, colored in primary (#FF6B6B)
- Hold 200ms, then call onFinished
- Use `LaunchedEffect` + `delay` for timing
- Use `animateFloatAsState` for alpha and `animateDpAsState` for offset

**Step 2: Wire into MainActivity**

Add a `var showSplash by remember { mutableStateOf(true) }` in `setContent`. When `showSplash` is true, show `SplashScreen(onFinished = { showSplash = false })`. Otherwise show `PodbellNavHost`. Use `AnimatedContent` or `Crossfade` for the transition.

**Step 3: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/podbelly/ui/SplashScreen.kt app/src/main/java/com/podbelly/MainActivity.kt
git commit -m "feat: add animated splash screen with Jukebox brand reveal"
```

---

## Phase 3: Home Screen Redesign

### Task 8: Add branded header and hero now-playing card to HomeScreen

**Files:**
- Modify: `feature/home/src/main/java/com/podbelly/feature/home/HomeScreen.kt`
- Modify: `feature/home/src/main/java/com/podbelly/feature/home/HomeViewModel.kt` (if needed to expose playback state)

**Step 1: Read the current HomeScreen.kt completely**

Understand the existing composable structure, state parameters, and what data is available.

**Step 2: Add branded header**

Replace the top app bar title "Home" with a branded header row:
- Left: `Text("pod", displayLarge style)` + `Text("belly", displayLarge style, color = primary)`
- Right: Small circular avatar/icon placeholder (32dp, rounded 10dp, primary gradient background)

**Step 3: Add hero now-playing card (conditional)**

If the HomeScreen has access to playback state (check ViewModel), add a hero card at the top of the episode list:
- Full-width card with gradient background (`Color(0xFF1A1530)` → `Color(0xFF0F1828)`)
- "NOW PLAYING" label in labelLarge style, uppercase, with animated pulsing dot
- Row: tilted artwork (-3deg via `Modifier.rotate(-3f)`) 56dp + episode info + play button
- Gradient progress bar below

If HomeScreen doesn't have playback state, this can be deferred to integration work later.

**Step 4: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/home/src/main/java/com/podbelly/feature/home/
git commit -m "feat: add Jukebox branded header and hero now-playing card to home"
```

---

### Task 9: Add horizontal card carousel for recently added episodes

**Files:**
- Modify: `feature/home/src/main/java/com/podbelly/feature/home/HomeScreen.kt`

**Step 1: Add "Recently Added" section**

Below the hero card (or at the top if no hero), add:
- Section header: Row with "Recently Added" (titleLarge) and "See all →" (labelLarge, primary color)
- Horizontal `LazyRow` with 130dp wide episode cards
- Each card: artwork fills top (aspectRatio 1:1, 10dp corners), title below (titleSmall), podcast name below that (labelSmall, onSurfaceVariant)
- 10dp gap between cards, 16dp horizontal padding

**Step 2: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`

**Step 3: Commit**

```bash
git add feature/home/src/main/java/com/podbelly/feature/home/HomeScreen.kt
git commit -m "feat: add horizontal card carousel for recently added episodes"
```

---

### Task 10: Restyle episode list with color-coded duration tags

**Files:**
- Modify: `feature/home/src/main/java/com/podbelly/feature/home/HomeScreen.kt`

**Step 1: Update EpisodeCard composable**

Changes to the existing episode cards:
- Card background: `surfaceVariant` (which is now #1A1F2E dark)
- Border: 1dp `Color(0xFFFFFFFF).copy(alpha = 0.024f)` (subtle glass edge)
- Border radius: 14dp
- Currently-playing card gets 3dp left border in primary color
- Replace the duration text with a color-coded chip:
  - `< 15 min` → teal (#4ECDC4) text on `#4ECDC418` background
  - `15-45 min` → amber (#FFA502) text on `#FFA50218` background
  - `> 45 min` → coral (#FF6B6B) text on `#FF6B6B18` background
  - 6dp corner radius, 3dp vertical / 8dp horizontal padding

**Step 2: Add staggered list animation**

Wrap the LazyColumn items with `AnimatedVisibility` using staggered delays:
- Each item: `fadeIn() + slideInVertically(initialOffsetY = { 40 })` with 50ms * index delay
- Only on first composition (use a `remember` flag)

**Step 3: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`

**Step 4: Commit**

```bash
git add feature/home/src/main/java/com/podbelly/feature/home/HomeScreen.kt
git commit -m "feat: restyle episode cards with color-coded tags and staggered animation"
```

---

## Phase 4: Player Screen

### Task 11: Add Palette API color extraction to PlayerScreen

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt`
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerViewModel.kt` (to expose dominant color)

**Step 1: Read PlayerScreen.kt and PlayerViewModel.kt**

Understand the current player layout and state management.

**Step 2: Add artwork color extraction**

In PlayerViewModel (or a helper composable), use the Palette API to extract the dominant color from the current episode artwork:

```kotlin
// In a composable or ViewModel
val bitmap = // load artwork bitmap via Coil's ImageLoader
val palette = Palette.from(bitmap).generate()
val dominantColor = Color(palette.getDominantColor(defaultColor))
```

For a simpler approach in Compose, use Coil's `AsyncImagePainter` to get the bitmap, then extract the palette.

**Step 3: Use extracted color for background gradient**

Replace the player's background with a gradient tinted by the extracted color:
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    dominantColor.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.background,
                )
            )
        )
)
```

**Step 4: Style transport controls**

- Progress bar: gradient brush primary → tertiary
- Play/pause button: primary filled, 64dp
- Skip buttons: tonal (primaryContainer), 56dp
- Speed/sleep chips: tonal with rounded 12dp corners

**Step 5: Build and verify**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`

**Step 6: Commit**

```bash
git add feature/player/
git commit -m "feat: add artwork color extraction and Jukebox styling to player"
```

---

## Phase 5: Remaining Screens

### Task 12: Restyle PodcastDetailScreen

**Files:**
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/PodcastDetailScreen.kt`

**Changes:**
- Artwork: 120dp with colored glow shadow (box-shadow using Modifier.shadow with SpotColor)
- Title: headlineMedium (Outfit Bold)
- Filter chips: tonal styling (primaryContainer bg, primary text, 12dp radius)
- Episode items: color-coded duration tags (same as home)
- Section labels: labelLarge, uppercase, wide tracking

**Commit:** `git commit -m "feat: restyle podcast detail with Jukebox components"`

---

### Task 13: Restyle EpisodeDetailScreen

**Files:**
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/EpisodeDetailScreen.kt`

**Changes:**
- Large artwork (200dp) with colored glow shadow
- Title: headlineSmall (Outfit Bold)
- Action buttons: primary filled for play, tonal for download
- Color-coded duration tag

**Commit:** `git commit -m "feat: restyle episode detail with Jukebox components"`

---

### Task 14: Restyle LibraryScreen

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/LibraryScreen.kt`

**Changes:**
- Grid cards: 14dp radius, artwork fills card, title overlaid with gradient at bottom
- List rows: episode count as tonal badge with secondary color
- Sort chips: horizontal row of tonal chips
- Section title: headlineLarge (Outfit Bold)

**Commit:** `git commit -m "feat: restyle library with Jukebox card design"`

---

### Task 15: Restyle DiscoverScreen

**Files:**
- Modify: `feature/discover/src/main/java/com/podbelly/feature/discover/DiscoverScreen.kt`

**Changes:**
- Search bar: pill-shaped (28dp radius), primary-colored search icon
- Results: use Jukebox card components (14dp radius, glass border)
- Subscribe button: primary filled when not subscribed, tonal with check when subscribed
- RSS input: styled card with 14dp radius

**Commit:** `git commit -m "feat: restyle discover with Jukebox search and card components"`

---

### Task 16: Restyle DownloadsScreen

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/DownloadsScreen.kt`

**Changes:**
- Same card styling as home episode cards (14dp radius, glass border)
- Failed downloads: 3dp left border in primary (same as error)
- Download progress: gradient progress bar (primary → tertiary)
- Color-coded duration tags

**Commit:** `git commit -m "feat: restyle downloads with Jukebox card components"`

---

### Task 17: Restyle SettingsScreen and StatsScreen

**Files:**
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/StatsScreen.kt`

**Settings changes:**
- Section headers: labelLarge, uppercase, wide letter spacing, primary color
- Setting cards: 14dp radius, surfaceVariant background
- Switches: primary color when checked
- Sliders: primary color thumb and track

**Stats changes:**
- Hero stat cards: gradient background (surfaceVariant → surface)
- Stat values: displayMedium (Outfit ExtraBold)
- Podcast leaderboard: numbered ranks, artwork thumbnails

**Commit:** `git commit -m "feat: restyle settings and stats with Jukebox styling"`

---

## Phase 6: Polish & Finalize

### Task 18: Add haptic feedback to key interactions

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt`
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/MiniPlayer.kt`
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/PodcastDetailScreen.kt`

**Changes:**
Use `LocalView.current.performHapticFeedback()`:
- Light haptic: play/pause button press, pull-to-refresh threshold
- Medium haptic: subscribe/unsubscribe

```kotlin
val view = LocalView.current
// On click:
view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) // light
view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) // medium (API 30+)
```

**Commit:** `git commit -m "feat: add haptic feedback to play/subscribe interactions"`

---

### Task 19: Update screenshot test baselines

**Files:**
- Modify: All `src/test/snapshots/images/` files across feature modules

**Step 1: Record new Paparazzi baselines**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew recordPaparazziDebug`

This regenerates all screenshot baselines with the new Jukebox styling.

**Step 2: Verify the new screenshots look correct**

Visually inspect the generated PNGs in each module's `src/test/snapshots/images/` directory.

**Step 3: Commit**

```bash
git add "*/src/test/snapshots/images/*"
git commit -m "test: update screenshot baselines for Jukebox redesign"
```

---

### Task 20: Run full test suite and fix any failures

**Step 1: Run all unit tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`

**Step 2: Fix any test failures**

Most failures will be from screenshot tests (already handled) or hardcoded color assertions. Fix as needed.

**Step 3: Verify clean build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`

**Step 4: Final commit**

```bash
git commit -m "fix: resolve test failures from Jukebox redesign"
```

---

## Summary

| Phase | Tasks | What Changes |
|-------|-------|-------------|
| 1. Foundation | 1-4 | Font files, Color.kt, Type.kt, Theme.kt |
| 2. Navigation | 5-7 | Bottom nav, mini-player, splash screen |
| 3. Home Screen | 8-10 | Brand header, hero card, carousel, episode cards |
| 4. Player | 11 | Color extraction, gradient bg, transport controls |
| 5. Screens | 12-17 | Podcast detail, episode detail, library, discover, downloads, settings/stats |
| 6. Polish | 18-20 | Haptics, screenshot baselines, test fixes |
