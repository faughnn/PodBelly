# Podbelly Improvements Batch — Design

**Date:** 2026-02-22

## 1. CLAUDE.md — Explicit reference app research with tool instructions

Update the Feature Development Workflow to make searching reference apps unmissable. Add explicit instructions to use WebSearch and WebFetch tools to find implementations in Pocket Casts (`github.com/Automattic/pocket-casts-android`) and AntennaPod before writing any feature code.

Include directory hints: playback features → `modules/services/mediaplayer/` and `modules/features/player/`; feed/sync → `modules/services/repositories/`; UI screens → `modules/features/`.

## 2. Speed picker — Grid + slider hybrid

Replace the current 3x3 grid-only speed picker with a hybrid:

- **Preset buttons:** 1.0, 1.2, 1.4, 1.5, 1.6, 1.8, 2.0, 2.5, 3.0 (removed 0.5x and 0.8x)
- **Fine-tune slider:** Below the grid, Slider from 0.5x to 3.0x in 0.1x increments
- **Interaction:** Tapping a preset snaps the slider. Dragging the slider updates the speed live and applies on release.
- **Reference:** Pocket Casts `PlaybackSpeedFragment` has a similar hybrid approach.

**Files:** `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt`

## 3. Library image shape fix

The `PodcastGridItem` in `LibraryScreen.kt` uses `fillMaxWidth()` + `ContentScale.Crop` but has no `aspectRatio(1f)` modifier. Non-square source images can stretch tall, breaking the grid layout.

**Fix:** Add `.aspectRatio(1f)` before `.clip(RoundedCornerShape(14.dp))` to enforce square artwork.

**Files:** `app/src/main/java/com/podbelly/ui/LibraryScreen.kt`

## 4. Refresh on resume with 15-minute cooldown

Add lifecycle-aware refresh so the app refreshes feeds when returning from the background (Android `ON_START`), not just on cold start.

- Add `LifecycleEventObserver` in `MainActivity` observing `ON_START`
- Track `lastRefreshTimestamp` in `AppViewModel`
- Only call `refreshFeeds()` if 15+ minutes since last refresh
- Cold starts still refresh immediately (existing `init` behavior)
- **Reference:** Pocket Casts uses `ProcessLifecycleOwner` for this pattern.

**Files:** `app/src/main/java/com/podbelly/MainActivity.kt`, `app/src/main/java/com/podbelly/AppViewModel.kt`

## 5. Refresh result banner at top

Replace the bottom Snackbar with an animated banner at the top of the home screen content.

- `AnimatedVisibility` with `slideInVertically` from top
- Auto-dismisses after 3 seconds via `LaunchedEffect` + `delay`
- Shows "X new episodes found" or "Everything up to date"
- Jukebox styling: surfaceVariant background, 14dp radius, primary accent

**Files:** `app/src/main/java/com/podbelly/navigation/PodbellNavHost.kt`
