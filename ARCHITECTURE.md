# Podbelly Architecture

## Overview

Podbelly is an ad-free, privacy-focused podcast player for Android. It is built entirely
with Kotlin and Jetpack Compose, targets API 26+ (Android 8.0), and follows modern Android
architecture conventions: multi-module Gradle project, MVVM with unidirectional data flow,
Hilt dependency injection, and Media3 for playback.

---

## Module Structure

The project is organized into three module layers: **app**, **core**, and **feature**.

| Module | Purpose |
|---|---|
| `app` | Application entry point, Hilt setup, Compose theme, navigation host, WorkManager workers |
| `core:database` | Room database, entities (`PodcastEntity`, `EpisodeEntity`, `QueueItemEntity`), DAOs |
| `core:network` | Retrofit iTunes Search API client, OkHttp-based RSS feed fetcher, RSS XML parser, OPML import/export |
| `core:playback` | Media3 `MediaSessionService`, `PlaybackController` singleton, `PlaybackState` data class, sleep timer |
| `core:common` | Shared utilities, user preferences (DataStore), date/duration formatting helpers |
| `feature:home` | Home screen showing recent and in-progress episodes |
| `feature:discover` | Podcast search and discovery powered by the iTunes Search API |
| `feature:podcast` | Subscribed podcast library grid, podcast detail screen with episode list |
| `feature:player` | Full-screen now-playing UI with seek bar, speed controls, skip-silence, and volume boost |
| `feature:queue` | Queue management screen with drag-to-reorder support |
| `feature:settings` | App settings, OPML import/export, download storage management |

---

## Module Dependency Graph

```
                          app
                       /  |  \  \
                      /   |   \  \-----------\
                     v    v    v              v
              feature:*  ...  feature:*    core:playback
                  |                           |
                  +------ core:database ------+
                  |                           |
                  +------ core:network -------+
                  |
                  +------ core:common --------+
```

Key rules:

- **`app`** depends on every `feature:*` and `core:*` module.
- **`feature:*`** modules depend on `core:database`, `core:network`, `core:common`, and
  occasionally `core:playback` (e.g., `feature:player`). Feature modules never depend on
  each other.
- **`core:*`** modules depend only on `core:common` (and on each other where necessary,
  e.g., `core:playback` depends on `core:database` for `QueueDao`).

---

## Key Patterns

### MVVM with Jetpack ViewModels

Each feature screen has a corresponding `@HiltViewModel` ViewModel that holds UI state
as a `StateFlow` and exposes action methods. Compose screens collect the state flow via
`collectAsState()` and call ViewModel methods in response to user interaction.

### Reactive Data Flow with Kotlin Flow

Room DAOs return `Flow<List<T>>` for observable queries. ViewModels transform and combine
these flows (using `map`, `combine`, `flatMapLatest`) and expose a single `StateFlow` of
screen-level UI state.

### Dependency Injection with Hilt

Hilt is the sole DI framework. Modules provide singletons for the Room database, Retrofit
service, OkHttpClient, RssParser, and PlaybackController. ViewModels receive repositories
and DAOs through constructor injection.

### Repository Pattern

`PodcastSearchRepository` in `core:network` abstracts iTunes API calls and RSS feed
fetching behind a clean suspend-function API. Database DAOs serve as the repository layer
for local data, queried directly by ViewModels.

### Room for Local Persistence

The Room database stores podcasts, episodes, and queue items. `PodcastEntity` and
`EpisodeEntity` are linked by a foreign key (`podcastId`) with cascade delete. Episodes
track playback position, download path, and played state to support resume and offline
playback.

### WorkManager for Background Feed Refresh

A periodic `Worker` (scheduled via WorkManager) refreshes subscribed podcast feeds in the
background, fetching new episodes and inserting them into the Room database. This runs
even when the app is not in the foreground.

---

## Tech Stack

| Library | Purpose |
|---|---|
| **Jetpack Compose + Material 3** | Declarative UI with Material You theming |
| **Navigation Compose** | Type-safe single-Activity navigation with bottom nav |
| **Hilt** | Compile-time dependency injection |
| **Room** | SQLite persistence with Flow-based reactive queries |
| **Media3 (ExoPlayer)** | Audio playback engine with `MediaSessionService` |
| **Media3 Session** | `MediaController`/`MediaSession` for system media integration |
| **Retrofit + OkHttp** | HTTP client for iTunes Search API |
| **Coil** | Async image loading for podcast artwork |
| **Kotlin Coroutines + Flow** | Asynchronous programming and reactive streams |
| **WorkManager** | Reliable background feed refresh scheduling |
| **KSP** | Annotation processing for Hilt and Room |

