# Podbelly Development Guidelines

## Feature Development Workflow

When implementing a new feature:

1. **Check RESEARCH.md first** — Read `RESEARCH.md` to identify relevant open-source podcast apps (primarily Pocket Casts and AntennaPod) that may have already implemented the feature.

2. **Research existing implementations** — Before writing code, search the reference codebases (especially [Pocket Casts](https://github.com/Automattic/pocket-casts-android)) to find how they implemented the feature. Use their proven patterns rather than reinventing the wheel.

3. **Copy working patterns** — Adapt the architecture, state management, and UI patterns from the reference app to fit Podbelly's conventions (Compose, Hilt, Room, Media3). This gives confidence that the feature will work correctly on real devices.

4. **Build and test** — Verify compilation, run unit tests, and install on device to confirm the feature works end-to-end.

## Key Principle

Always prefer using a known working feature pattern from an established open-source podcast app over designing from scratch. The reference apps have been battle-tested on millions of devices.
