package com.podbelly.core.common.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.podbelly.core.common.AppTheme
import com.podbelly.core.common.theme.catalog.ArcadeThemes
import com.podbelly.core.common.theme.catalog.ClassicThemes
import com.podbelly.core.common.theme.catalog.DeveloperThemes
import com.podbelly.core.common.theme.catalog.GameWorldThemes
import com.podbelly.core.common.theme.catalog.ModernConsoleThemes
import com.podbelly.core.common.theme.catalog.MonochromeThemes
import com.podbelly.core.common.theme.catalog.MusicThemes
import com.podbelly.core.common.theme.catalog.NatureThemes
import com.podbelly.core.common.theme.catalog.PastelThemes
import com.podbelly.core.common.theme.catalog.RetroComputingThemes
import com.podbelly.core.common.theme.catalog.RetroConsoleThemes
import com.podbelly.core.common.theme.catalog.SpaceThemes
import com.podbelly.core.common.theme.catalog.SynthwaveThemes

/**
 * The single source of truth for every selectable appearance theme.
 *
 * Each category contributes its themes from its own file under [catalog]; this
 * object just aggregates them and answers the questions the UI and the app
 * theme need: resolve a scheme, list categories, list the themes in a category.
 */
object ThemeCatalog {

    /** All themes, in category-declaration then in-file order. */
    val specs: List<ThemeSpec> = buildList {
        addAll(ClassicThemes.specs)
        addAll(RetroConsoleThemes.specs)
        addAll(ModernConsoleThemes.specs)
        addAll(ArcadeThemes.specs)
        addAll(GameWorldThemes.specs)
        addAll(RetroComputingThemes.specs)
        addAll(SynthwaveThemes.specs)
        addAll(MusicThemes.specs)
        addAll(SpaceThemes.specs)
        addAll(DeveloperThemes.specs)
        addAll(NatureThemes.specs)
        addAll(PastelThemes.specs)
        addAll(MonochromeThemes.specs)
    }

    private val byId: Map<AppTheme, ThemeSpec> = specs.associateBy { it.id }

    /** The spec for [id], falling back to the system entry for unknown ids. */
    fun specFor(id: AppTheme): ThemeSpec = byId[id] ?: byId.getValue(AppTheme.SYSTEM)

    /** Categories that actually contain at least one theme, in display order. */
    fun categoriesInOrder(): List<ThemeCategory> =
        ThemeCategory.entries.filter { category -> specs.any { it.category == category } }

    /** The themes in [category], in the order they were declared. */
    fun themesIn(category: ThemeCategory): List<ThemeSpec> =
        specs.filter { it.category == category }

    /**
     * Resolves the color scheme for [id], mapping the system-following entry
     * onto light or dark according to [darkSystem].
     */
    fun colorSchemeFor(id: AppTheme, darkSystem: Boolean): ColorScheme =
        specFor(id).colorScheme ?: if (darkSystem) ClassicThemes.dark else ClassicThemes.light

    /**
     * A few representative colors (background, primary, secondary, tertiary)
     * for rendering a small preview swatch beside a theme in the picker.
     */
    fun swatchFor(id: AppTheme, darkSystem: Boolean): List<Color> {
        val scheme = colorSchemeFor(id, darkSystem)
        return listOf(scheme.background, scheme.primary, scheme.secondary, scheme.tertiary)
    }
}
