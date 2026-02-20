# Podbelly

An ad-free, open-source podcast player for Android.

## Features

- **Podcast Discovery** — Search iTunes for podcasts, subscribe via RSS feed URL
- **Library Management** — Grid/list view toggle, sort by name, recently added, episode count, most recent episode, or most listened
- **Playback Controls** — Variable speed (0.5x–3.0x), skip silence, volume boost, sleep timer, chapter navigation
- **Download Manager** — Background episode downloads with progress tracking, error logging, and retry
- **Queue** — Drag-to-reorder playback queue
- **Themes** — System default, Light, Dark, OLED Dark (pure black), High Contrast
- **Background Refresh** — Periodic feed updates via WorkManager with new episode notifications
- **Playback Statistics** — Total listening time, time saved by speed/silence, most listened podcasts and episodes
- **Auto-Rewind on Resume** — Automatically rewinds based on pause duration (3s–20s)
- **Share** — Share episode links from the detail screen

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Multi-module (app, core/*, feature/*)
- **DI:** Hilt
- **Database:** Room
- **Playback:** Media3 (ExoPlayer + MediaSession)
- **Networking:** Retrofit + OkHttp
- **Preferences:** DataStore
- **Image Loading:** Coil
- **Testing:** JUnit, MockK, Turbine, Paparazzi

## Project Structure

```
app/                    Main app module, navigation, theme
core/
  common/               Preferences, download manager, shared enums
  database/             Room entities, DAOs, migrations
  network/              Retrofit API, RSS parser, iTunes search
  playback/             Media3 controller, playback service, sleep timer
feature/
  home/                 Recent episodes feed
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
