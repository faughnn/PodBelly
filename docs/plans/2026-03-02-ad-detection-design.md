# On-Device Ad Detection

## Overview

Fully on-device podcast ad detection pipeline that identifies ad segments in downloaded episodes and stores timestamped results locally. Users can view ad markers on the seekbar and optionally auto-skip detected ads. Off by default — entirely opt-in.

## Design Decisions

- **Models bundled in APK** (~140 MB total) — no separate download flow
- **Off by default** — user enables in settings with granular constraints (Wi-Fi only, charging only, auto-on-download, manual per-episode)
- **UX**: Seekbar ad markers + auto-skip with per-podcast toggle
- **KMP-ready**: Android-only implementation behind `expect/actual`-style interfaces for future iOS port
- **Full signal processing from v1**: Hysteresis, gap merging, boundary snapping from the start

## Module Structure

One new core module: `core:addetection`

Dependencies: `core:database`, `core:playback`, `core:common`

Native libraries (whisper.cpp, Chromaprint, libebur128, Silero VAD) compile via NDK CMake and ship as `.so` files within this module.

## Data Model

### AdSegment

One row per detected ad region in an episode.

| Column | Type | Notes |
|---|---|---|
| id | Long | Auto PK |
| episodeId | Long | FK → Episode |
| startMs | Long | Ad start timestamp |
| endMs | Long | Ad end timestamp |
| confidence | Float | 0.0–1.0 |
| source | String | "regex", "classifier", "fingerprint", "manual" |
| dismissed | Boolean | User marked as "not an ad" |

### AdDetectionJob

Tracks processing state per episode.

| Column | Type | Notes |
|---|---|---|
| id | Long | Auto PK |
| episodeId | Long | FK → Episode |
| status | String | "queued", "processing", "completed", "failed" |
| startedAt | Long? | |
| completedAt | Long? | |
| transcriptPath | String? | Path to cached transcript JSON |

### AdFingerprint

Known ad audio fingerprints that grow over time.

| Column | Type | Notes |
|---|---|---|
| id | Long | Auto PK |
| fingerprint | ByteArray | Chromaprint hash |
| durationMs | Long | |
| label | String? | Optional brand/sponsor name |
| firstSeenEpisodeId | Long | |

### Settings Preferences

- `adDetectionEnabled: Boolean` (default: false)
- `adDetectionAutoOnDownload: Boolean` (default: false)
- `adDetectionRequiresWifi: Boolean` (default: true)
- `adDetectionRequiresCharging: Boolean` (default: false)
- `adAutoSkipEnabled: Boolean` (default: false)

## KMP-Ready Interfaces

```kotlin
interface TranscriptionEngine {
    suspend fun transcribe(audioPath: String, onProgress: (Float) -> Unit): TranscriptResult
}

interface TextClassifier {
    fun classify(segment: TranscriptSegment): AdProbability
}

interface AudioFingerprinter {
    fun fingerprint(audioPath: String, windowMs: Long): List<FingerprintWindow>
    fun match(fingerprint: ByteArray, database: FingerprintDatabase): MatchResult?
}
```

Android implementations back these with JNI/TFLite. iOS implementations slot in behind the same interfaces later.

## Pipeline Stages

### Stage 0 — Fingerprint matching (~10 seconds)

Generate Chromaprint fingerprints in sliding 30-second windows with 15-second overlap. Compare against local `AdFingerprint` database. Matches above 0.8 similarity are marked as ads immediately and skip transcription.

### Stage 1 — Audio preprocessing (~5 seconds)

Decode episode to 16kHz mono PCM via ffmpeg-kit. Run `silencedetect` (threshold -50dB, min 0.5s). Measure per-window LUFS via libebur128 (5-second windows). Cache silence map and loudness profile alongside transcript.

### Stage 2 — VAD-filtered transcription (20–45 minutes)

Silero VAD (ONNX, ~2 MB) identifies speech regions, skipping music/silence — reduces workload by ~20–40%. whisper.cpp base.en Q5_0 transcribes speech in 30-second windows. Output: `List<TranscriptSegment(startMs, endMs, text)>`. Runs via WorkManager with user-configured constraints.

Memory management: check `ActivityManager.getMemoryClass()` before starting. Below 512 MB available heap → fall back to tiny.en model. One episode at a time (sequential, not parallel).

Transcripts cached as JSON in `files/transcripts/{episodeId}.json` for re-classification without re-transcription.

### Stage 3 — Text classification (~2 seconds)

**Tier 1 — Regex patterns** (zero compute cost):
- High confidence (0.9+): "sponsored by", "brought to you by", "use code \w+", "promo code", vanity URL pattern `\w+\.(com|co|io|org)/\w+`, "and now back to"
- Medium confidence (0.7): "download the app", "free trial", "I want to tell you about"

High-confidence regex matches skip MobileBERT.

**Tier 2 — MobileBERT** (TFLite INT8, ~25 MB):
- Fine-tuned from `morenolq/spotify-podcast-advertising-classification` (92% baseline)
- Input: 128-token text window + relative episode position (float 0.0–1.0)
- Output: ad probability 0.0–1.0
- ~62 ms per segment, ~2 seconds for a full episode (~120 segments)

