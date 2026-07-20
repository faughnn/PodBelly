package com.podbelly.core.common.theme

/**
 * Groups the selectable appearance themes into browsable sections.
 *
 * Declaration order here is the order categories appear in the theme picker.
 */
enum class ThemeCategory(val displayName: String) {
    CLASSIC("Classic"),
    RETRO_CONSOLES("Retro Consoles"),
    MODERN_CONSOLES("Modern Consoles"),
    ARCADE("Arcade"),
    GAME_WORLDS("Game Worlds"),
    RETRO_COMPUTING("Retro Computing & Terminals"),
    SYNTHWAVE("Synthwave & Aesthetic"),
    MUSIC("Music & Audio"),
    SPACE("Space & Cosmic"),
    DEVELOPER("Developer"),
    NATURE("Nature & Scenic"),
    PASTEL("Pastel & Soft"),
    MONOCHROME("Monochrome & Minimal"),
}
