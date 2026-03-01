package com.podbelly.ui

object WhatsNew {
    const val LATEST_VERSION_CODE = 9

    val changelog: Map<Int, List<String>> = mapOf(
        9 to listOf(
            "What's New dialog shown after updates",
            "Improved release notes in Firebase App Distribution",
        ),
        8 to listOf(
            "Stats screen with streaks, habits, and completion cards",
            "Library search",
            "Episode restart fix",
            "Improved series detail page",
        ),
    )
}
