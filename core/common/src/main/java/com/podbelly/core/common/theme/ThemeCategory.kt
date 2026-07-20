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
    CINEMA("Cinema & Sci-Fi"),
    ANIME("Anime & Manga"),
    RETRO_COMPUTING("Retro Computing & Terminals"),
    DEVELOPER("Developer"),
    SYNTHWAVE("Synthwave & Aesthetic"),
    Y2K("Y2K / Frutiger Aero"),
    MUSIC("Music & Audio"),
    SPACE("Space & Cosmic"),
    WEATHER("Weather & Sky"),
    NATURE("Nature & Scenic"),
    FANTASY("Fantasy & RPG"),
    GEMSTONES("Gemstones"),
    MATERIALS("Materials & Metals"),
    FOOD("Food & Drink"),
    PASTEL("Pastel & Soft"),
    MONOCHROME("Monochrome & Minimal"),
    SEASONAL("Seasonal & Holiday"),
    MOOD("Mood & Focus"),
}
