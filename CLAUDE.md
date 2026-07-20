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

The app version **must be incremented with every commit**. Update **both** of the following locations in `app/build.gradle.kts`:

1. **`versionCode`** (integer, increment by 1)
2. **`versionName`** (string, e.g. `"1.0.2"`)

The Settings screen reads the version dynamically at runtime via `PackageManager`, so it does **not** need manual updates.

## What's New / Patch Notes

When bumping the version, also update `app/src/main/java/com/podbelly/ui/WhatsNew.kt`:

1. Add a new entry to `changelog` keyed by the new `versionCode`
2. Update `LATEST_VERSION_CODE` to match
3. Write **user-facing** descriptions — no commit messages, branch names, or technical jargon. Describe what the user will notice (e.g. "Queue: long-press episodes to Play Next or Play Last"), not implementation details (e.g. "gate advanceQueue on queueEnabled preference")
4. Skip purely internal changes (refactors, test fixes, docs) — only list things the user can see or interact with

## Building & CI

**Do not attempt a local Gradle build in the cloud/agent environment.** The
outbound network policy blocks `dl.google.com` (the Android Gradle Plugin and
dependencies) and the Gradle distribution host with `403`, and there is no
pre-populated dependency cache — so `./gradlew` and the system `gradle` both
fail to resolve plugins. Don't burn time retrying; it will not succeed here.

**Compilation and tests are verified by GitHub Actions instead.** When a pull
request is opened (or updated), the `build` check runs the Gradle build and
unit tests on CI. Treat that check as the source of truth for whether the code
compiles:

1. Make the change, then verify by hand what you can (id/coverage checks,
   reading the diff, confirming exhaustive `when`s and imports).
2. Push and open/refresh the PR — let the `build` check compile it.
3. If `build` fails, read the CI logs, fix, and push again.

State plainly in the PR/description that local compilation could not be run and
CI is the compile check.

## Key Principle

Always prefer using a known working feature pattern from an established open-source podcast app over designing from scratch. The reference apps have been battle-tested on millions of devices. When in doubt, look at how Pocket Casts does it.
