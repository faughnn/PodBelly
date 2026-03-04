package com.podbelly.ui

object WhatsNew {
    const val LATEST_VERSION_CODE = 16

    val changelog: Map<Int, List<String>> = mapOf(
        16 to listOf(
            "Test: verifying What's New dialog appears on update",
        ),
        15 to listOf(
            "Settings moved to the top bar for easier access",
        ),
        14 to listOf(
            "Queue: long-press episodes to Play Next or Play Last",
            "Add to Queue button on episode details",
            "Enable or disable the queue from Settings",
        ),
        13 to listOf(
            "Crash reporting with Firebase Crashlytics",
            "Improved OPML import reliability",
            "Orientation change no longer crashes the app",
        ),
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
