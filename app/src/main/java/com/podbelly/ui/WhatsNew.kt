package com.podbelly.ui

object WhatsNew {
    // 65 was a test-stability fix with no user-facing changes; no changelog entry.
    // 72 was internal test hardening with no user-facing changes; no changelog entry.
    // 73-75 wired up coverage reporting (CI only); no changelog entries.
    // 83-84 were docs-only (CLAUDE.md); no changelog entries.
    const val LATEST_VERSION_CODE = 85

    val changelog: Map<Int, List<String>> = mapOf(
        85 to listOf(
            "Tidied up the theme list: trimmed a batch of near-duplicate themes so the Appearance picker is easier to browse (if you were on one of the removed themes, the app falls back to System default)",
        ),
        82 to listOf(
            "Ten more theme categories in Settings > Appearance — over 50 new looks to explore",
            "Cinema & Sci-Fi and Anime & Manga: Lightsaber, Xenomorph, Grid Rider, Red Eye, Spice Planet, Shonen, Magical Girl, Mecha, Ink Wash and Cyber Ronin",
            "Y2K / Frutiger Aero: Aqua Gel, Lime Gloss, Chrome, Bubble Blue and Frost Glass",
            "Weather & Sky and Space extras: Storm, Golden Hour, Fog, Rainbow and Clear Night",
            "Fantasy & RPG: Dungeon, Elven, Dragonfire, Potion, Mana Blue and Necromancer",
            "Gemstones and Materials & Metals: Amethyst, Sapphire, Ruby, Jade, Opal, Gold, Rose Gold, Copper, Gunmetal, Emerald and Obsidian",
            "Food & Drink: Matcha, Coffee, Watermelon, Neapolitan, Blood Orange and Mango",
            "Seasonal & Holiday: Halloween, Christmas, Autumn, Winter Frost, Spring Bloom and Valentine",
            "Mood & Focus: Calm, Energize, Deep Focus, Cozy and Night Shift (a warm, low-blue-light theme for night listening)",
        ),
        81 to listOf(
            "Six more theme categories with dozens of new looks to try in Settings > Appearance",
            "Modern Consoles: PlayStation, Xbox, Nintendo Switch, Dreamcast, GameCube and Steam Deck",
            "Game Worlds: colourful looks inspired by famous games — Blocky Overworld, Rip & Tear, Test Chamber, Hero of Time, Monster Trainer, Blue Blur and Pip Terminal",
            "Music & Audio: Vinyl, Lo-fi, Jazz Club, Punk and Radio Static",
            "Space & Cosmic: Nebula, Mars, Galaxy, Solar Flare and Deep Space",
            "Pastel & Soft: Cotton Candy, Catppuccin, Bubblegum, Pastel Goth and Mint",
            "Monochrome & Minimal: Grayscale, Sepia, E-Ink, Newspaper and Blueprint",
        ),
        80 to listOf(
            "The theme picker is now organised into browsable categories with a colour preview beside each theme — tap a category to expand it",
            "Loads more themes to choose from, grouped as Classic, Retro Consoles, Arcade, Retro Computing & Terminals, Synthwave & Aesthetic, Developer and Nature & Scenic",
            "New retro-console looks: Game Boy Color, Sega Genesis, Virtual Boy and Atari 2600",
            "New arcade looks: Space Invaders, Tetris, Donkey Kong, Neon Cabinet and Frogger",
            "New retro-computing looks: MS-DOS, ZX Spectrum, Amiga Workbench and Apple II",
            "New synthwave & aesthetic looks: Outrun, Miami Vice, Hologram and Laser Grid",
            "New developer looks: Nord, Gruvbox, Solarized Dark, Solarized Light, Monokai and Tokyo Night",
            "New nature looks: Forest, Deep Ocean, Sunset, Aurora, Cherry Blossom and Desert",
        ),
        79 to listOf(
            "Eleven new appearance themes in Settings > Appearance, including retro and video-game looks: Game Boy, Nintendo (NES), Super Nintendo, Commodore 64, Terminal Green, Terminal Amber, Synthwave, Vaporwave, Cyberpunk, Arcade and Dracula",
        ),
        78 to listOf(
            "Tap the About card on the You tab for the complete version history — every update's patch notes in one scrollable list",
        ),
        77 to listOf(
            "The player now shows when the episode came out and how long it runs, right under the show name",
            "New Notes button on the player opens the episode's full show notes without leaving playback",
            "The transcript button is now labelled \"Transcript\" instead of a bare icon, so it's easier to spot on shows that provide one",
        ),
        76 to listOf(
            "Pick exactly which shows auto-download: Settings > Downloads > Auto-download per show lists every subscription with its listening history beside a Smart / Always / Never picker",
            "The same picker now sits on every card in Stats > Podcasts, so the least-listened list doubles as the place to mute shows you never play",
        ),
        // 70 never shipped (CI test fix re-roll).
        71 to listOf(
            "Tapping a bottom-bar button now always brings you back to that screen's start — even from a podcast or episode page (tap again for a fresh scroll-to-top)",
            "Per-show auto-download: set any podcast to Always or Never auto-download from its page menu, overriding the smart setting",
            "Smart auto-download window is now adjustable: count shows you've listened to in the last 7, 14, 30 or 60 days",
            "Keep per show: cap smart downloads at the newest 1, 3, 5 or 10 unplayed episodes per show — older auto-downloads clean themselves up (your manual downloads are never touched)",
            "Only while charging: optionally defer auto-downloads until the phone is plugged in",
        ),
        69 to listOf(
            "New \"You\" tab in the bottom bar: your listening stats front and center — today, this week and your streak — one tap from anywhere",
            "Tap the summary card for the full stats: charts, top shows and your Year in Review",
            "Settings reorganized into quick categories (Playback, Downloads, Appearance, Feeds, Import & Export, Diagnostics) instead of one long scroll",
            "The settings gear moved off the Home screen into the new You tab",
        ),
        68 to listOf(
            "Skip ad chapters: for shows that mark their ads as chapters, the player now jumps straight over them (Settings > Playback)",
            "Chapters embedded in episodes now show up in the player — tap the chapter title for the full list",
            "Merge duplicate subscriptions: Stats > Podcasts can now fold copies of the same show into one, moving your listening history and downloads over",
            "Smart auto-download: only fetches new episodes from shows you've actually listened to in the last 30 days (Settings > Downloads)",
            "Auto-delete played downloads after 1, 3, 7 or 30 days to free up space",
            "The player's seek bar now shades the intro/outro stretches your skip settings jump over",
            "Year in Review: a shareable summary of your listening year, at the top of Stats",
            "The queue feature is gone — playback simply stops at the end of an episode",
        ),
        67 to listOf(
            "Podcast and episode titles no longer show raw \"&amp;\" codes — existing titles correct themselves on the next feed refresh",
        ),
        66 to listOf(
            "Stats now flags duplicate subscriptions — the same show added via different feeds — at the top of the Podcasts tab, with each copy's feed and listening so you can unsubscribe the spare",
        ),
        64 to listOf(
            "Tap any podcast in Stats — the Podcasts tab cards or the Top lists — to open its page before deciding to unsubscribe",
        ),
        63 to listOf(
            "Stats Overview redesigned: a big listening headline with today/week/month, a 30-day listening chart, and day-of-week and hour-of-day charts",
            "Time saved now adds up faster playback, silence trimming and skipped intros/outros in one card — skipped intro/outro time is tracked from now on",
            "New stats: average playback speed, session counts, days listened, keep-up rate for new arrivals, and a library summary",
            "Filter the Overview by All time, This year or Last 30 days",
            "Most-listened podcasts and episodes moved to their own Top tab",
        ),
        62 to listOf(
            "Stats has a new Podcasts tab: every subscription ranked least-listened first, with played counts, last-listened dates, downloads and more",
            "Unsubscribe from neglected shows right from the list (with undo)",
            "Sort the list by least listened, least played or longest idle",
        ),
        61 to listOf(
            "Opening the app is much snappier: feeds only refresh when your refresh interval has actually elapsed, not on every launch",
            "Artwork loads immediately even while a refresh is running",
            "The app stays smooth during large refreshes instead of stuttering while feeds update",
        ),
        60 to listOf(
            "Skip intro & outro now takes an exact number of seconds instead of preset buttons",
            "From the player, pause where the ads end and tap \"Up to now\" to set the intro — or where they begin and tap \"After now\" for the outro",
            "An outro set mid-episode now applies to the episode you're listening to, not just the next one",
        ),
        59 to listOf(
            "Tap the podcast name on the Now Playing screen to jump to that show's episodes",
            "Skip intro & outro can now be set right from the Now Playing screen (⋮ menu)",
            "Tap the tab you're already on to jump back to the top of that screen",
            "The playing episode now shows a pause button on its card in Home, and tapping it pauses instead of restarting",
        ),
        57 to listOf(
            "Discover is tidier: add-by-RSS now lives behind the feed button beside the search bar",
            "Pick your chart region: tap the country chip next to the categories to browse another country's top podcasts",
        ),
        54 to listOf(
            "Home-screen widget: see what's playing and control it without opening the app",
        ),
        53 to listOf(
            "Chromecast support: tap the cast button on the player to send your episode to a TV or speaker",
            "Casting plays the show's stream; switching back to your phone resumes your downloaded copy where you left off",
        ),
        52 to listOf(
            "Android Auto support: browse your queue and downloaded episodes from the car screen",
            "Playback started in the car picks up where you left off, with your per-podcast speed and intro skip",
        ),
        51 to listOf(
            "Read along with episode transcripts: tap the transcript button on the player for shows that provide them",
            "Tap any line in the transcript to jump straight to that moment",
        ),
        50 to listOf(
            "Skip intros and outros automatically: set per-podcast skip times from the podcast page menu (Skip intro & outro)",
            "Episodes start past the intro and finish before the outro, then move on to your queue as usual",
        ),
        49 to listOf(
            "Fixed feeds with special characters (accents, smart quotes) potentially displaying incorrectly on some devices",
            "Fixed feeds that start with a byte-order mark failing to load",
        ),
        48 to listOf(
            "Discover now shows the top podcast charts for your country the moment you open it — no search needed",
            "Browse charts by category: Comedy, News, True Crime, Technology, Sport and more",
        ),
        47 to listOf(
            "Feed refresh is much faster: up to 32 podcasts update at once instead of 5",
            "Refreshing uses far less memory, even for shows with huge episode archives",
        ),
        46 to listOf(
            "Tap the New header to clear the section — episodes slide back into their place in the list",
        ),
        45 to listOf(
            "The refresh status moved to the top bar beside the logo, so it stays visible while you scroll",
            "While feeds are refreshing you now see live progress, e.g. \"Checking 7/12\"",
        ),
        44 to listOf(
            "Home now shows a refresh status below Continue Listening: a spinner while checking for new episodes, and \"Updated X ago\" the rest of the time",
        ),
        42 to listOf(
            "The New section now clears when you return to the app after a while away, not only after a full restart",
        ),
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
