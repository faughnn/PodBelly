# Open-source Android podcast apps with the best documentation

**Pocket Casts and AntennaPod stand out as the two most valuable reference codebases** for someone building an Android podcast app from scratch, though for different reasons: Pocket Casts offers a modern Kotlin/Compose multi-module architecture used in a production-grade commercial app, while AntennaPod provides the most transparent documentation of architectural evolution across 13 years of development. Several smaller projects round out the landscape, but none match these two for combined documentation depth.

The gap between user documentation and developer documentation is consistent across every project reviewed. User-facing docs tend to be thorough; architectural documentation that explains *why* the code is structured a certain way is rare. This report ranks six notable projects by their combined documentation value, with emphasis on developer docs.

---

## 1. Pocket Casts — the modern reference architecture

**Repository:** https://github.com/Automattic/pocket-casts-android  
**Stars:** ~2,800 | **License:** MPL-2.0 | **Last release:** v8.4 (Feb 2, 2026)

Pocket Casts is the strongest reference for building a modern Android podcast app. Open-sourced by Automattic in October 2022, it represents a **production-quality, multi-module Kotlin codebase** with Jetpack Compose migration underway. The app ships to millions of users through Google Play, making it a rare example of a commercial-grade open-source podcast player.

**Tech stack:** 100% Kotlin, Jetpack Compose (ongoing migration from XML), Dagger/Hilt for DI, Room for local storage, Media3 (ExoPlayer) for playback, Kotlin Coroutines & Flow, Protocol Buffers for server serialization, Gradle Kotlin DSL. Supports Wear OS, Android Automotive, and Chromecast.

**Developer documentation highlights:**

