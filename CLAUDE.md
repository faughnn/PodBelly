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

## Key Principle

Always prefer using a known working feature pattern from an established open-source podcast app over designing from scratch. The reference apps have been battle-tested on millions of devices. When in doubt, look at how Pocket Casts does it.
