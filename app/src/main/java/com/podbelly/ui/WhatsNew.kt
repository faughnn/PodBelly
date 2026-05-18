package com.podbelly.ui

object WhatsNew {
    const val LATEST_VERSION_CODE = 31

    val changelog: Map<Int, List<String>> = mapOf(
        31 to listOf(
            "Play buttons on Home, Podcast, and Episode pages now turn into Pause while the episode is playing — tapping them actually pauses instead of restarting the episode",
        ),
        30 to listOf(
            "Episode page Play button turns into Pause while the episode is playing",
        ),
        29 to listOf(
            "Continue Listening is now sorted by most recently played, so the episode you were just listening to is always first",
            "Settings now shows how much space your downloaded episodes are using next to the Delete all downloads button",
        ),
        28 to listOf(
            "Podcast artwork now loads faster and fades in smoothly while scrolling",
        ),
        27 to listOf(
            "Fixed playback speed reverting to 1x when returning to the app after a long pause",
        ),
        26 to listOf(
            "Fixed the player showing blank after returning to the app following a long pause",
        ),
        25 to listOf(
            "Fixed crash when searching for podcasts in Discover",
        ),
        24 to listOf(
            "Fixed crash when starting a download on Android 14+",
        ),
        23 to listOf(
            "Fixed downloads not completing after tapping the download button",
            "Tap the progress circle while downloading to cancel the download",
        ),
        22 to listOf(
            "Fast forward 30 seconds from the notification playback controls",
        ),
        21 to listOf(
            "Downloads now continue in the background when you switch to another app",
        ),
        20 to listOf(
            "New episodes now slide in smoothly when you refresh instead of popping in",
        ),
        19 to listOf(
            "Continue Listening now tracks your position more accurately, even if the app closes unexpectedly",
        ),
        18 to listOf(
            "Download errors now show a message on screen so you can see what went wrong",
        ),
        17 to listOf(
            "Continue Listening now reliably shows all in-progress episodes",
            "Episodes are marked as played when they finish",
        ),
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