- **Module structure in README** — The directory tree in the README maps out a clean feature-based modularization: `modules/features/account`, `modules/features/discover`, `modules/features/player`, `modules/features/podcasts`, `modules/features/profile`, `modules/features/filters`, `modules/features/endofyear`, plus shared `services` modules. Separate `app`, `automotive`, and `wear` entry points demonstrate multi-form-factor architecture.
- **CONTRIBUTING.md** (https://github.com/Automattic/pocket-casts-android/blob/main/CONTRIBUTING.md) — Covers beta testing, "good first issue" onboarding, GlotPress translation workflow, PR process, and code guidelines.
- **TESTING-CHECKLIST.md** — A comprehensive manual QA checklist covering subscriptions, folders, Up Next, search, playback, video, swipe actions, and premium features. Invaluable for understanding feature scope.
- **CHANGELOG.md** — Detailed version-by-version history with links to individual PRs.
- **CLAUDE.md and AGENTS.md** — AI coding agent instruction files, showing modern development practices and containing useful project context.
- **CI/CD infrastructure** — BuildKite pipelines (`.buildkite/`), Fastlane for deployment (`fastlane/`), Danger for automated PR checks, gitleaks for secret scanning.

**User documentation:** The support center at https://support.pocketcasts.com/android/ contains **39+ articles** covering sync, notifications, auto-downloads, playback errors, duplicate episodes, and advanced troubleshooting. A blog at https://blog.pocketcasts.com/ covers feature announcements. Community forum at https://forums.pocketcasts.com/.

**What's missing:** No dedicated architecture decision records (ADRs), no high-level design document explaining the dependency graph between modules, and no explicit documentation of patterns (MVVM, repository pattern). You'll need to read the code to understand data flow. The `docs/` directory exists but isn't prominently linked.

**Why it ranks #1 for app builders:** The combination of Kotlin, Compose, Hilt, Room, Media3, and feature-based modularization represents the current best-practice Android stack. The multi-form-factor support (phone, watch, car) demonstrates how to architect for platform diversity. Reading this codebase teaches production patterns that no documentation alone could convey.

---

## 2. AntennaPod — the most transparent architectural journey

**Repository:** https://github.com/AntennaPod/AntennaPod  
**Stars:** ~7,600 | **License:** GPL-3.0 | **Last release:** v3.11.0 (Jan 13, 2026)

AntennaPod is the oldest and most popular open-source podcast app for Android, active since **2011** with **200+ contributors** over its lifetime. While its Java-based codebase represents an older technical approach, it offers something no other project does: **detailed public documentation of a multi-year architectural refactoring**, making it uniquely educational for understanding how podcast app architecture evolves.

**Tech stack:** Primarily Java (not Kotlin), XML layouts (no Jetpack Compose), OkHttp for networking, custom SQLite (not Room), Media3/ExoPlayer for playback, RxJava 2, EventBus (greenrobot), Glide for images, Jsoup for HTML parsing. Gradle Groovy DSL. **~31 acyclic modules** after restructuring.

**Developer documentation highlights:**

- **"Modernizing the AntennaPod Code Structure" blog post** (https://antennapod.org/blog/2024/05/modernizing-the-code-structure) — This is the single most valuable architectural document across all projects reviewed. Written by lead developer ByteHamster, it chronicles **62 pull requests over 3 years** that transformed a tightly coupled 2-module app into 31 acyclic modules. It explains the methodology: using Gephi graph analysis to identify strongly connected components, breaking cyclic dependencies, then grouping files into Gradle modules. Includes before/after dependency graphs.
- **GitHub Issue #4661** (https://github.com/AntennaPod/AntennaPod/issues/4661) — "Modularize code to improve testability" — contains extensive technical discussion about module architecture with dependency visualizations.
- **CONTRIBUTING.md** (https://github.com/AntennaPod/AntennaPod/blob/develop/CONTRIBUTING.md) — Covers branching model (`develop` → `master`), build instructions, PR guidelines, code quality enforcement via Checkstyle, SpotBugs, and Android Lint. Quality check command: `./gradlew checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug`.
- **Website contribute section** (https://antennapod.org/contribute/develop) — Separate pages for app development, website contributions, upstream projects, and infrastructure.
- **GitHub Wiki** — Pages on code style (https://github.com/AntennaPod/AntennaPod/wiki/Code-style) and debug builds (https://github.com/AntennaPod/AntennaPod/wiki/Debug-Build).
- **YouTube visualization** of code structure evolution (2011–2024): https://www.youtube.com/watch?v=kILkeiLGkJY

**User documentation:** The most comprehensive of any project reviewed. The documentation site at https://antennapod.org/documentation/ covers getting started, playback, queue management, subscriptions, automation, and troubleshooting — **translated into 9 languages**. A Discourse forum at https://forum.antennapod.org/ hosts 700+ users and 350+ annual topics. Blog at https://antennapod.org/blog/ provides development updates.

**What's missing:** Despite the excellent architecture blog post, there's no living architecture document explaining what each of the 31 modules does. Build instructions are minimal ("build just like any other Android project"). No published Javadoc or API documentation.

**Why it ranks #2:** The architecture evolution documentation is unmatched — understanding *how and why* a codebase was restructured teaches more about architecture than any static diagram. However, the Java/XML stack is dated for someone starting a new app in 2026.

---

## 3. Podcini.X — modern fork with the most detailed README

**Repository:** https://github.com/XilinJia/Podcini.X  
**Stars:** 74 | **License:** GPL-3.0 | **Last release:** v10.9.3 (Feb 13, 2026)

Podcini.X is the actively maintained successor to Podcini, itself a fork of AntennaPod that rewrote the entire codebase in **modern Kotlin with Jetpack Compose**. It demonstrates how to take AntennaPod's battle-tested podcast logic and rebuild it with a contemporary stack. The project ships very frequent releases and is available on F-Droid.

**Tech stack:** Pure Kotlin, Jetpack Compose UI, Media3 with AudioOffloadMode, **Realm DB** (krdb fork, notably different from Room), OkHttp, Jsoup. Features multiple queues, synthetic podcasts, 5-level rating system, 12-level play states, personal notes, auto-backup, instant WiFi sync, TTS for plain RSS, and NextCloud sync.

**Documentation available:** The README is exceptionally detailed for a project of its size — it covers every feature with explanations, getting started guides, import/export instructions, feed settings, and queue management. Supporting docs include CONTRIBUTING.md, changelog.md, AndroidAuto.md, Licenses_and_permissions.md, and a migration guide (migrationTo6.md). GitHub Discussions are enabled.

**Limitations:** No architecture documentation, no wiki, no separate docs site. Developer onboarding beyond "read the code" is limited. The project is maintained by a single developer, which creates bus-factor risk.

---

## 4. Podverse — cross-platform with a full-stack ecosystem

**Repository (mobile):** https://github.com/podverse/podverse-rn (284 stars)  
**Repository (API):** https://github.com/podverse/podverse-api  
**Repository (ops/docs):** https://github.com/podverse/podverse-ops  
**Website:** https://podverse.fm | **License:** AGPL-3.0

Podverse is the only project that provides a **complete open-source podcast ecosystem**: mobile apps, web player, REST API, and backend infrastructure, spread across **53+ repositories**. It's built with React Native rather than native Android, making it less directly applicable for a Kotlin/Android build, but the API architecture and backend design are highly instructive.

**Tech stack:** React Native (TypeScript) for mobile, React + Next.js for web, Node.js API, PostgreSQL. Supports Podcasting 2.0 features including chapters, transcripts, cross-app comments, Value for Value (boostagrams), livestreams, and video podcasts.

**Developer documentation:** The `podverse-ops` repo contains the most comprehensive setup docs with Docker deployment scripts, database setup, and feed parsing instructions. Contributing guides exist in multiple repos with git flow process and environment setup instructions. A Discord #dev channel serves as the contributor communication hub. The team has acknowledged (GitHub issue #811) that onboarding documentation needs improvement.

**Best for:** Someone interested in building the full podcast stack (not just the Android client) or wanting to understand API design for podcast apps.

---

## 5. Tsacdop — the only project with an architecture diagram in the README

**Repository:** https://github.com/tsacdop/tsacdop (510 stars)  
**License:** GPL-3.0 | **Status:** Inactive since 2021

Despite being unmaintained, Tsacdop earns mention because its README contains something almost no other podcast app provides: **a visual project structure diagram showing the relationship between UI layers, state management, and service layers**. The architecture section maps folder structure to responsibilities — `src/state/` handles audio, download, group, refresh, and settings state; `src/service/` handles API, gpodder, and OPML services. Build instructions include API key setup for ListenNotes and PodcastIndex.

**Tech stack:** Flutter/Dart. An active fork by lojcs (https://github.com/lojcs/tsacdop) continues development with the latest F-Droid release in November 2025.

**Best for:** Understanding state management architecture in a podcast app, even if the Flutter stack isn't your target.

---

## Other projects worth noting briefly

**Escapepod** (https://github.com/y20k/escapepod, 123 stars) — A beautifully minimalist Kotlin podcast player archived in February 2025. Its FAQ-style README is a model of clear user documentation. MIT-licensed, making it the most permissively licensed option. Useful for studying a simple, focused implementation.

**FocusPodcast** (https://github.com/allentown521/FocusPodcast, 98 stars) — Java/Kotlin hybrid with Material Design 3, Android Auto, Chromecast, and audio effects support. Multi-module structure but sparse developer docs.

**Anytime Podcast Player** (https://github.com/amugofjava/anytime_podcast_player) — Flutter-based with Podcasting 2.0 support (chapters, transcripts, funding links). Clear build instructions with PodcastIndex API key setup. BSD-3-Clause licensed.

---

## How these projects compare at a glance

| App | Stars | Language | UI Framework | Database | Active | User Docs | Dev Docs |
|---|---|---|---|---|---|---|---|
| **Pocket Casts** | 2,800 | Kotlin | Compose + XML | Room | ✅ Very active | ★★★★ | ★★★★ |
| **AntennaPod** | 7,600 | Java | XML | Custom SQLite | ✅ Very active | ★★★★★ | ★★★½ |
| **Podcini.X** | 74 | Kotlin | Compose | Realm | ✅ Active | ★★★½ | ★★ |
| **Podverse** | 284 | TypeScript | React Native | PostgreSQL | ✅ Active | ★★★ | ★★★ |
| **Tsacdop** | 510 | Dart | Flutter | — | ❌ Inactive | ★★★ | ★★½ |
| **Escapepod** | 123 | Kotlin | XML | Room | ❌ Archived | ★★★ | ★½ |

---

## Practical recommendations for building your own app

The ideal study path combines insights from multiple projects. **Start with Pocket Casts** to understand modern multi-module architecture with Kotlin, Compose, Hilt, and Media3 — this is the stack you should target. Study its module boundaries, build variants, and CI/CD setup. Then **read AntennaPod's architecture blog post** to understand why modularization matters and how to avoid the coupling pitfalls that a podcast app naturally creates between its database, playback, sync, and UI layers.

For podcast-specific technical decisions, three areas deserve particular attention. **Media playback** is the hardest part: every project uses Media3/ExoPlayer, and Pocket Casts's player module shows how to handle background playback, casting, Wear OS, and Android Auto. **Feed parsing** is surprisingly complex — AntennaPod's custom RSS/Atom parser with Jsoup handles the many non-standard feeds in the wild, while Podverse's API approach offloads parsing to a server. **Offline sync** between devices is the third challenge: AntennaPod integrates with gPodder.net, Podcini.X offers WiFi sync and NextCloud, and Pocket Casts uses its own server infrastructure.

No single project has truly comprehensive developer documentation. The most productive approach is to clone both Pocket Casts and AntennaPod, build them locally, and read the code with the available documentation as your guide. The module structure in Pocket Casts and the modularization history in AntennaPod together tell the full story of how to architect a podcast app well.