---

## Data Flow

Data flows through the app in a single direction:

```
Network (iTunes API / RSS)
        |
        v
   Repository / RssParser
        |
        v
   Room Database (single source of truth)
        |
        v
   DAO (returns Flow<T>)
        |
        v
   ViewModel (transforms into UI StateFlow)
        |
        v
   Compose UI (collectAsState, renders)
        |
        v
   User Action --> ViewModel method --> Repository / DAO / PlaybackController
```

1. **Discover flow**: User searches -> `ItunesSearchApi` returns results -> user
   subscribes -> `PodcastSearchRepository.fetchFeed()` fetches and parses the RSS XML
   -> podcast and episodes are inserted into Room.

2. **Refresh flow**: WorkManager triggers periodic refresh -> for each subscribed podcast,
   `fetchFeed()` is called -> new episodes are inserted, existing ones are updated.

3. **Playback flow**: User taps an episode -> ViewModel calls
   `PlaybackController.play()` -> controller builds a `MediaItem` with metadata and
   sends it to the `MediaController` -> `PlaybackService` receives it via the
   `MediaSession` and starts ExoPlayer -> `Player.Listener` callbacks update
   `PlaybackState` -> Compose UI reacts to the new state.

---

## Navigation

Podbelly uses a single-Activity architecture with Jetpack Navigation Compose.

### Routes

All routes are defined as a sealed class in `Screen.kt`:

| Route | Description |
|---|---|
| `home` | Home screen (start destination) |
| `discover` | Search and discover podcasts |
| `library` | Subscribed podcast grid |
| `queue` | Playback queue |
| `settings` | App settings |
| `podcast/{podcastId}` | Podcast detail (parameterized) |
| `player` | Full-screen now-playing |

### Bottom Navigation

Five tabs are shown in a `NavigationBar`: Home, Discover, Library, Queue, and Settings.
The bottom bar uses `saveState` and `restoreState` so each tab remembers its scroll
position and back stack independently. The bar is hidden on the full-screen player route.

### Mini Player

When an episode is loaded (i.e., `playbackState.episodeId != 0`), an animated mini player
appears above the bottom navigation bar. It displays episode artwork, title, podcast name,
a progress indicator, and a play/pause toggle. Tapping the mini player navigates to the
full-screen player route.

---

## Playback Architecture

Playback is built on three cooperating components:

```
Compose UI (NowPlayingScreen, MiniPlayer)
        |  observes StateFlow<PlaybackState>
        v
  PlaybackController (@Singleton)
        |  sends commands via MediaController
        v
  PlaybackService (MediaSessionService)
        |  owns ExoPlayer, MediaSession
        v
  Android system (notification, lock screen, Bluetooth, audio focus)
```

### PlaybackService

A `MediaSessionService` that runs in the foreground with a persistent notification.
It creates an `ExoPlayer` configured for speech audio (`AUDIO_CONTENT_TYPE_SPEECH`),
automatic audio focus management, and handling of audio-becoming-noisy events (e.g.,
headphone disconnect). It exposes two custom session commands:

- `SET_SKIP_SILENCE` -- enables ExoPlayer's silence-skipping mode.
- `SET_VOLUME_BOOST` -- increases player volume to 1.5x for quiet podcasts.

### PlaybackController

A Hilt `@Singleton` that bridges the UI layer and the service. It:

- Connects to `PlaybackService` via a `MediaController` (built from a `SessionToken`).
- Exposes a `StateFlow<PlaybackState>` that the UI observes. This state includes episode
  metadata, play/pause status, position, duration, speed, and queue flags.
- Provides control methods: `play()`, `pause()`, `resume()`, `seekTo()`, `skipForward()`,
  `skipBack()`, `setPlaybackSpeed()`, `setSkipSilence()`, `setVolumeBoost()`, and `stop()`.
- Runs a coroutine-based position-update loop (every 250ms) while playback is active.
- Manages queue advancement: when an episode ends (`Player.STATE_ENDED`), it removes the
  finished episode from the queue via `QueueDao` and starts the next one automatically.

### PlaybackState

An immutable data class that fully describes the current playback status. ViewModels and
Compose screens never talk to the `MediaController` directly -- they read `PlaybackState`
and call methods on `PlaybackController`.

### Lifecycle

`MainActivity.onCreate()` calls `playbackController.connectToService(this)`, which
asynchronously builds the `MediaController`. Because `PlaybackController` is a singleton
and the service runs independently, playback continues across configuration changes and
even after the Activity is destroyed (e.g., when the user swipes the app away, the service
stops only if nothing is playing).
