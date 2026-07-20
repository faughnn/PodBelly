package com.podbelly.core.common.theme

/**
 * Groups the selectable appearance themes into browsable sections.
 *
 * Declaration order here is the order categories appear in the theme picker.
 */
enum class ThemeCategory(val displayName: String) {
    CLASSIC("Classic"),
    RETRO_CONSOLES("Retro Consoles"),
    ARCADE("Arcade"),
    RETRO_COMPUTING("Retro Computing & Terminals"),
    SYNTHWAVE("Synthwave & Aesthetic"),
    DEVELOPER("Developer"),
    NATURE("Nature & Scenic"),
}
