package com.podbelly.ui

object WhatsNew {
    const val LATEST_VERSION_CODE = 12

    val changelog: Map<Int, List<String>> = mapOf(
        12 to listOf(
            "Finished episodes now look clearly played (faded artwork and checkmark)",
            "Playback speed is remembered per podcast and applied instantly",
            "View and reset per-podcast speeds in Settings",
        ),
        9 to listOf(
            "What's New dialog shown after updates",
        ),
        8 to listOf(
            "Stats screen with streaks, habits, and completion cards",
            "Library search",
            "Episode restart fix",
            "Improved series detail page",
        ),
    )
}