**Score combination**: `finalScore = max(regexScore, classifierScore) * positionBoost` where positionBoost is 1.0–1.15 for segments near 0%, 33%, 50%, 67%, 100% of episode duration.

### Stage 4 — Signal processing (~1 second)

1. **Kernel smoothing**: Gaussian weighted moving average (window of 5 segments) over raw scores
2. **Hysteresis thresholding**: Enter ad state at score > 0.6. Exit requires 3 consecutive segments below 0.4.
3. **Gap merging**: Ad regions separated by < 8 seconds merge into one. Regions > 8 seconds apart stay separate.
4. **Boundary snapping**: Snap to nearest silence gap within ±3 seconds. Fallback to LUFS discontinuity. Fallback to raw transcript segment edge.

Output: `AdSegment` rows written to Room.

## Playback Integration

### Seekbar markers

`AdSegmentOverlay` composable renders semi-transparent colored bars on the seekbar at ad positions. Loaded reactively via `Flow<List<AdSegment>>` keyed on current episodeId.

### Auto-skip

In `PlaybackController`'s existing 250ms position-update loop:
1. Check if `adAutoSkipEnabled` (global or per-podcast override)
2. If `currentPositionMs` falls within a non-dismissed `AdSegment`, seek to `endMs`
3. Show snackbar "Skipped ad" with 5-second "Undo" action
4. Undo seeks back to `startMs` and sets `dismissed = true`

Edge case: suppress auto-skip if user manually sought into the ad region (track via `lastUserSeekMs` timestamp, suppress within 2 seconds of user seek).

### Per-podcast override

New nullable `adAutoSkip: Boolean?` column on Podcast entity. `null` = follow global, `true`/`false` = override. Accessible from podcast detail overflow menu.

### Episode detail screen

- No detection run → "Detect ads" button (enqueues WorkManager job)
- Processing → progress bar with percentage
- Completed → "X ad segments detected (Ym Zs total)" summary
- Option to clear results and re-run

## Audio Fingerprinting

Chromaprint compiled via NDK with KissFFT (MIT license). Fingerprints (~2.5 KB each) generated for every confirmed ad segment after classification. Database grows organically — ads detected in one podcast are caught instantly in others.

Matching runs at pipeline Stage 0, before transcription. 240 windows against 1,000 fingerprints takes < 2 seconds.

Storage: ~500 fingerprints over 6 months of heavy use = ~1.25 MB.

## User Feedback and Manual Marking

Long-press seekbar opens region selector with draggable start/end handles. Manual marks create `AdSegment` rows with `source = "manual"` and get fingerprinted for the database. Dismissing a segment sets `dismissed = true`.

## Settings UI

New "Ad Detection" section in settings:
- Master toggle (off by default)
- Auto-detect on download toggle
- Wi-Fi only / Charging only constraint toggles
- Auto-skip toggle (global default)
- "Clear all ad data" action
- Storage usage display

## Battery and Resource Management

- Foreground notification during processing with progress
- Pause if battery drops below 15% (even without charging constraint)
- One concurrent detection job maximum
- Sequential episode processing via WorkManager chaining

## Phased Implementation

- **Phase 1**: Core infrastructure — `core:addetection` module, Room schema, settings UI, interfaces
- **Phase 2**: Transcription — whisper.cpp NDK/JNI, WorkManager job, VAD, memory management
- **Phase 3**: Classification — regex patterns, MobileBERT TFLite, score combination
- **Phase 4**: Signal processing — smoothing, hysteresis, gap merging, boundary snapping
- **Phase 5**: Playback integration — seekbar markers, auto-skip, per-podcast toggle, episode detail UI
- **Phase 6**: Fingerprinting — Chromaprint NDK, fingerprint DB, cross-episode matching, Stage 0 integration
- **Phase 7**: Polish — manual marking, settings UI, battery optimization, accuracy tuning

## Expected Accuracy

- DAI pre-recorded ads: ~95%+
- Host-read with standard phrases: ~80%
- Seamless host-read ads: ~50–60%
- Repeated ads (after first encounter): ~97%+
- Overall: 85–90% recall, 90–95% precision

## Bill of Materials

| Component | Library | Size | License |
|---|---|---|---|
| Audio decode | FFmpeg (ffmpeg-kit) | ~15 MB | LGPL 2.1+ |
| Loudness | libebur128 | < 1 MB | MIT |
| Fingerprinting | Chromaprint (KissFFT) | ~2 MB | MIT |
| VAD | Silero VAD (ONNX) | ~2 MB | MIT |
| Transcription | whisper.cpp base.en Q5_0 | ~90 MB | MIT |
| Classification | MobileBERT (TFLite INT8) | ~25 MB | Apache 2.0 |
| ML inference | TensorFlow Lite (LiteRT) | ~5 MB | Apache 2.0 |
| **Total** | | **~140 MB** | **All permissive** |
