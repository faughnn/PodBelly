# Podbelly

An ad-free, open-source podcast player for Android.


## Features

- **Podcast Discovery** — Search iTunes for podcasts or subscribe via RSS feed URL
- **Continue Listening** — Pick up where you left off with in-progress episodes
- **Library Management** — Grid/list view, sort by name, recently added, episode count, most recent, or most listened
- **Full-Featured Player** — Variable speed (0.5x-3.0x), skip silence, volume boost, chapter navigation
- **Sleep Timer** — Preset durations or end-of-episode mode
- **Download-First Playback** — Episodes download before playing, with background download manager
- **Queue** — Drag-to-reorder playback queue
- **Themes** — System, Light, Dark, OLED Dark, High Contrast
- **Background Refresh** — Periodic feed updates with new episode notifications
- **Playback Statistics** — Total listening time, time saved by speed/silence, most listened podcasts
- **OPML Import/Export** — Bring your subscriptions from another app or back them up
- **Auto-Rewind on Resume** — Automatically rewinds based on pause duration

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Multi-module (app, core/\*, feature/\*)
- **DI:** Hilt
- **Database:** Room
- **Playback:** Media3 (ExoPlayer + MediaSession)
- **Networking:** Retrofit + OkHttp
- **Preferences:** DataStore
- **Image Loading:** Coil
- **Testing:** JUnit, MockK, Turbine, Paparazzi
- **CI/CD:** GitHub Actions + Firebase App Distribution

## Project Structure

```
app/                    Main app module, navigation, theme
core/
  common/               Preferences, download manager, shared enums
  database/             Room entities, DAOs, migrations
  network/              Retrofit API, RSS parser, iTunes search
  playback/             Media3 controller, playback service, sleep timer
feature/
  home/                 Recent episodes feed, continue listening
  discover/             Podcast search and discovery
  podcast/              Podcast detail, episode detail, library
  player/               Full-screen player with chapters
  queue/                Playback queue management
  settings/             App settings, playback stats
```

## Build

Requires JDK 17 and Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew testDebugUnitTest
```

## License

All rights reserved.
