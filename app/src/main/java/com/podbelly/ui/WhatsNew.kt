package com.podbelly.ui

object WhatsNew {
    const val LATEST_VERSION_CODE = 41

    val changelog: Map<Int, List<String>> = mapOf(
        41 to listOf(
            "Fixed just-released episodes sometimes landing under Earlier instead of the New section",
            "Episodes you've already played no longer show in the New section",
        ),
        40 to listOf(
            "Episodes that arrive from a refresh now appear in a \"New\" section at the top of Home, so nothing slips past you further down the list",
            "Once you've seen them, they return to their usual place in the list",
        ),
        39 to listOf(
            "Continue Listening no longer shows episodes you've already finished (\"0 seconds left\")",
        ),
        38 to listOf(
            "Fixed a crash when tapping rewind or fast-forward repeatedly from the notification or lock screen",
            "Fast-forwarding near the end of an episode no longer marks it as finished",
        ),
        37 to listOf(
            "A show's cover art, title and description now update when the podcast changes them — not only when you first subscribe",
        ),
        36 to listOf(
            "Background feed refresh now actually runs on the interval you set, so new episodes and notifications arrive without opening the app",
            "Switching straight to another episode now saves your place in the one you were listening to",
            "Tapping the episode that's already playing in the queue now just opens the player instead of restarting it",
            "The lock-screen and notification 30-second skip no longer jumps to the start while an episode is buffering",
            "Smoother player screen when the artwork changes between episodes",
            "Downloads from servers that don't report a file size now show an active spinner instead of a stuck 0%",
            "Starting a download while offline no longer leaves a stuck progress spinner",
            "Subscribing by RSS URL now shows a progress spinner while it works",
            "Library now shows the podcast placeholder for shows without artwork instead of a blank tile",
            "Fixed some podcasts showing a title or link taken from their artwork instead of the real show details",
        ),
        35 to listOf(
            "Auto-advancing to the next queued episode now plays your downloaded file — no more streaming or failing when offline",
            "The player no longer keeps showing a finished episode at 0:00 when there's nothing left to play",
            "Storage usage now stays accurate after feeds refresh instead of dropping to zero for downloaded episodes",
            "Podcast titles and descriptions with accents, smart quotes and em dashes now display correctly",
            "Clearing the Discover search box no longer briefly repopulates old results",
            "Failed downloads no longer leave leftover files wasting storage",
            "Downloads blocked by Wi-Fi-only no longer show a stuck progress spinner",
            "Listening stats are more accurate when skipping quickly between episodes",
            "Faster, smoother app startup",
        ),
        34 to listOf(
            "Volume boost no longer changes your device's media volume",
            "Skipping forward or scrubbing no longer jumps to the start while an episode is buffering",
            "Scrubbing the player progress bar is now smooth",
            "Switching tabs keeps your place (scroll position and search) instead of resetting",
            "Listening stats and streaks now use your local time zone",
            "Subscribe buttons no longer all disable while one subscription is in progress",
            "Episode details now stay up to date when a podcast updates them",
        ),
        33 to listOf(
            "Sleep timer's \"end of episode\" now reliably pauses when the episode finishes",
            "Playing an episode from the queue now continues to the correct next episode",
            "Queue playback now uses each podcast's custom speed",
            "Downloads automatically retry after a temporary network drop",
            "Feed refresh now respects the interval you set in Settings",
            "Fixed some episodes not showing up when subscribed to multiple shows",
        ),
        32 to listOf(
            "Fixed episodes restarting from the beginning after they finished instead of stopping",
        ),
        31 to listOf(
            "Fixed a rare crash that could happen when playback was unexpectedly interrupted",
        ),
        30 to listOf(
            "Settings now has a Diagnostics section — share crash logs from your phone when something goes wrong",
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
