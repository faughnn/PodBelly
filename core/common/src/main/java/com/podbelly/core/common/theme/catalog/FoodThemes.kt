package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object FoodThemes {

    private val Matcha = lightColorScheme(
        primary = Color(0xFF7D9B3F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8E6B4),
        onPrimaryContainer = Color(0xFF2A3A0F),
        secondary = Color(0xFF6E8F5E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6E3C7),
        onSecondaryContainer = Color(0xFF283A1E),
        tertiary = Color(0xFFA8824E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEBD9BC),
        onTertiaryContainer = Color(0xFF3D2C13),
        background = Color(0xFFF6F2E2),
        onBackground = Color(0xFF25330F),
        surface = Color(0xFFFBF8EC),
        onSurface = Color(0xFF25330F),
        surfaceVariant = Color(0xFFE6E4CE),
        onSurfaceVariant = Color(0xFF56543D),
        outline = Color(0xFF87856B),
        outlineVariant = Color(0xFFC9C7AD),
    )

    private val Coffee = darkColorScheme(
        primary = Color(0xFFC9A57A),
        onPrimary = Color(0xFF3A2611),
        primaryContainer = Color(0xFF56391F),
        onPrimaryContainer = Color(0xFFF2DEC4),
        secondary = Color(0xFFD1A06A),
        onSecondary = Color(0xFF3B2510),
        secondaryContainer = Color(0xFF573B1F),
        onSecondaryContainer = Color(0xFFF4DCC0),
        tertiary = Color(0xFFB08968),
        onTertiary = Color(0xFF35200F),
        tertiaryContainer = Color(0xFF4C3320),
        onTertiaryContainer = Color(0xFFECD5C1),
        background = Color(0xFF1E1410),
        onBackground = Color(0xFFEDE0D5),
        surface = Color(0xFF261A14),
        onSurface = Color(0xFFEDE0D5),
        surfaceVariant = Color(0xFF3A2A20),
        onSurfaceVariant = Color(0xFFCDB8A6),
        outline = Color(0xFF917761),
        outlineVariant = Color(0xFF4B3A2D),
    )

    private val Watermelon = lightColorScheme(
        primary = Color(0xFFF0506E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD5DD),
        onPrimaryContainer = Color(0xFF5C0A1D),
        secondary = Color(0xFF2EA84F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFBEEEC6),
        onSecondaryContainer = Color(0xFF083518),
        tertiary = Color(0xFF4A2E1E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE7D2C6),
        onTertiaryContainer = Color(0xFF2C1810),
        background = Color(0xFFFFF2F3),
        onBackground = Color(0xFF3B1119),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF3B1119),
        surfaceVariant = Color(0xFFF3DEE0),
        onSurfaceVariant = Color(0xFF6A4A4E),
        outline = Color(0xFF9C787C),
        outlineVariant = Color(0xFFE2C6C9),
    )

    private val Neapolitan = lightColorScheme(
        primary = Color(0xFFE87A99),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAD6E1),
        onPrimaryContainer = Color(0xFF561329),
        secondary = Color(0xFF6B4226),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE7CDBB),
        onSecondaryContainer = Color(0xFF35200F),
        tertiary = Color(0xFFB8933F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3E4BE),
        onTertiaryContainer = Color(0xFF3E3009),
        background = Color(0xFFF7EFE0),
        onBackground = Color(0xFF3A2C1C),
        surface = Color(0xFFFCF6EB),
        onSurface = Color(0xFF3A2C1C),
        surfaceVariant = Color(0xFFEBE0CF),
        onSurfaceVariant = Color(0xFF5E5344),
        outline = Color(0xFF908573),
        outlineVariant = Color(0xFFD5C9B6),
    )

    private val BloodOrange = darkColorScheme(
        primary = Color(0xFFE84A1A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF7A2409),
        onPrimaryContainer = Color(0xFFFFD6C4),
        secondary = Color(0xFFB0233A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF66101F),
        onSecondaryContainer = Color(0xFFFFD3D9),
        tertiary = Color(0xFFF2A83E),
        onTertiary = Color(0xFF3E2903),
        tertiaryContainer = Color(0xFF5E3F0C),
        onTertiaryContainer = Color(0xFFFCE3BC),
        background = Color(0xFF1E0E08),
        onBackground = Color(0xFFF3DFD6),
        surface = Color(0xFF29140C),
        onSurface = Color(0xFFF3DFD6),
        surfaceVariant = Color(0xFF432217),
        onSurfaceVariant = Color(0xFFD5B4A6),
        outline = Color(0xFF9C7261),
        outlineVariant = Color(0xFF4F2C1F),
    )

    private val Mango = lightColorScheme(
        primary = Color(0xFFF2A81E),
        onPrimary = Color(0xFF3E2A00),
        primaryContainer = Color(0xFFFCE6B0),
        onPrimaryContainer = Color(0xFF4A3200),
        secondary = Color(0xFF5FAF3F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFEDBC),
        onSecondaryContainer = Color(0xFF163808),
        tertiary = Color(0xFFE8708A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFAD4DC),
        onTertiaryContainer = Color(0xFF551122),
        background = Color(0xFFFFF8E6),
        onBackground = Color(0xFF3B2F12),
        surface = Color(0xFFFFFCF2),
        onSurface = Color(0xFF3B2F12),
        surfaceVariant = Color(0xFFEEE6CE),
        onSurfaceVariant = Color(0xFF5E5540),
        outline = Color(0xFF91876C),
        outlineVariant = Color(0xFFD9CFB4),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.MATCHA, "Matcha", ThemeCategory.FOOD, Matcha),
        ThemeSpec(AppTheme.COFFEE, "Coffee", ThemeCategory.FOOD, Coffee),
        ThemeSpec(AppTheme.WATERMELON, "Watermelon", ThemeCategory.FOOD, Watermelon),
        ThemeSpec(AppTheme.NEAPOLITAN, "Neapolitan", ThemeCategory.FOOD, Neapolitan),
        ThemeSpec(AppTheme.BLOOD_ORANGE, "Blood Orange", ThemeCategory.FOOD, BloodOrange),
        ThemeSpec(AppTheme.MANGO, "Mango", ThemeCategory.FOOD, Mango),
    )
}
