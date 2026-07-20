package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object MusicThemes {

    private val vinylScheme = darkColorScheme(
        primary = Color(0xFFE8C877),
        onPrimary = Color(0xFF2A2005),
        primaryContainer = Color(0xFF5C4A1E),
        onPrimaryContainer = Color(0xFFF6E4B8),
        secondary = Color(0xFFD98A3D),
        onSecondary = Color(0xFF321B06),
        secondaryContainer = Color(0xFF5E3A16),
        onSecondaryContainer = Color(0xFFF7D6B0),
        tertiary = Color(0xFFC15A4A),
        onTertiary = Color(0xFF2E0C07),
        tertiaryContainer = Color(0xFF5A241C),
        onTertiaryContainer = Color(0xFFF6C6BC),
        background = Color(0xFF141210),
        onBackground = Color(0xFFEDE6DA),
        surface = Color(0xFF141210),
        onSurface = Color(0xFFEDE6DA),
        surfaceVariant = Color(0xFF241F19),
        onSurfaceVariant = Color(0xFFBBAF9C),
        outline = Color(0xFF897D68),
        outlineVariant = Color(0xFF3B342A),
    )

    private val lofiScheme = darkColorScheme(
        primary = Color(0xFFE0A3C0),
        onPrimary = Color(0xFF3A162A),
        primaryContainer = Color(0xFF5A3348),
        onPrimaryContainer = Color(0xFFF7D3E4),
        secondary = Color(0xFFA99BD1),
        onSecondary = Color(0xFF241638),
        secondaryContainer = Color(0xFF423658),
        onSecondaryContainer = Color(0xFFE0D6F3),
        tertiary = Color(0xFFD8B78C),
        onTertiary = Color(0xFF332107),
        tertiaryContainer = Color(0xFF554123),
        onTertiaryContainer = Color(0xFFF4DEC3),
        background = Color(0xFF241E2E),
        onBackground = Color(0xFFE9E1F0),
        surface = Color(0xFF241E2E),
        onSurface = Color(0xFFE9E1F0),
        surfaceVariant = Color(0xFF322A3E),
        onSurfaceVariant = Color(0xFFB6ABC4),
        outline = Color(0xFF837791),
        outlineVariant = Color(0xFF453B52),
    )

    private val jazzClubScheme = darkColorScheme(
        primary = Color(0xFFD4A44A),
        onPrimary = Color(0xFF2A1D04),
        primaryContainer = Color(0xFF574018),
        onPrimaryContainer = Color(0xFFF4DBAA),
        secondary = Color(0xFFA33A3A),
        onSecondary = Color(0xFF2E0808),
        secondaryContainer = Color(0xFF541C1C),
        onSecondaryContainer = Color(0xFFF3BEBE),
        tertiary = Color(0xFFC98A3E),
        onTertiary = Color(0xFF2C1B05),
        tertiaryContainer = Color(0xFF57391A),
        onTertiaryContainer = Color(0xFFF3D3AC),
        background = Color(0xFF14100C),
        onBackground = Color(0xFFEAE1D4),
        surface = Color(0xFF14100C),
        onSurface = Color(0xFFEAE1D4),
        surfaceVariant = Color(0xFF231C15),
        onSurfaceVariant = Color(0xFFB7AB98),
        outline = Color(0xFF877B67),
        outlineVariant = Color(0xFF39301F),
    )

    private val punkScheme = darkColorScheme(
        primary = Color(0xFFFF1F6B),
        onPrimary = Color(0xFF33000F),
        primaryContainer = Color(0xFF7A0A31),
        onPrimaryContainer = Color(0xFFFFCADB),
        secondary = Color(0xFFF5E63D),
        onSecondary = Color(0xFF2E2A00),
        secondaryContainer = Color(0xFF5C5400),
        onSecondaryContainer = Color(0xFFFBF4A6),
        tertiary = Color(0xFF2CE0E0),
        onTertiary = Color(0xFF00302F),
        tertiaryContainer = Color(0xFF00524F),
        onTertiaryContainer = Color(0xFFACF6F3),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF161616),
        onSurfaceVariant = Color(0xFFB8B8B8),
        outline = Color(0xFF7A7A7A),
        outlineVariant = Color(0xFF2C2C2C),
    )

    private val radioStaticScheme = darkColorScheme(
        primary = Color(0xFFF0A830),
        onPrimary = Color(0xFF301F02),
        primaryContainer = Color(0xFF5E4012),
        onPrimaryContainer = Color(0xFFF9DCA8),
        secondary = Color(0xFFD64545),
        onSecondary = Color(0xFF320909),
        secondaryContainer = Color(0xFF5C1E1E),
        onSecondaryContainer = Color(0xFFF5C2C2),
        tertiary = Color(0xFFBFB8A8),
        onTertiary = Color(0xFF2B2820),
        tertiaryContainer = Color(0xFF45413A),
        onTertiaryContainer = Color(0xFFE6E0D3),
        background = Color(0xFF17140F),
        onBackground = Color(0xFFE8E2D6),
        surface = Color(0xFF17140F),
        onSurface = Color(0xFFE8E2D6),
        surfaceVariant = Color(0xFF25211A),
        onSurfaceVariant = Color(0xFFB5AD9C),
        outline = Color(0xFF847C6A),
        outlineVariant = Color(0xFF3A342A),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.VINYL, "Vinyl", ThemeCategory.MUSIC, vinylScheme),
        ThemeSpec(AppTheme.LOFI, "Lo-fi", ThemeCategory.MUSIC, lofiScheme),
        ThemeSpec(AppTheme.JAZZ_CLUB, "Jazz Club", ThemeCategory.MUSIC, jazzClubScheme),
        ThemeSpec(AppTheme.PUNK, "Punk", ThemeCategory.MUSIC, punkScheme),
        ThemeSpec(AppTheme.RADIO_STATIC, "Radio Static", ThemeCategory.MUSIC, radioStaticScheme),
    )
}
