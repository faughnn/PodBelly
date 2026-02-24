# Podbelly Development Guidelines

## Feature Development Workflow

When implementing a new feature:

1. **Check RESEARCH.md first** — Read `RESEARCH.md` to identify relevant open-source podcast apps (primarily Pocket Casts and AntennaPod) that may have already implemented the feature.

2. **Search the reference codebases on GitHub** — Before writing ANY code, use WebSearch and WebFetch to find how Pocket Casts or AntennaPod implemented the same feature. Do NOT skip this step.
   - **Repository:** [Pocket Casts](https://github.com/Automattic/pocket-casts-android)
   - Search for relevant class names, screen names, or feature keywords in their repo
   - Read the actual source files to understand their approach
   - **Directory guide for Pocket Casts:**
     - Playback features → `modules/services/mediaplayer/`, `modules/features/player/`
     - Feed/sync features → `modules/services/repositories/`
     - UI screens → `modules/features/` (e.g., `modules/features/podcasts/`, `modules/features/discover/`)
     - Settings → `modules/features/settings/`

3. **Copy working patterns** — Adapt the architecture, state management, and UI patterns from the reference app to fit Podbelly's conventions (Compose, Hilt, Room, Media3). This gives confidence that the feature will work correctly on real devices.

4. **Build and test** — Verify compilation, run unit tests, and install on device to confirm the feature works end-to-end.

## Download-First Playback Pattern

Episodes must be downloaded before they can be played. Never offer a standalone "Play" option for episodes that haven't been downloaded. Every screen should use a single unified action button that follows this state progression:

1. **Not downloaded** → Show download icon/button. Tapping starts the download.
2. **Downloading** → Show progress indicator. Tapping does nothing.
3. **Downloaded** → Show play icon/button. Tapping plays the episode.

This pattern is already implemented in HomeScreen's `EpisodeCard`, PodcastDetailScreen's `EpisodeCard`, and EpisodeDetailScreen. Do not add separate Play and Download buttons — always combine them into one button that transitions through the states above.

## Version Number Management

The app version **must be incremented with every commit**. Update **all three** of the following locations to keep them in sync:

1. **`app/build.gradle.kts`** — `versionCode` (integer, increment by 1)
2. **`app/build.gradle.kts`** — `versionName` (string, e.g. `"1.0.2"`)
3. **`feature/settings/src/main/java/com/podbelly/feature/settings/SettingsScreen.kt`** — The `"Version X.Y.Z"` string displayed in the Settings/About section

All three values must match (versionName and the Settings string should be identical). Forgetting to update any of these causes version mismatches that can trigger update crash loops.

## Key Principle

Always prefer using a known working feature pattern from an established open-source podcast app over designing from scratch. The reference apps have been battle-tested on millions of devices. When in doubt, look at how Pocket Casts does it.
