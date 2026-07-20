package com.podbelly.core.common.theme.catalog

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.ThemeCategory
import com.podbelly.core.common.theme.ThemeSpec

object MaterialThemes {

    private val Gold = darkColorScheme(
        primary = Color(0xFFD4AF37),
        onPrimary = Color(0xFF2B2200),
        primaryContainer = Color(0xFF4A3D0E),
        onPrimaryContainer = Color(0xFFF3E2A1),
        secondary = Color(0xFFE0A93F),
        onSecondary = Color(0xFF2E2000),
        secondaryContainer = Color(0xFF4C3810),
        onSecondaryContainer = Color(0xFFF6DCA6),
        tertiary = Color(0xFFEFE0B0),
        onTertiary = Color(0xFF33290A),
        tertiaryContainer = Color(0xFF4E441F),
        onTertiaryContainer = Color(0xFFF7EFCF),
        background = Color(0xFF12100A),
        onBackground = Color(0xFFEDE7D6),
        surface = Color(0xFF17140D),
        onSurface = Color(0xFFEDE7D6),
        surfaceVariant = Color(0xFF262115),
        onSurfaceVariant = Color(0xFFCBC1A6),
        outline = Color(0xFF938A6E),
        outlineVariant = Color(0xFF473F2C),
    )

    private val RoseGold = lightColorScheme(
        primary = Color(0xFFC9857E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF6DAD5),
        onPrimaryContainer = Color(0xFF3E1714),
        secondary = Color(0xFFD79BA0),
        onSecondary = Color(0xFF3A1519),
        secondaryContainer = Color(0xFFF8DFE2),
        onSecondaryContainer = Color(0xFF3A1519),
        tertiary = Color(0xFF9C8574),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEEE0D6),
        onTertiaryContainer = Color(0xFF35271B),
        background = Color(0xFFFBF1EE),
        onBackground = Color(0xFF241917),
        surface = Color(0xFFFFF7F5),
        onSurface = Color(0xFF241917),
        surfaceVariant = Color(0xFFEEDCD8),
        onSurfaceVariant = Color(0xFF6E5652),
        outline = Color(0xFF9C7E79),
        outlineVariant = Color(0xFFE0C7C2),
    )

    private val Copper = darkColorScheme(
        primary = Color(0xFFC87A3F),
        onPrimary = Color(0xFF2B1400),
        primaryContainer = Color(0xFF4A2C13),
        onPrimaryContainer = Color(0xFFF5D2B4),
        secondary = Color(0xFF3FB0A0),
        onSecondary = Color(0xFF00332D),
        secondaryContainer = Color(0xFF0F463F),
        onSecondaryContainer = Color(0xFFB4EFE6),
        tertiary = Color(0xFFC9985E),
        onTertiary = Color(0xFF2E1E00),
        tertiaryContainer = Color(0xFF48380F),
        onTertiaryContainer = Color(0xFFF3E0C0),
        background = Color(0xFF17110C),
        onBackground = Color(0xFFEFE3D7),
        surface = Color(0xFF1D160F),
        onSurface = Color(0xFFEFE3D7),
        surfaceVariant = Color(0xFF2E2318),
        onSurfaceVariant = Color(0xFFCFBBA5),
        outline = Color(0xFF97846E),
        outlineVariant = Color(0xFF4B3B2A),
    )

    private val Gunmetal = darkColorScheme(
        primary = Color(0xFF8A98A6),
        onPrimary = Color(0xFF10171E),
        primaryContainer = Color(0xFF2C3742),
        onPrimaryContainer = Color(0xFFCEDAE6),
        secondary = Color(0xFF7C8896),
        onSecondary = Color(0xFF11181F),
        secondaryContainer = Color(0xFF2A343E),
        onSecondaryContainer = Color(0xFFC6D1DC),
        tertiary = Color(0xFFB6C2CE),
        onTertiary = Color(0xFF1A222B),
        tertiaryContainer = Color(0xFF37424D),
        onTertiaryContainer = Color(0xFFDDE6EF),
        background = Color(0xFF14181C),
        onBackground = Color(0xFFE1E6EB),
        surface = Color(0xFF191E23),
        onSurface = Color(0xFFE1E6EB),
        surfaceVariant = Color(0xFF283038),
        onSurfaceVariant = Color(0xFFB2BCC6),
        outline = Color(0xFF7C8791),
        outlineVariant = Color(0xFF3C454E),
    )

    private val Emerald = darkColorScheme(
        primary = Color(0xFF2ECC71),
        onPrimary = Color(0xFF00280F),
        primaryContainer = Color(0xFF0E4325),
        onPrimaryContainer = Color(0xFFAFEEC6),
        secondary = Color(0xFF2FA99A),
        onSecondary = Color(0xFF00302A),
        secondaryContainer = Color(0xFF0D453E),
        onSecondaryContainer = Color(0xFFAEEDE3),
        tertiary = Color(0xFFD9C878),
        onTertiary = Color(0xFF2E2800),
        tertiaryContainer = Color(0xFF473F13),
        onTertiaryContainer = Color(0xFFF3E9B6),
        background = Color(0xFF0C1A12),
        onBackground = Color(0xFFDCEBE1),
        surface = Color(0xFF112117),
        onSurface = Color(0xFFDCEBE1),
        surfaceVariant = Color(0xFF1E3327),
        onSurfaceVariant = Color(0xFFB4CBBD),
        outline = Color(0xFF7C9587),
        outlineVariant = Color(0xFF334A3B),
    )

    private val Obsidian = darkColorScheme(
        primary = Color(0xFF8A6FD1),
        onPrimary = Color(0xFF190A38),
        primaryContainer = Color(0xFF322050),
        onPrimaryContainer = Color(0xFFDCCFF5),
        secondary = Color(0xFF9A9AA8),
        onSecondary = Color(0xFF17171F),
        secondaryContainer = Color(0xFF2C2C38),
        onSecondaryContainer = Color(0xFFDADAE4),
        tertiary = Color(0xFF6FBFB8),
        onTertiary = Color(0xFF002E2A),
        tertiaryContainer = Color(0xFF12403B),
        onTertiaryContainer = Color(0xFFBEECE6),
        background = Color(0xFF0A0A0D),
        onBackground = Color(0xFFE4E3EA),
        surface = Color(0xFF0F0F14),
        onSurface = Color(0xFFE4E3EA),
        surfaceVariant = Color(0xFF1E1E27),
        onSurfaceVariant = Color(0xFFBBBAC8),
        outline = Color(0xFF83828F),
        outlineVariant = Color(0xFF35343F),
    )

    val specs: List<ThemeSpec> = listOf(
        ThemeSpec(AppTheme.GOLD, "Gold", ThemeCategory.MATERIALS, Gold),
        ThemeSpec(AppTheme.ROSE_GOLD, "Rose Gold", ThemeCategory.MATERIALS, RoseGold),
        ThemeSpec(AppTheme.COPPER, "Copper", ThemeCategory.MATERIALS, Copper),
        ThemeSpec(AppTheme.GUNMETAL, "Gunmetal", ThemeCategory.MATERIALS, Gunmetal),
        ThemeSpec(AppTheme.EMERALD, "Emerald", ThemeCategory.MATERIALS, Emerald),
        ThemeSpec(AppTheme.OBSIDIAN, "Obsidian", ThemeCategory.MATERIALS, Obsidian),
    )
}
