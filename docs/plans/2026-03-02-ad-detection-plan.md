# Ad Detection Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add fully on-device podcast ad detection that marks ads on the seekbar and optionally auto-skips them.

**Architecture:** A 5-stage pipeline (fingerprint match → audio preprocess → transcribe → classify → smooth boundaries) runs as a background WorkManager job on downloaded episodes. Results stored as `AdSegment` rows in Room. Playback integration adds seekbar markers and auto-skip in the existing 250ms position-update loop.

**Tech Stack:** whisper.cpp (NDK/JNI), MobileBERT (TFLite), Chromaprint (NDK/JNI), libebur128 (NDK/JNI), Silero VAD (ONNX Runtime), WorkManager, Room, Jetpack Compose

---

## Phase 1: Core Infrastructure

### Task 1: Create `core:addetection` module skeleton

**Files:**
- Create: `core/addetection/build.gradle.kts`
- Create: `core/addetection/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts` — add `include(":core:addetection")`

**Step 1: Create module directory structure**

```bash
mkdir -p core/addetection/src/main/java/com/podbelly/core/addetection
mkdir -p core/addetection/src/test/java/com/podbelly/core/addetection
```

**Step 2: Write `core/addetection/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.podbelly.core.addetection"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:common"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
```

**Step 3: Write `core/addetection/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

**Step 4: Add module to `settings.gradle.kts`**

Add `include(":core:addetection")` after the existing `include(":core:common")` line.

**Step 5: Add module dependency to `app/build.gradle.kts`**

Add `implementation(project(":core:addetection"))` in the dependencies block after the other core modules.

**Step 6: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add core/addetection/ settings.gradle.kts app/build.gradle.kts
git commit -m "feat: add core:addetection module skeleton"
```

---

### Task 2: Room entities — AdSegment, AdDetectionJob, AdFingerprint

**Files:**
- Create: `core/database/src/main/java/com/podbelly/core/database/entity/AdSegmentEntity.kt`
- Create: `core/database/src/main/java/com/podbelly/core/database/entity/AdDetectionJobEntity.kt`
- Create: `core/database/src/main/java/com/podbelly/core/database/entity/AdFingerprintEntity.kt`

**Step 1: Write `AdSegmentEntity.kt`**

```kotlin
package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ad_segments",
    indices = [
        Index(value = ["episodeId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AdSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "episodeId")
    val episodeId: Long,

    @ColumnInfo(name = "startMs")
    val startMs: Long,

    @ColumnInfo(name = "endMs")
    val endMs: Long,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "dismissed")
    val dismissed: Boolean = false,
)
```

**Step 2: Write `AdDetectionJobEntity.kt`**

```kotlin
package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ad_detection_jobs",
    indices = [
        Index(value = ["episodeId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AdDetectionJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "episodeId")
    val episodeId: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "startedAt")
    val startedAt: Long? = null,

    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,

    @ColumnInfo(name = "transcriptPath")
    val transcriptPath: String? = null,
)
```

**Step 3: Write `AdFingerprintEntity.kt`**

```kotlin
package com.podbelly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ad_fingerprints")
data class AdFingerprintEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: ByteArray,

    @ColumnInfo(name = "durationMs")
    val durationMs: Long,

    @ColumnInfo(name = "label")
    val label: String? = null,

    @ColumnInfo(name = "firstSeenEpisodeId")
    val firstSeenEpisodeId: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdFingerprintEntity) return false
        return id == other.id && fingerprint.contentEquals(other.fingerprint) &&
            durationMs == other.durationMs && label == other.label &&
            firstSeenEpisodeId == other.firstSeenEpisodeId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fingerprint.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + firstSeenEpisodeId.hashCode()
        return result
    }
}
```

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:database:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/entity/Ad*.kt
git commit -m "feat: add Room entities for ad segments, detection jobs, and fingerprints"
```

---

### Task 3: DAOs for ad detection tables

**Files:**
- Create: `core/database/src/main/java/com/podbelly/core/database/dao/AdSegmentDao.kt`
- Create: `core/database/src/main/java/com/podbelly/core/database/dao/AdDetectionJobDao.kt`
- Create: `core/database/src/main/java/com/podbelly/core/database/dao/AdFingerprintDao.kt`

**Step 1: Write `AdSegmentDao.kt`**

```kotlin
package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.podbelly.core.database.entity.AdSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdSegmentDao {

    @Query("SELECT * FROM ad_segments WHERE episodeId = :episodeId AND dismissed = 0 ORDER BY startMs ASC")
    fun getByEpisodeId(episodeId: Long): Flow<List<AdSegmentEntity>>

    @Query("SELECT * FROM ad_segments WHERE episodeId = :episodeId ORDER BY startMs ASC")
    fun getAllByEpisodeId(episodeId: Long): Flow<List<AdSegmentEntity>>

    @Query("SELECT * FROM ad_segments WHERE episodeId = :episodeId AND dismissed = 0 ORDER BY startMs ASC")
    suspend fun getActiveByEpisodeIdOnce(episodeId: Long): List<AdSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<AdSegmentEntity>)

    @Query("UPDATE ad_segments SET dismissed = :dismissed WHERE id = :id")
    suspend fun setDismissed(id: Long, dismissed: Boolean)

    @Query("DELETE FROM ad_segments WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @Query("SELECT SUM(endMs - startMs) FROM ad_segments WHERE episodeId = :episodeId AND dismissed = 0")
    suspend fun getTotalAdDurationMs(episodeId: Long): Long?

    @Query("SELECT COUNT(*) FROM ad_segments WHERE episodeId = :episodeId AND dismissed = 0")
    suspend fun getActiveCount(episodeId: Long): Int
}
```

**Step 2: Write `AdDetectionJobDao.kt`**

```kotlin
package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.podbelly.core.database.entity.AdDetectionJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdDetectionJobDao {

    @Query("SELECT * FROM ad_detection_jobs WHERE episodeId = :episodeId LIMIT 1")
    fun getByEpisodeId(episodeId: Long): Flow<AdDetectionJobEntity?>

    @Query("SELECT * FROM ad_detection_jobs WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getByEpisodeIdOnce(episodeId: Long): AdDetectionJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: AdDetectionJobEntity)

    @Query("UPDATE ad_detection_jobs SET status = :status, completedAt = :completedAt WHERE episodeId = :episodeId")
    suspend fun updateStatus(episodeId: Long, status: String, completedAt: Long? = null)

    @Query("DELETE FROM ad_detection_jobs WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)
}
```

**Step 3: Write `AdFingerprintDao.kt`**

```kotlin
package com.podbelly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.podbelly.core.database.entity.AdFingerprintEntity

@Dao
interface AdFingerprintDao {

    @Query("SELECT * FROM ad_fingerprints")
    suspend fun getAll(): List<AdFingerprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fingerprint: AdFingerprintEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fingerprints: List<AdFingerprintEntity>)

    @Query("DELETE FROM ad_fingerprints WHERE firstSeenEpisodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @Query("SELECT COUNT(*) FROM ad_fingerprints")
    suspend fun getCount(): Int

    @Query("DELETE FROM ad_fingerprints")
    suspend fun deleteAll()
}
```

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:database:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/dao/Ad*.kt
git commit -m "feat: add DAOs for ad segments, detection jobs, and fingerprints"
```

---

### Task 4: Database migration (version 3 → 4)

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/PodbellDatabase.kt`
- Modify: `core/database/src/main/java/com/podbelly/core/database/di/DatabaseModule.kt`

**Step 1: Add entities and DAOs to PodbellDatabase**

In `PodbellDatabase.kt`, add the three new entities to the `@Database` annotation, add abstract DAO methods, bump version to 4, and add MIGRATION_3_4:

```kotlin
// Add to @Database entities list:
//   AdSegmentEntity::class,
//   AdDetectionJobEntity::class,
//   AdFingerprintEntity::class,
// Change: version = 4

// Add abstract DAO methods:
//   abstract fun adSegmentDao(): AdSegmentDao
//   abstract fun adDetectionJobDao(): AdDetectionJobDao
//   abstract fun adFingerprintDao(): AdFingerprintDao

// Add migration in companion object:
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ad_segments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                episodeId INTEGER NOT NULL,
                startMs INTEGER NOT NULL,
                endMs INTEGER NOT NULL,
                confidence REAL NOT NULL,
                source TEXT NOT NULL,
                dismissed INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (episodeId) REFERENCES episodes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_ad_segments_episodeId ON ad_segments(episodeId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ad_detection_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                episodeId INTEGER NOT NULL,
                status TEXT NOT NULL,
                startedAt INTEGER,
                completedAt INTEGER,
                transcriptPath TEXT,
                FOREIGN KEY (episodeId) REFERENCES episodes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_ad_detection_jobs_episodeId ON ad_detection_jobs(episodeId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ad_fingerprints (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                fingerprint BLOB NOT NULL,
                durationMs INTEGER NOT NULL,
                label TEXT,
                firstSeenEpisodeId INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
```

**Step 2: Register migration in DatabaseModule.kt**

In `DatabaseModule.kt`, add `.addMigrations(..., PodbellDatabase.MIGRATION_3_4)` to the builder chain.

**Step 3: Register new DAOs in DatabaseModule.kt**

Add three new `@Provides @Singleton` methods for `AdSegmentDao`, `AdDetectionJobDao`, and `AdFingerprintDao`, following the existing pattern.

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:database:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/PodbellDatabase.kt
git add core/database/src/main/java/com/podbelly/core/database/di/DatabaseModule.kt
git commit -m "feat: add database migration 3→4 for ad detection tables"
```

---

### Task 5: Ad detection preferences in PreferencesManager

**Files:**
- Modify: `core/common/src/main/java/com/podbelly/core/common/PreferencesManager.kt`

**Step 1: Add preference keys**

Add to the `Keys` object:

```kotlin
val AD_DETECTION_ENABLED = booleanPreferencesKey("ad_detection_enabled")
val AD_DETECTION_AUTO_ON_DOWNLOAD = booleanPreferencesKey("ad_detection_auto_on_download")
val AD_DETECTION_REQUIRES_WIFI = booleanPreferencesKey("ad_detection_requires_wifi")
val AD_DETECTION_REQUIRES_CHARGING = booleanPreferencesKey("ad_detection_requires_charging")
val AD_AUTO_SKIP_ENABLED = booleanPreferencesKey("ad_auto_skip_enabled")
```

**Step 2: Add Flow getters**

```kotlin
val adDetectionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[Keys.AD_DETECTION_ENABLED] ?: false
}

val adDetectionAutoOnDownload: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[Keys.AD_DETECTION_AUTO_ON_DOWNLOAD] ?: false
}

val adDetectionRequiresWifi: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[Keys.AD_DETECTION_REQUIRES_WIFI] ?: true
}

val adDetectionRequiresCharging: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[Keys.AD_DETECTION_REQUIRES_CHARGING] ?: false
}

val adAutoSkipEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[Keys.AD_AUTO_SKIP_ENABLED] ?: false
}
```

**Step 3: Add suspend setters**

```kotlin
suspend fun setAdDetectionEnabled(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[Keys.AD_DETECTION_ENABLED] = enabled }
}

suspend fun setAdDetectionAutoOnDownload(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[Keys.AD_DETECTION_AUTO_ON_DOWNLOAD] = enabled }
}

suspend fun setAdDetectionRequiresWifi(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[Keys.AD_DETECTION_REQUIRES_WIFI] = enabled }
}

suspend fun setAdDetectionRequiresCharging(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[Keys.AD_DETECTION_REQUIRES_CHARGING] = enabled }
}

suspend fun setAdAutoSkipEnabled(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[Keys.AD_AUTO_SKIP_ENABLED] = enabled }
}
```

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:common:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add core/common/src/main/java/com/podbelly/core/common/PreferencesManager.kt
git commit -m "feat: add ad detection preferences to PreferencesManager"
```

---

### Task 6: KMP-ready interfaces

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/TranscriptionEngine.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/TextClassifier.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/AudioFingerprinter.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/model/TranscriptSegment.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/model/AdProbability.kt`

**Step 1: Write data models in `model/` package**

`TranscriptSegment.kt`:
```kotlin
package com.podbelly.core.addetection.model

data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class TranscriptResult(
    val segments: List<TranscriptSegment>,
    val language: String = "en",
)
```

`AdProbability.kt`:
```kotlin
package com.podbelly.core.addetection.model

data class AdProbability(
    val score: Float,
    val source: String,
)
```

**Step 2: Write interfaces**

`TranscriptionEngine.kt`:
```kotlin
package com.podbelly.core.addetection

import com.podbelly.core.addetection.model.TranscriptResult

interface TranscriptionEngine {
    suspend fun transcribe(
        audioPath: String,
        onProgress: (Float) -> Unit = {},
    ): TranscriptResult

    fun release()
}
```

`TextClassifier.kt`:
```kotlin
package com.podbelly.core.addetection

import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptSegment

interface TextClassifier {
    fun classify(segment: TranscriptSegment, relativePosition: Float): AdProbability
    fun release()
}
```

`AudioFingerprinter.kt`:
```kotlin
package com.podbelly.core.addetection

interface AudioFingerprinter {
    fun fingerprint(pcmPath: String, startMs: Long, endMs: Long): ByteArray
    fun match(a: ByteArray, b: ByteArray): Float
    fun release()
}
```

**Step 3: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add KMP-ready interfaces for transcription, classification, and fingerprinting"
```

---

### Task 7: Ad detection settings UI section

**Files:**
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/SettingsViewModel.kt`
- Modify: `feature/settings/build.gradle.kts` — add dependency on `core:addetection` if needed

**Step 1: Add ad detection state to SettingsViewModel**

Add flows for the 5 ad detection preferences, collected into the UI state. Add setter methods that call through to `PreferencesManager`.

**Step 2: Add "Ad Detection" section to SettingsScreen**

Add after the existing "Downloads" section (or wherever appropriate). Use existing `SectionHeader`, `SettingsCard`, `SwitchRow` composables:

```kotlin
item { SectionHeader(title = "Ad Detection") }
item {
    SettingsCard {
        SwitchRow(
            title = "Enable ad detection",
            subtitle = "Analyze downloaded episodes to find ads",
            checked = uiState.adDetectionEnabled,
            onCheckedChange = { viewModel.setAdDetectionEnabled(it) },
        )
        AnimatedVisibility(visible = uiState.adDetectionEnabled) {
            Column {
                SwitchRow(
                    title = "Auto-detect on download",
                    subtitle = "Automatically analyze new downloads",
                    checked = uiState.adDetectionAutoOnDownload,
                    onCheckedChange = { viewModel.setAdDetectionAutoOnDownload(it) },
                )
                SwitchRow(
                    title = "Wi-Fi only",
                    subtitle = "Only run analysis when connected to Wi-Fi",
                    checked = uiState.adDetectionRequiresWifi,
                    onCheckedChange = { viewModel.setAdDetectionRequiresWifi(it) },
                )
                SwitchRow(
                    title = "While charging only",
                    subtitle = "Only run analysis while device is charging",
                    checked = uiState.adDetectionRequiresCharging,
                    onCheckedChange = { viewModel.setAdDetectionRequiresCharging(it) },
                )
                SwitchRow(
                    title = "Auto-skip ads",
                    subtitle = "Automatically skip detected ad segments during playback",
                    checked = uiState.adAutoSkipEnabled,
                    onCheckedChange = { viewModel.setAdAutoSkipEnabled(it) },
                )
            }
        }
    }
}
```

**Step 3: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add feature/settings/src/ feature/settings/build.gradle.kts
git commit -m "feat: add ad detection settings UI section"
```

---

## Phase 2: Transcription Engine

> **Note:** This phase involves significant native (C/C++) integration. Before starting, clone whisper.cpp and Silero VAD ONNX model. The whisper.cpp Android example at `examples/whisper.android/` in the whisper.cpp repo is the primary reference.

### Task 8: Add native build infrastructure to `core:addetection`

**Files:**
- Create: `core/addetection/src/main/cpp/CMakeLists.txt`
- Create: `core/addetection/src/main/cpp/whisper-jni.cpp`
- Modify: `core/addetection/build.gradle.kts` — add NDK/CMake configuration
- Modify: `gradle/libs.versions.toml` — add ONNX Runtime dependency

**Step 1: Update `build.gradle.kts` with NDK configuration**

Add to the `android` block:

```kotlin
android {
    // ... existing config ...

    ndkVersion = "26.1.10909125"

    defaultConfig {
        // ... existing config ...
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

**Step 2: Write initial `CMakeLists.txt`**

This will be expanded as each native library is integrated. Start with whisper.cpp:

```cmake
cmake_minimum_required(VERSION 3.22.1)
project(podbelly-addetection)

# whisper.cpp source will be added as a subdirectory
# See whisper.cpp repo examples/whisper.android for reference CMake setup
# Placeholder — populated when whisper.cpp source is vendored in

add_library(whisper-jni SHARED whisper-jni.cpp)
target_link_libraries(whisper-jni android log)
```

**Step 3: Write placeholder JNI bridge `whisper-jni.cpp`**

```cpp
#include <jni.h>
#include <android/log.h>

#define TAG "WhisperJNI"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_podbelly_core_addetection_engine_WhisperJni_loadModel(
    JNIEnv *env, jobject thiz, jstring model_path) {
    // TODO: Initialize whisper context from model file
    __android_log_print(ANDROID_LOG_INFO, TAG, "loadModel called");
    return 0L;
}

JNIEXPORT void JNICALL
Java_com_podbelly_core_addetection_engine_WhisperJni_release(
    JNIEnv *env, jobject thiz, jlong handle) {
    // TODO: Free whisper context
    __android_log_print(ANDROID_LOG_INFO, TAG, "release called");
}

} // extern "C"
```

**Step 4: Verify native build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL (native .so files produced)

**Step 5: Commit**

```bash
git add core/addetection/src/main/cpp/ core/addetection/build.gradle.kts
git commit -m "feat: add NDK/CMake build infrastructure for native ad detection libs"
```

---

### Task 9: Vendor whisper.cpp and build for Android

**Files:**
- Create: `core/addetection/src/main/cpp/whisper/` — vendored whisper.cpp source
- Modify: `core/addetection/src/main/cpp/CMakeLists.txt` — add whisper.cpp build targets
- Modify: `core/addetection/src/main/cpp/whisper-jni.cpp` — implement load/transcribe/release

> **Reference:** Study `https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android` for the exact CMake setup, JNI bridge, and model loading pattern. Copy and adapt their JNI implementation.

**Step 1: Clone whisper.cpp and copy source files**

Clone the whisper.cpp repo. Copy the core source files (`whisper.cpp`, `whisper.h`, `ggml*.c`, `ggml*.h`) into `core/addetection/src/main/cpp/whisper/`. Follow the Android example's file list exactly.

**Step 2: Update CMakeLists.txt**

Follow the whisper.cpp Android example's CMakeLists.txt. Key points:
- Build ggml and whisper as static libraries
- Link against `android`, `log`, `c++_shared`
- Set appropriate compiler flags (`-O3`, `-DNDEBUG` for release)
- Enable ARM NEON for `arm64-v8a` and `armeabi-v7a`

**Step 3: Implement full JNI bridge**

Follow the whisper.cpp Android example's `whisper-jni.cpp`. The JNI bridge must expose:
- `loadModel(modelPath: String): Long` — calls `whisper_init_from_file()`, returns context pointer as jlong
- `transcribe(handle: Long, pcmPath: String): String` — reads 16kHz mono PCM, calls `whisper_full()`, returns JSON with segments
- `release(handle: Long)` — calls `whisper_free()`

**Step 4: Verify native build with whisper.cpp**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL with libwhisper-jni.so for all ABIs

**Step 5: Commit**

```bash
git add core/addetection/src/main/cpp/
git commit -m "feat: vendor whisper.cpp and build JNI bridge for Android"
```

---

### Task 10: Kotlin WhisperJni wrapper and WhisperTranscriptionEngine

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/WhisperJni.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/WhisperTranscriptionEngine.kt`

**Step 1: Write `WhisperJni.kt`**

```kotlin
package com.podbelly.core.addetection.engine

class WhisperJni {
    companion object {
        init {
            System.loadLibrary("whisper-jni")
        }
    }

    external fun loadModel(modelPath: String): Long
    external fun transcribe(handle: Long, pcmPath: String): String
    external fun release(handle: Long)
}
```

**Step 2: Write `WhisperTranscriptionEngine.kt`**

```kotlin
package com.podbelly.core.addetection.engine

import android.content.Context
import com.podbelly.core.addetection.TranscriptionEngine
import com.podbelly.core.addetection.model.TranscriptResult
import com.podbelly.core.addetection.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class WhisperTranscriptionEngine(
    private val context: Context,
) : TranscriptionEngine {

    private val whisperJni = WhisperJni()
    private var handle: Long = 0L

    private fun ensureModelLoaded() {
        if (handle != 0L) return
        val modelFile = File(context.filesDir, "models/ggml-base.en-q5_0.bin")
        if (!modelFile.exists()) {
            // Copy from assets on first run
            modelFile.parentFile?.mkdirs()
            context.assets.open("ggml-base.en-q5_0.bin").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        handle = whisperJni.loadModel(modelFile.absolutePath)
    }

    override suspend fun transcribe(
        audioPath: String,
        onProgress: (Float) -> Unit,
    ): TranscriptResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()
        val resultJson = whisperJni.transcribe(handle, audioPath)
        parseTranscriptJson(resultJson)
    }

    override fun release() {
        if (handle != 0L) {
            whisperJni.release(handle)
            handle = 0L
        }
    }

    private fun parseTranscriptJson(json: String): TranscriptResult {
        val segments = mutableListOf<TranscriptSegment>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            segments.add(
                TranscriptSegment(
                    startMs = obj.getLong("start"),
                    endMs = obj.getLong("end"),
                    text = obj.getString("text"),
                )
            )
        }
        return TranscriptResult(segments = segments)
    }
}
```

**Step 3: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add core/addetection/src/main/java/com/podbelly/core/addetection/engine/
git commit -m "feat: add WhisperJni wrapper and WhisperTranscriptionEngine"
```

---

### Task 11: Add whisper.cpp model to APK assets

**Files:**
- Create: `core/addetection/src/main/assets/ggml-base.en-q5_0.bin` — the quantized model file

> **Note:** Download the base.en Q5_0 model from HuggingFace (ggerganov/whisper.cpp). The file is ~90 MB. Due to its size, configure `aaptOptions` to prevent compression (model files must be read directly):

**Step 1: Download model**

Download `ggml-base.en-q5_0.bin` from `https://huggingface.co/ggerganov/whisper.cpp/tree/main` and place in `core/addetection/src/main/assets/`.

**Step 2: Configure no-compress in build.gradle.kts**

Add to the `android` block:

```kotlin
androidResources {
    noCompress += listOf("bin", "onnx", "tflite")
}
```

**Step 3: Add model file to .gitignore and use Git LFS or document download**

Since ~90 MB is too large for regular git, either:
- Add to `.gitignore` and document the download URL in a README
- Or use Git LFS: `git lfs track "*.bin"`

**Step 4: Verify APK includes the model**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Check APK size increased by ~90 MB.

**Step 5: Commit**

```bash
git add core/addetection/build.gradle.kts
git commit -m "feat: configure asset packaging for ML models (no-compress bin/onnx/tflite)"
```

---

### Task 12: Silero VAD integration

**Files:**
- Create: `core/addetection/src/main/assets/silero_vad.onnx` — Silero VAD model (~2 MB)
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/SileroVad.kt`
- Modify: `core/addetection/build.gradle.kts` — add ONNX Runtime dependency
- Modify: `gradle/libs.versions.toml` — add ONNX Runtime version

**Step 1: Add ONNX Runtime to version catalog**

In `libs.versions.toml`:
```toml
onnxruntime = "1.17.0"

# In [libraries]:
onnxruntime-android = { module = "com.microsoft.onnxruntime:onnxruntime-android", version.ref = "onnxruntime" }
```

**Step 2: Add dependency to `core:addetection/build.gradle.kts`**

```kotlin
implementation(libs.onnxruntime.android)
```

**Step 3: Download Silero VAD ONNX model**

Download from `https://github.com/snakers4/silero-vad/tree/master/src/silero_vad/data` and place in assets.

**Step 4: Write `SileroVad.kt`**

```kotlin
package com.podbelly.core.addetection.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.File
import java.nio.FloatBuffer

class SileroVad(context: Context) {

    private val ortEnv = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelFile = File(context.cacheDir, "silero_vad.onnx")
        if (!modelFile.exists()) {
            context.assets.open("silero_vad.onnx").use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        session = ortEnv.createSession(modelFile.absolutePath)
    }

    data class SpeechRegion(val startMs: Long, val endMs: Long)

    /**
     * Detect speech regions in 16kHz mono PCM audio.
     * Returns list of speech regions (non-silence) for selective transcription.
     */
    fun detectSpeech(
        pcmSamples: FloatArray,
        sampleRate: Int = 16000,
        windowSizeMs: Int = 512,
        threshold: Float = 0.5f,
    ): List<SpeechRegion> {
        val windowSize = sampleRate * windowSizeMs / 1000
        val regions = mutableListOf<SpeechRegion>()
        var inSpeech = false
        var speechStart = 0L

        for (i in pcmSamples.indices step windowSize) {
            val end = minOf(i + windowSize, pcmSamples.size)
            val chunk = pcmSamples.copyOfRange(i, end)
            if (chunk.size < windowSize) break

            val prob = runInference(chunk)
            val timeMs = (i.toLong() * 1000) / sampleRate

            if (prob >= threshold && !inSpeech) {
                inSpeech = true
                speechStart = timeMs
            } else if (prob < threshold && inSpeech) {
                inSpeech = false
                regions.add(SpeechRegion(speechStart, timeMs))
            }
        }

        if (inSpeech) {
            regions.add(
                SpeechRegion(speechStart, (pcmSamples.size.toLong() * 1000) / sampleRate)
            )
        }

        return regions
    }

    private fun runInference(chunk: FloatArray): Float {
        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            FloatBuffer.wrap(chunk),
            longArrayOf(1, chunk.size.toLong()),
        )
        val results = session.run(mapOf("input" to inputTensor))
        val output = results[0].value as Array<FloatArray>
        inputTensor.close()
        return output[0][0]
    }

    fun release() {
        session.close()
    }
}
```

> **Note:** The actual Silero VAD API may differ — check the model's input/output spec when integrating. The above is a structural template.

**Step 5: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add gradle/libs.versions.toml core/addetection/
git commit -m "feat: add Silero VAD integration for speech region detection"
```

---

### Task 13: AdDetectionWorker (WorkManager job)

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/AdDetectionWorker.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/AdDetectionManager.kt`

**Step 1: Write `AdDetectionWorker.kt`**

```kotlin
package com.podbelly.core.addetection

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.podbelly.core.database.dao.AdDetectionJobDao
import com.podbelly.core.database.dao.AdSegmentDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.entity.AdDetectionJobEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AdDetectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
    private val adDetectionJobDao: AdDetectionJobDao,
    private val adSegmentDao: AdSegmentDao,
    private val adDetectionPipeline: AdDetectionPipeline,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getLong(KEY_EPISODE_ID, 0L)
        if (episodeId == 0L) return Result.failure()

        val episode = episodeDao.getByIdOnce(episodeId) ?: return Result.failure()
        if (episode.downloadPath.isEmpty()) return Result.failure()

        Log.i(TAG, "Starting ad detection for episode: ${episode.title}")

        adDetectionJobDao.upsert(
            AdDetectionJobEntity(
                episodeId = episodeId,
                status = "processing",
                startedAt = System.currentTimeMillis(),
            )
        )

        return try {
            val segments = adDetectionPipeline.process(
                episodeId = episodeId,
                audioPath = episode.downloadPath,
                durationMs = episode.durationSeconds * 1000L,
                onProgress = { progress ->
                    setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                },
            )

            adSegmentDao.deleteByEpisodeId(episodeId)
            adSegmentDao.insertAll(segments)

            adDetectionJobDao.updateStatus(
                episodeId = episodeId,
                status = "completed",
                completedAt = System.currentTimeMillis(),
            )

            Log.i(TAG, "Ad detection complete: ${segments.size} segments found")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Ad detection failed for episode: ${episode.title}", e)
            adDetectionJobDao.updateStatus(episodeId = episodeId, status = "failed")
            Result.failure()
        }
    }

    companion object {
        const val TAG = "AdDetectionWorker"
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_PROGRESS = "progress"
        const val WORK_NAME_PREFIX = "ad_detection_"
    }
}
```

**Step 2: Write `AdDetectionManager.kt`**

```kotlin
package com.podbelly.core.addetection

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.podbelly.core.common.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
) {

    suspend fun enqueueDetection(episodeId: Long) {
        val requiresWifi = preferencesManager.adDetectionRequiresWifi.first()
        val requiresCharging = preferencesManager.adDetectionRequiresCharging.first()

        val constraints = Constraints.Builder().apply {
            if (requiresWifi) setRequiredNetworkType(NetworkType.UNMETERED)
            if (requiresCharging) setRequiresCharging(true)
        }.build()

        val workRequest = OneTimeWorkRequestBuilder<AdDetectionWorker>()
            .setInputData(workDataOf(AdDetectionWorker.KEY_EPISODE_ID to episodeId))
            .setConstraints(constraints)
            .addTag(AdDetectionWorker.WORK_NAME_PREFIX + episodeId)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            AdDetectionWorker.WORK_NAME_PREFIX + episodeId,
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }

    fun cancelDetection(episodeId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AdDetectionWorker.WORK_NAME_PREFIX + episodeId)
    }
}
```

**Step 3: Create placeholder `AdDetectionPipeline.kt`**

```kotlin
package com.podbelly.core.addetection

import com.podbelly.core.database.entity.AdSegmentEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdDetectionPipeline @Inject constructor() {

    suspend fun process(
        episodeId: Long,
        audioPath: String,
        durationMs: Long,
        onProgress: suspend (Float) -> Unit,
    ): List<AdSegmentEntity> {
        // Phases 2-4 implement this pipeline stage by stage
        // For now, return empty — no ads detected
        onProgress(1.0f)
        return emptyList()
    }
}
```

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add core/addetection/src/main/java/com/podbelly/core/addetection/
git commit -m "feat: add AdDetectionWorker, AdDetectionManager, and pipeline placeholder"
```

---

## Phase 3: Text Classification

### Task 14: Regex ad pattern matcher

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/classifier/RegexAdMatcher.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/classifier/RegexAdMatcherTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.podbelly.core.addetection.classifier

import com.podbelly.core.addetection.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexAdMatcherTest {

    private val matcher = RegexAdMatcher()

    @Test
    fun `sponsored by yields high confidence`() {
        val segment = TranscriptSegment(0, 5000, "This episode is sponsored by Squarespace")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.9f)
        assertEquals("regex", result.source)
    }

    @Test
    fun `promo code yields high confidence`() {
        val segment = TranscriptSegment(0, 5000, "Use code PODCAST for 20 percent off")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.9f)
    }

    @Test
    fun `vanity URL yields high confidence`() {
        val segment = TranscriptSegment(0, 5000, "Head to squarespace.com/podname to get started")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.9f)
    }

    @Test
    fun `back to the show yields high confidence`() {
        val segment = TranscriptSegment(0, 5000, "And now back to the show")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.9f)
    }

    @Test
    fun `free trial yields medium confidence`() {
        val segment = TranscriptSegment(0, 5000, "Sign up for a free trial today")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.7f)
        assertTrue(result.score < 0.9f)
    }

    @Test
    fun `normal content yields zero confidence`() {
        val segment = TranscriptSegment(0, 5000, "So I was thinking about the nature of consciousness")
        val result = matcher.match(segment)
        assertEquals(0f, result.score)
    }

    @Test
    fun `case insensitive matching`() {
        val segment = TranscriptSegment(0, 5000, "SPONSORED BY Athletic Greens")
        val result = matcher.match(segment)
        assertTrue(result.score >= 0.9f)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.RegexAdMatcherTest"`
Expected: FAIL — class not found

**Step 3: Write implementation**

```kotlin
package com.podbelly.core.addetection.classifier

import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptSegment

class RegexAdMatcher {

    private data class Pattern(val regex: Regex, val confidence: Float)

    private val highConfidence = listOf(
        "sponsored by",
        "brought to you by",
        "a word from our sponsors?",
        "support for this podcast comes from",
        "use code \\w+",
        "promo code",
        "discount code",
        "coupon code",
        "\\w+\\.(com|co|io|org)/\\w+",
        "and now back to the show",
        "and now back to",
        "let's get back to the show",
    ).map { Pattern(Regex(it, RegexOption.IGNORE_CASE), 0.9f) }

    private val mediumConfidence = listOf(
        "download the app",
        "free trial",
        "sign up today",
        "first month free",
        "i want to tell you about",
        "i've been using",
        "let me tell you about",
        "listeners get \\d+%? off",
    ).map { Pattern(Regex(it, RegexOption.IGNORE_CASE), 0.7f) }

    private val allPatterns = highConfidence + mediumConfidence

    fun match(segment: TranscriptSegment): AdProbability {
        var maxScore = 0f
        for (pattern in allPatterns) {
            if (pattern.regex.containsMatchIn(segment.text)) {
                maxScore = maxOf(maxScore, pattern.confidence)
            }
        }
        return AdProbability(score = maxScore, source = "regex")
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.RegexAdMatcherTest"`
Expected: All 7 tests PASS

**Step 5: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add regex-based ad pattern matcher with tests"
```

---

### Task 15: MobileBERT TFLite classifier

**Files:**
- Create: `core/addetection/src/main/assets/ad_classifier.tflite` — the quantized model
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/classifier/MobileBertClassifier.kt`
- Modify: `gradle/libs.versions.toml` — add TFLite dependency
- Modify: `core/addetection/build.gradle.kts` — add TFLite dependency

> **Pre-requisite:** Fine-tune MobileBERT on ad classification data (offline). Start from `morenolq/spotify-podcast-advertising-classification` weights, distill to MobileBERT, export as TFLite INT8. This is a one-time offline ML task, not part of the Android build.

**Step 1: Add TFLite to version catalog**

In `libs.versions.toml`:
```toml
tflite = "2.14.0"

# In [libraries]:
tflite = { module = "org.tensorflow:tensorflow-lite", version.ref = "tflite" }
tflite-support = { module = "org.tensorflow:tensorflow-lite-support", version.ref = "tflite" }
```

**Step 2: Add dependency to `core:addetection/build.gradle.kts`**

```kotlin
implementation(libs.tflite)
implementation(libs.tflite.support)
```

**Step 3: Write `MobileBertClassifier.kt`**

```kotlin
package com.podbelly.core.addetection.classifier

import android.content.Context
import com.podbelly.core.addetection.TextClassifier
import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptSegment
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MobileBertClassifier(context: Context) : TextClassifier {

    private val interpreter: Interpreter

    init {
        val modelFile = File(context.cacheDir, "ad_classifier.tflite")
        if (!modelFile.exists()) {
            context.assets.open("ad_classifier.tflite").use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        interpreter = Interpreter(modelFile)
    }

    override fun classify(segment: TranscriptSegment, relativePosition: Float): AdProbability {
        // Tokenize text (simplified — real implementation needs WordPiece tokenizer)
        val inputIds = tokenize(segment.text)
        val inputBuffer = ByteBuffer.allocateDirect(inputIds.size * 4).apply {
            order(ByteOrder.nativeOrder())
            inputIds.forEach { putInt(it) }
        }

        val outputBuffer = ByteBuffer.allocateDirect(4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val rawScore = outputBuffer.float

        // Apply position boost
        val positionBoost = calculatePositionBoost(relativePosition)
        val finalScore = (rawScore * positionBoost).coerceIn(0f, 1f)

        return AdProbability(score = finalScore, source = "classifier")
    }

    override fun release() {
        interpreter.close()
    }

    private fun tokenize(text: String): IntArray {
        // Simplified tokenization — real implementation uses WordPiece
        // Pad/truncate to 128 tokens
        val tokens = text.lowercase().split(" ")
            .map { it.hashCode() and 0x7FFF } // Placeholder — use real vocab
            .take(128)
        return IntArray(128) { i ->
            if (i < tokens.size) tokens[i] else 0
        }
    }

    private fun calculatePositionBoost(relativePosition: Float): Float {
        // Boost segments near typical ad positions
        val adPositions = floatArrayOf(0f, 0.33f, 0.5f, 0.67f, 1f)
        val minDistance = adPositions.minOf { kotlin.math.abs(it - relativePosition) }
        return if (minDistance < 0.05f) 1.15f
        else if (minDistance < 0.10f) 1.07f
        else 1.0f
    }
}
```

> **Important:** The tokenizer above is a placeholder. Real implementation needs a WordPiece tokenizer matching the model's vocabulary. Use the TFLite Support Library's `BertTokenizer` or port a vocab.txt file. Adjust based on the actual exported model's input spec.

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml core/addetection/
git commit -m "feat: add MobileBERT TFLite ad classifier with position boosting"
```

---

### Task 16: Combined text classification tier

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/classifier/CombinedTextClassifier.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/classifier/CombinedTextClassifierTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.podbelly.core.addetection.classifier

import com.podbelly.core.addetection.TextClassifier
import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptSegment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedTextClassifierTest {

    private val regexMatcher = RegexAdMatcher()
    private val mlClassifier = mockk<TextClassifier>()

    @Test
    fun `high confidence regex skips ML classifier`() {
        val classifier = CombinedTextClassifier(regexMatcher, mlClassifier)
        val segment = TranscriptSegment(0, 5000, "This is sponsored by Acme Corp")
        val result = classifier.classify(segment, 0.5f)
        assertTrue(result.score >= 0.9f)
        verify(exactly = 0) { mlClassifier.classify(any(), any()) }
    }

    @Test
    fun `no regex match falls through to ML classifier`() {
        every { mlClassifier.classify(any(), any()) } returns AdProbability(0.75f, "classifier")
        val classifier = CombinedTextClassifier(regexMatcher, mlClassifier)
        val segment = TranscriptSegment(0, 5000, "Let me tell you about my morning routine")
        val result = classifier.classify(segment, 0.5f)
        assertEquals(0.75f, result.score)
        verify(exactly = 1) { mlClassifier.classify(any(), any()) }
    }

    @Test
    fun `takes max of regex and ML scores`() {
        every { mlClassifier.classify(any(), any()) } returns AdProbability(0.95f, "classifier")
        val classifier = CombinedTextClassifier(regexMatcher, mlClassifier)
        val segment = TranscriptSegment(0, 5000, "Sign up for a free trial with my link")
        val result = classifier.classify(segment, 0.5f)
        assertTrue(result.score >= 0.7f) // At least medium regex confidence
    }
}
```

**Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.CombinedTextClassifierTest"`
Expected: FAIL — class not found

**Step 3: Write implementation**

```kotlin
package com.podbelly.core.addetection.classifier

import com.podbelly.core.addetection.TextClassifier
import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptSegment

class CombinedTextClassifier(
    private val regexMatcher: RegexAdMatcher,
    private val mlClassifier: TextClassifier?,
) : TextClassifier {

    override fun classify(segment: TranscriptSegment, relativePosition: Float): AdProbability {
        val regexResult = regexMatcher.match(segment)

        // High-confidence regex match — skip ML
        if (regexResult.score >= 0.9f) {
            return regexResult
        }

        // Run ML classifier if available
        val mlResult = mlClassifier?.classify(segment, relativePosition)

        // Return whichever scored higher
        return if (mlResult != null && mlResult.score > regexResult.score) {
            mlResult
        } else {
            regexResult
        }
    }

    override fun release() {
        mlClassifier?.release()
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.CombinedTextClassifierTest"`
Expected: All 3 tests PASS

**Step 5: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add CombinedTextClassifier (regex + ML tiered classification)"
```

---

## Phase 4: Signal Processing Pipeline

### Task 17: Probability smoothing

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/pipeline/ProbabilitySmoothing.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/pipeline/ProbabilitySmoothingTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.podbelly.core.addetection.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbabilitySmoothingTest {

    @Test
    fun `smoothing reduces isolated spike`() {
        // One high score surrounded by low scores
        val scores = floatArrayOf(0.1f, 0.1f, 0.9f, 0.1f, 0.1f)
        val smoothed = ProbabilitySmoothing.gaussianSmooth(scores, windowSize = 5)
        // The spike should be dampened
        assert(smoothed[2] < 0.9f)
        assert(smoothed[2] > 0.1f)
    }

    @Test
    fun `smoothing preserves contiguous block`() {
        val scores = floatArrayOf(0.1f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.1f)
        val smoothed = ProbabilitySmoothing.gaussianSmooth(scores, windowSize = 5)
        // Middle of block should remain high
        assert(smoothed[3] > 0.7f)
    }

    @Test
    fun `empty array returns empty`() {
        val smoothed = ProbabilitySmoothing.gaussianSmooth(floatArrayOf(), windowSize = 5)
        assertEquals(0, smoothed.size)
    }

    @Test
    fun `single element returns same`() {
        val smoothed = ProbabilitySmoothing.gaussianSmooth(floatArrayOf(0.5f), windowSize = 5)
        assertEquals(1, smoothed.size)
        assertEquals(0.5f, smoothed[0], 0.01f)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.ProbabilitySmoothingTest"`
Expected: FAIL

**Step 3: Write implementation**

```kotlin
package com.podbelly.core.addetection.pipeline

import kotlin.math.exp

object ProbabilitySmoothing {

    fun gaussianSmooth(scores: FloatArray, windowSize: Int = 5): FloatArray {
        if (scores.isEmpty()) return floatArrayOf()
        if (scores.size == 1) return scores.copyOf()

        val halfWindow = windowSize / 2
        val kernel = generateGaussianKernel(windowSize)
        val result = FloatArray(scores.size)

        for (i in scores.indices) {
            var weightedSum = 0f
            var kernelSum = 0f
            for (j in -halfWindow..halfWindow) {
                val idx = i + j
                if (idx in scores.indices) {
                    val weight = kernel[j + halfWindow]
                    weightedSum += scores[idx] * weight
                    kernelSum += weight
                }
            }
            result[i] = if (kernelSum > 0f) weightedSum / kernelSum else scores[i]
        }

        return result
    }

    private fun generateGaussianKernel(size: Int): FloatArray {
        val sigma = size / 4.0
        val half = size / 2
        return FloatArray(size) { i ->
            val x = (i - half).toDouble()
            exp(-(x * x) / (2 * sigma * sigma)).toFloat()
        }
    }
}
```

**Step 4: Run tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.ProbabilitySmoothingTest"`
Expected: All PASS

**Step 5: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add Gaussian probability smoothing for ad scores"
```

---

### Task 18: Hysteresis thresholding

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/pipeline/HysteresisDetector.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/pipeline/HysteresisDetectorTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.podbelly.core.addetection.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class HysteresisDetectorTest {

    private val detector = HysteresisDetector(
        enterThreshold = 0.6f,
        exitThreshold = 0.4f,
        exitCount = 3,
    )

    @Test
    fun `enters ad state above threshold`() {
        val scores = floatArrayOf(0.1f, 0.7f, 0.8f, 0.7f, 0.1f, 0.1f, 0.1f)
        val regions = detector.detect(scores, segmentDurationMs = 5000)
        assertEquals(1, regions.size)
    }

    @Test
    fun `requires multiple low scores to exit ad state`() {
        // Brief dip below exit threshold should NOT exit
        val scores = floatArrayOf(0.7f, 0.8f, 0.3f, 0.8f, 0.7f, 0.1f, 0.1f, 0.1f)
        val regions = detector.detect(scores, segmentDurationMs = 5000)
        assertEquals(1, regions.size)
        // Region should span from index 0 to index 4 (brief dip merged)
    }

    @Test
    fun `no ad regions for all low scores`() {
        val scores = floatArrayOf(0.1f, 0.2f, 0.3f, 0.2f, 0.1f)
        val regions = detector.detect(scores, segmentDurationMs = 5000)
        assertEquals(0, regions.size)
    }

    @Test
    fun `multiple separate ad regions`() {
        val scores = floatArrayOf(0.8f, 0.8f, 0.1f, 0.1f, 0.1f, 0.1f, 0.8f, 0.8f, 0.1f, 0.1f, 0.1f)
        val regions = detector.detect(scores, segmentDurationMs = 5000)
        assertEquals(2, regions.size)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.HysteresisDetectorTest"`
Expected: FAIL

**Step 3: Write implementation**

```kotlin
package com.podbelly.core.addetection.pipeline

data class AdRegion(
    val startMs: Long,
    val endMs: Long,
    val averageConfidence: Float,
)

class HysteresisDetector(
    private val enterThreshold: Float = 0.6f,
    private val exitThreshold: Float = 0.4f,
    private val exitCount: Int = 3,
) {

    fun detect(scores: FloatArray, segmentDurationMs: Long): List<AdRegion> {
        val regions = mutableListOf<AdRegion>()
        var inAd = false
        var adStartIndex = 0
        var belowCount = 0
        var scoreSum = 0f
        var scoreCount = 0

        for (i in scores.indices) {
            if (!inAd) {
                if (scores[i] >= enterThreshold) {
                    inAd = true
                    adStartIndex = i
                    belowCount = 0
                    scoreSum = scores[i]
                    scoreCount = 1
                }
            } else {
                scoreSum += scores[i]
                scoreCount++
                if (scores[i] < exitThreshold) {
                    belowCount++
                    if (belowCount >= exitCount) {
                        // Exit ad state — end region at the point where dip started
                        val endIndex = i - exitCount + 1
                        regions.add(
                            AdRegion(
                                startMs = adStartIndex * segmentDurationMs,
                                endMs = endIndex * segmentDurationMs,
                                averageConfidence = scoreSum / scoreCount,
                            )
                        )
                        inAd = false
                        scoreSum = 0f
                        scoreCount = 0
                    }
                } else {
                    belowCount = 0
                }
            }
        }

        // Close open region at end of episode
        if (inAd) {
            regions.add(
                AdRegion(
                    startMs = adStartIndex * segmentDurationMs,
                    endMs = scores.size * segmentDurationMs,
                    averageConfidence = scoreSum / scoreCount,
                )
            )
        }

        return regions
    }
}
```

**Step 4: Run tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.HysteresisDetectorTest"`
Expected: All PASS

**Step 5: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add hysteresis-based ad region detector"
```

---

### Task 19: Gap merging and boundary snapping

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/pipeline/BoundaryRefiner.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/pipeline/BoundaryRefinerTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.podbelly.core.addetection.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundaryRefinerTest {

    private val refiner = BoundaryRefiner(gapThresholdMs = 8000, snapWindowMs = 3000)

    @Test
    fun `merges regions separated by less than threshold`() {
        val regions = listOf(
            AdRegion(10000, 20000, 0.8f),
            AdRegion(25000, 35000, 0.8f), // 5s gap — should merge
        )
        val merged = refiner.mergeGaps(regions)
        assertEquals(1, merged.size)
        assertEquals(10000, merged[0].startMs)
        assertEquals(35000, merged[0].endMs)
    }

    @Test
    fun `keeps regions separated by more than threshold`() {
        val regions = listOf(
            AdRegion(10000, 20000, 0.8f),
            AdRegion(30000, 40000, 0.8f), // 10s gap — should NOT merge
        )
        val merged = refiner.mergeGaps(regions)
        assertEquals(2, merged.size)
    }

    @Test
    fun `snaps boundary to nearest silence`() {
        val silences = listOf(
            SilenceGap(9500, 10200), // Near ad start at 10000
            SilenceGap(19800, 20500), // Near ad end at 20000
        )
        val region = AdRegion(10000, 20000, 0.8f)
        val snapped = refiner.snapToSilence(region, silences)
        assertEquals(9500, snapped.startMs) // Snapped to silence start
        assertEquals(20500, snapped.endMs) // Snapped to silence end
    }

    @Test
    fun `no snap if no silence within window`() {
        val silences = listOf(
            SilenceGap(5000, 5500), // Too far from ad at 10000
        )
        val region = AdRegion(10000, 20000, 0.8f)
        val snapped = refiner.snapToSilence(region, silences)
        assertEquals(10000, snapped.startMs) // Unchanged
        assertEquals(20000, snapped.endMs) // Unchanged
    }
}
```

**Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.BoundaryRefinerTest"`
Expected: FAIL

**Step 3: Write implementation**

```kotlin
package com.podbelly.core.addetection.pipeline

import kotlin.math.abs

data class SilenceGap(val startMs: Long, val endMs: Long)

class BoundaryRefiner(
    private val gapThresholdMs: Long = 8000,
    private val snapWindowMs: Long = 3000,
) {

    fun mergeGaps(regions: List<AdRegion>): List<AdRegion> {
        if (regions.isEmpty()) return emptyList()

        val merged = mutableListOf<AdRegion>()
        var current = regions[0]

        for (i in 1 until regions.size) {
            val next = regions[i]
            val gap = next.startMs - current.endMs
            if (gap < gapThresholdMs) {
                // Merge
                val totalDuration = (current.endMs - current.startMs) + (next.endMs - next.startMs)
                val currentWeight = (current.endMs - current.startMs).toFloat() / totalDuration
                val nextWeight = (next.endMs - next.startMs).toFloat() / totalDuration
                current = AdRegion(
                    startMs = current.startMs,
                    endMs = next.endMs,
                    averageConfidence = current.averageConfidence * currentWeight +
                        next.averageConfidence * nextWeight,
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged
    }

    fun snapToSilence(region: AdRegion, silences: List<SilenceGap>): AdRegion {
        var startMs = region.startMs
        var endMs = region.endMs

        // Snap start to nearest silence gap start
        val nearestStart = silences
            .filter { abs(it.startMs - region.startMs) <= snapWindowMs }
            .minByOrNull { abs(it.startMs - region.startMs) }
        if (nearestStart != null) {
            startMs = nearestStart.startMs
        }

        // Snap end to nearest silence gap end
        val nearestEnd = silences
            .filter { abs(it.endMs - region.endMs) <= snapWindowMs }
            .minByOrNull { abs(it.endMs - region.endMs) }
        if (nearestEnd != null) {
            endMs = nearestEnd.endMs
        }

        return region.copy(startMs = startMs, endMs = endMs)
    }

    fun refine(regions: List<AdRegion>, silences: List<SilenceGap>): List<AdRegion> {
        val merged = mergeGaps(regions)
        return merged.map { snapToSilence(it, silences) }
    }
}
```

**Step 4: Run tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.BoundaryRefinerTest"`
Expected: All PASS

**Step 5: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add gap merging and silence-snapping boundary refinement"
```

---

### Task 20: Wire up the full AdDetectionPipeline

**Files:**
- Modify: `core/addetection/src/main/java/com/podbelly/core/addetection/AdDetectionPipeline.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/AdDetectionPipelineTest.kt`

**Step 1: Write integration test with mocked components**

```kotlin
package com.podbelly.core.addetection

import com.podbelly.core.addetection.classifier.CombinedTextClassifier
import com.podbelly.core.addetection.classifier.RegexAdMatcher
import com.podbelly.core.addetection.model.AdProbability
import com.podbelly.core.addetection.model.TranscriptResult
import com.podbelly.core.addetection.model.TranscriptSegment
import com.podbelly.core.addetection.pipeline.BoundaryRefiner
import com.podbelly.core.addetection.pipeline.HysteresisDetector
import com.podbelly.core.addetection.pipeline.SilenceGap
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdDetectionPipelineTest {

    @Test
    fun `pipeline produces ad segments from transcript with ad phrases`() = runTest {
        val transcriptionEngine = mockk<TranscriptionEngine>()
        coEvery { transcriptionEngine.transcribe(any(), any()) } returns TranscriptResult(
            segments = listOf(
                TranscriptSegment(0, 30000, "Welcome to the show"),
                TranscriptSegment(30000, 60000, "This episode is sponsored by Acme Corp"),
                TranscriptSegment(60000, 90000, "Go to acme.com/podcast for a free trial"),
                TranscriptSegment(90000, 120000, "And now back to the show"),
                TranscriptSegment(120000, 150000, "So as I was saying about consciousness"),
            )
        )

        val classifier = CombinedTextClassifier(RegexAdMatcher(), mlClassifier = null)
        val pipeline = AdDetectionPipeline(
            transcriptionEngine = transcriptionEngine,
            textClassifier = classifier,
            hysteresisDetector = HysteresisDetector(),
            boundaryRefiner = BoundaryRefiner(),
        )

        val segments = pipeline.process(
            episodeId = 1L,
            audioPath = "/fake/path.mp3",
            durationMs = 150000,
            onProgress = {},
        )

        assertTrue(segments.isNotEmpty())
        // Should detect the ad block (segments 1-3)
        assertTrue(segments.any { it.startMs <= 30000 && it.endMs >= 90000 })
    }
}
```

**Step 2: Update `AdDetectionPipeline.kt` with full implementation**

Replace the placeholder with the real pipeline that orchestrates:
1. Transcription (via `TranscriptionEngine`)
2. Classification (via `CombinedTextClassifier` on each segment)
3. Smoothing (via `ProbabilitySmoothing`)
4. Hysteresis detection (via `HysteresisDetector`)
5. Boundary refinement (via `BoundaryRefiner`)
6. Mapping `AdRegion` → `AdSegmentEntity`

```kotlin
package com.podbelly.core.addetection

import com.podbelly.core.addetection.pipeline.BoundaryRefiner
import com.podbelly.core.addetection.pipeline.HysteresisDetector
import com.podbelly.core.addetection.pipeline.ProbabilitySmoothing
import com.podbelly.core.database.entity.AdSegmentEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdDetectionPipeline @Inject constructor(
    private val transcriptionEngine: TranscriptionEngine,
    private val textClassifier: TextClassifier,
    private val hysteresisDetector: HysteresisDetector,
    private val boundaryRefiner: BoundaryRefiner,
) {

    suspend fun process(
        episodeId: Long,
        audioPath: String,
        durationMs: Long,
        onProgress: suspend (Float) -> Unit,
    ): List<AdSegmentEntity> {
        // Stage 1: Transcribe
        onProgress(0.1f)
        val transcript = transcriptionEngine.transcribe(audioPath) { p ->
            // Map transcription progress to 10-80% of total
        }
        onProgress(0.8f)

        if (transcript.segments.isEmpty()) return emptyList()

        // Stage 2: Classify each segment
        val scores = FloatArray(transcript.segments.size) { i ->
            val segment = transcript.segments[i]
            val relativePosition = if (durationMs > 0) {
                segment.startMs.toFloat() / durationMs
            } else 0f
            textClassifier.classify(segment, relativePosition).score
        }
        onProgress(0.85f)

        // Stage 3: Smooth
        val smoothed = ProbabilitySmoothing.gaussianSmooth(scores)

        // Stage 4: Hysteresis detection
        val avgSegmentDuration = if (transcript.segments.size > 1) {
            (transcript.segments.last().endMs - transcript.segments.first().startMs) /
                transcript.segments.size
        } else 30000L
        val regions = hysteresisDetector.detect(smoothed, avgSegmentDuration)
        onProgress(0.9f)

        // Stage 5: Boundary refinement (no silence data yet — Phase 6 adds this)
        val refined = boundaryRefiner.mergeGaps(regions)
        onProgress(0.95f)

        // Map to entities
        return refined.map { region ->
            AdSegmentEntity(
                episodeId = episodeId,
                startMs = region.startMs,
                endMs = region.endMs,
                confidence = region.averageConfidence,
                source = "pipeline",
            )
        }
    }
}
```

**Step 3: Run tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: wire up full ad detection pipeline (transcribe → classify → smooth → detect → refine)"
```

---

## Phase 5: Playback Integration

### Task 21: Seekbar ad segment overlay

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt` — add `AdSegmentOverlay` composable inside `SeekBar`
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerViewModel.kt` — expose ad segments flow
- Modify: `feature/player/build.gradle.kts` — add dependency on `core:database` (if not already present)

**Step 1: Add ad segments state to PlayerViewModel**

Inject `AdSegmentDao`. Expose a `StateFlow<List<AdSegmentEntity>>` that updates when `episodeId` changes:

```kotlin
// In PlayerViewModel:
private val _adSegments = MutableStateFlow<List<AdSegmentEntity>>(emptyList())
val adSegments: StateFlow<List<AdSegmentEntity>> = _adSegments.asStateFlow()

// In init or when episode changes:
viewModelScope.launch {
    playbackState.map { it.episodeId }
        .distinctUntilChanged()
        .flatMapLatest { episodeId ->
            if (episodeId > 0L) adSegmentDao.getByEpisodeId(episodeId)
            else flowOf(emptyList())
        }
        .collect { _adSegments.value = it }
}
```

**Step 2: Add `AdSegmentOverlay` composable**

Draw semi-transparent colored bars on the seekbar track at ad positions:

```kotlin
@Composable
private fun AdSegmentOverlay(
    adSegments: List<AdSegmentEntity>,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    if (adSegments.isEmpty() || duration <= 0L) return

    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        val trackWidth = size.width
        adSegments.forEach { segment ->
            val startFraction = segment.startMs.toFloat() / duration
            val endFraction = segment.endMs.toFloat() / duration
            val startX = startFraction * trackWidth
            val width = (endFraction - startFraction) * trackWidth

            drawRect(
                color = Color(0x66FF6B35), // Semi-transparent orange
                topLeft = Offset(startX, 0f),
                size = Size(width.coerceAtLeast(2f), size.height),
            )
        }
    }
}
```

**Step 3: Position overlay behind the Slider in SeekBar composable**

Use a `Box` to layer the overlay behind the existing `Slider`:

```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    AdSegmentOverlay(
        adSegments = adSegments,
        duration = duration,
        modifier = Modifier.align(Alignment.Center),
    )
    Slider(/* existing slider code */)
}
```

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :feature:player:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/player/src/ feature/player/build.gradle.kts
git commit -m "feat: add ad segment overlay on player seekbar"
```

---

### Task 22: Auto-skip logic in PlaybackController

**Files:**
- Modify: `core/playback/src/main/java/com/podbelly/core/playback/PlaybackController.kt`
- Modify: `core/playback/build.gradle.kts` — add dependency on `core:database`

**Step 1: Add ad segment caching to PlaybackController**

```kotlin
// New fields:
private var cachedAdSegments: List<AdSegmentEntity> = emptyList()
private var cachedEpisodeId: Long = 0L
private var lastUserSeekMs: Long = 0L
private var lastSkippedSegmentId: Long = 0L
```

**Step 2: Load ad segments when episode changes**

In the episode change handler, query `AdSegmentDao.getActiveByEpisodeIdOnce()` and cache the result.

**Step 3: Add skip check to the 250ms position update loop**

Inside `startPositionUpdates()`, after updating position, add:

```kotlin
// Auto-skip ad segments
if (adAutoSkipEnabled && cachedAdSegments.isNotEmpty()) {
    val pos = controller.currentPosition
    val adSegment = cachedAdSegments.firstOrNull { pos >= it.startMs && pos < it.endMs }
    if (adSegment != null
        && adSegment.id != lastSkippedSegmentId
        && (pos - lastUserSeekMs) > 2000L
    ) {
        lastSkippedSegmentId = adSegment.id
        controller.seekTo(adSegment.endMs)
        _playbackState.update { it.copy(currentPosition = adSegment.endMs) }
        _adSkipEvent.emit(adSegment) // For snackbar in UI
    }
}
```

**Step 4: Track user seeks**

In the existing `seekTo()` method, add:
```kotlin
lastUserSeekMs = System.currentTimeMillis()
```

Wait — the timestamp should be the position, not wall clock. Actually, it should be wall clock time (to detect "was there a recent user seek?"):

```kotlin
fun seekTo(position: Long) {
    lastUserSeekMs = System.currentTimeMillis() // Track when user last manually sought
    // ... existing seek logic
}
```

**Step 5: Expose skip event for UI snackbar**

```kotlin
private val _adSkipEvent = MutableSharedFlow<AdSegmentEntity>(extraBufferCapacity = 1)
val adSkipEvent: SharedFlow<AdSegmentEntity> = _adSkipEvent.asSharedFlow()
```

**Step 6: Add undo-skip method**

```kotlin
fun undoAdSkip(segment: AdSegmentEntity) {
    seekTo(segment.startMs)
    // Mark segment as dismissed in database
    scope.launch {
        adSegmentDao.setDismissed(segment.id, true)
        // Refresh cached segments
        cachedAdSegments = cachedAdSegments.filter { it.id != segment.id }
    }
}
```

**Step 7: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:playback:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add core/playback/src/ core/playback/build.gradle.kts
git commit -m "feat: add auto-skip logic for detected ad segments in PlaybackController"
```

---

### Task 23: Auto-skip snackbar and undo in PlayerScreen

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt`

**Step 1: Observe skip events and show snackbar**

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(Unit) {
    playbackController.adSkipEvent.collect { segment ->
        val result = snackbarHostState.showSnackbar(
            message = "Skipped ad",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            playbackController.undoAdSkip(segment)
        }
    }
}
```

**Step 2: Add `SnackbarHost` to the screen's `Scaffold`**

Ensure the snackbar is visible above the player controls.

**Step 3: Verify build compiles and test on device**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. Install and verify skip + undo flow works.

**Step 4: Commit**

```bash
git add feature/player/src/
git commit -m "feat: add auto-skip snackbar with undo in player screen"
```

---

### Task 24: Per-podcast auto-skip override

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/entity/PodcastEntity.kt` — add `adAutoSkip: Boolean?` column
- Modify: `core/database/src/main/java/com/podbelly/core/database/PodbellDatabase.kt` — add MIGRATION_4_5
- Modify: `core/database/src/main/java/com/podbelly/core/database/di/DatabaseModule.kt` — register migration
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/PodcastDetailScreen.kt` — add overflow menu option

**Step 1: Add column to PodcastEntity**

```kotlin
@ColumnInfo(name = "adAutoSkip")
val adAutoSkip: Boolean? = null,
```

**Step 2: Add MIGRATION_4_5**

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE podcasts ADD COLUMN adAutoSkip INTEGER DEFAULT NULL")
    }
}
```

Bump database version to 5. Register migration in `DatabaseModule.kt`.

**Step 3: Add toggle in PodcastDetailScreen overflow menu**

Add a menu item "Auto-skip ads" with a checkmark that cycles through null → true → false → null.

**Step 4: Update PlaybackController to check per-podcast override**

When loading ad segments, also load the podcast's `adAutoSkip` value. Use it to override the global setting.

**Step 5: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add core/database/src/ feature/podcast/src/
git commit -m "feat: add per-podcast auto-skip override"
```

---

### Task 25: Episode detail screen — detect ads button and status

**Files:**
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/EpisodeDetailScreen.kt`
- Modify: `feature/podcast/src/main/java/com/podbelly/feature/podcast/EpisodeDetailViewModel.kt`

**Step 1: Add ad detection state to ViewModel**

Observe `AdDetectionJobDao.getByEpisodeId()` and `AdSegmentDao.getActiveCount()` + `getTotalAdDurationMs()`.

**Step 2: Add UI elements to EpisodeDetailScreen**

Below the download/play button, add:

```kotlin
// No detection run + episode is downloaded
if (adDetectionJob == null && uiState.isDownloaded && adDetectionEnabled) {
    OutlinedButton(
        onClick = { viewModel.detectAds() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text("Detect ads")
    }
}

// Processing
if (adDetectionJob?.status == "processing") {
    LinearProgressIndicator(
        progress = { adDetectionProgress },
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Analyzing episode...", style = MaterialTheme.typography.bodySmall)
}

// Completed
if (adDetectionJob?.status == "completed") {
    Text(
        text = "$adSegmentCount ad segments detected (${formatMillis(totalAdDurationMs)})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

**Step 3: Implement `detectAds()` in ViewModel**

Call `AdDetectionManager.enqueueDetection(episodeId)`.

**Step 4: Verify build compiles**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/podcast/src/
git commit -m "feat: add detect ads button and status on episode detail screen"
```

---

## Phase 6: Audio Fingerprinting

### Task 26: Chromaprint NDK integration

**Files:**
- Create: `core/addetection/src/main/cpp/chromaprint/` — vendored Chromaprint source
- Create: `core/addetection/src/main/cpp/chromaprint-jni.cpp`
- Modify: `core/addetection/src/main/cpp/CMakeLists.txt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/ChromaprintJni.kt`
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/ChromaprintFingerprinter.kt`

> **Reference:** Clone Chromaprint from GitHub. Build with KissFFT (not FFTW) for MIT license. The C API in `chromaprint.h` is the integration surface.

**Step 1: Vendor Chromaprint source and configure CMake**

Add Chromaprint as a static library target in CMakeLists.txt. Build with `-DWITH_KISSFFT=ON`.

**Step 2: Write JNI bridge `chromaprint-jni.cpp`**

Expose `fingerprint(pcmPath, startMs, endMs) -> jbyteArray` and `match(a, b) -> jfloat`.

**Step 3: Write Kotlin wrapper `ChromaprintJni.kt`**

```kotlin
package com.podbelly.core.addetection.engine

class ChromaprintJni {
    companion object {
        init { System.loadLibrary("chromaprint-jni") }
    }

    external fun fingerprint(pcmPath: String, startMs: Long, endMs: Long): ByteArray
    external fun match(a: ByteArray, b: ByteArray): Float
}
```

**Step 4: Write `ChromaprintFingerprinter.kt` implementing `AudioFingerprinter`**

```kotlin
package com.podbelly.core.addetection.engine

import com.podbelly.core.addetection.AudioFingerprinter

class ChromaprintFingerprinter : AudioFingerprinter {
    private val jni = ChromaprintJni()

    override fun fingerprint(pcmPath: String, startMs: Long, endMs: Long): ByteArray {
        return jni.fingerprint(pcmPath, startMs, endMs)
    }

    override fun match(a: ByteArray, b: ByteArray): Float {
        return jni.match(a, b)
    }

    override fun release() { /* No resources to free */ }
}
```

**Step 5: Verify native build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add core/addetection/src/main/cpp/ core/addetection/src/main/java/com/podbelly/core/addetection/engine/Chromaprint*
git commit -m "feat: add Chromaprint NDK integration for audio fingerprinting"
```

---

### Task 27: Fingerprint matching as pipeline Stage 0

**Files:**
- Create: `core/addetection/src/main/java/com/podbelly/core/addetection/pipeline/FingerprintMatcher.kt`
- Create: `core/addetection/src/test/java/com/podbelly/core/addetection/pipeline/FingerprintMatcherTest.kt`
- Modify: `core/addetection/src/main/java/com/podbelly/core/addetection/AdDetectionPipeline.kt` — add Stage 0

**Step 1: Write `FingerprintMatcher.kt`**

```kotlin
package com.podbelly.core.addetection.pipeline

import com.podbelly.core.addetection.AudioFingerprinter
import com.podbelly.core.database.dao.AdFingerprintDao
import com.podbelly.core.database.entity.AdSegmentEntity

class FingerprintMatcher(
    private val fingerprinter: AudioFingerprinter,
    private val fingerprintDao: AdFingerprintDao,
    private val matchThreshold: Float = 0.8f,
    private val windowMs: Long = 30_000,
    private val overlapMs: Long = 15_000,
) {

    suspend fun matchKnownAds(
        episodeId: Long,
        pcmPath: String,
        durationMs: Long,
    ): List<AdSegmentEntity> {
        val knownFingerprints = fingerprintDao.getAll()
        if (knownFingerprints.isEmpty()) return emptyList()

        val segments = mutableListOf<AdSegmentEntity>()
        var startMs = 0L

        while (startMs < durationMs) {
            val endMs = minOf(startMs + windowMs, durationMs)
            val windowFp = fingerprinter.fingerprint(pcmPath, startMs, endMs)

            for (known in knownFingerprints) {
                val similarity = fingerprinter.match(windowFp, known.fingerprint)
                if (similarity >= matchThreshold) {
                    segments.add(
                        AdSegmentEntity(
                            episodeId = episodeId,
                            startMs = startMs,
                            endMs = minOf(startMs + known.durationMs, durationMs),
                            confidence = similarity,
                            source = "fingerprint",
                        )
                    )
                    break // One match per window is enough
                }
            }

            startMs += (windowMs - overlapMs)
        }

        return segments
    }
}
```

**Step 2: Write test**

```kotlin
package com.podbelly.core.addetection.pipeline

import com.podbelly.core.addetection.AudioFingerprinter
import com.podbelly.core.database.dao.AdFingerprintDao
import com.podbelly.core.database.entity.AdFingerprintEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FingerprintMatcherTest {

    @Test
    fun `returns empty when no known fingerprints`() = runTest {
        val fingerprinter = mockk<AudioFingerprinter>()
        val dao = mockk<AdFingerprintDao>()
        coEvery { dao.getAll() } returns emptyList()

        val matcher = FingerprintMatcher(fingerprinter, dao)
        val result = matcher.matchKnownAds(1L, "/fake.pcm", 60000)
        assertEquals(0, result.size)
    }

    @Test
    fun `detects known ad via fingerprint match`() = runTest {
        val fingerprinter = mockk<AudioFingerprinter>()
        val dao = mockk<AdFingerprintDao>()

        val knownFp = AdFingerprintEntity(
            id = 1,
            fingerprint = byteArrayOf(1, 2, 3),
            durationMs = 30000,
            firstSeenEpisodeId = 99,
        )
        coEvery { dao.getAll() } returns listOf(knownFp)
        every { fingerprinter.fingerprint(any(), any(), any()) } returns byteArrayOf(1, 2, 3)
        every { fingerprinter.match(any(), any()) } returns 0.95f

        val matcher = FingerprintMatcher(fingerprinter, dao)
        val result = matcher.matchKnownAds(1L, "/fake.pcm", 60000)
        assertEquals(true, result.isNotEmpty())
        assertEquals("fingerprint", result[0].source)
    }
}
```

**Step 3: Run tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:addetection:testDebugUnitTest --tests "*.FingerprintMatcherTest"`
Expected: All PASS

**Step 4: Integrate into AdDetectionPipeline as Stage 0**

Add fingerprint matching before transcription. Fingerprint-matched regions are excluded from transcription input (they're already detected).

**Step 5: Add post-pipeline fingerprint storage**

After the pipeline completes, fingerprint each detected ad segment and store in `AdFingerprintDao` for future matching.

**Step 6: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add fingerprint matching as pipeline Stage 0 and post-pipeline fingerprint storage"
```

---

## Phase 7: Polish

### Task 28: Manual ad marking on seekbar

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt`
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerViewModel.kt`

**Step 1: Add long-press gesture to seekbar**

On long-press, enter "region select" mode where two draggable handles appear. User drags to define start/end of an ad region.

**Step 2: Add "Mark as ad" / "Not an ad" buttons**

When in region select mode, show action buttons below the seekbar:
- "Mark as ad" → creates `AdSegmentEntity` with `source = "manual"`, fingerprints the region
- "Not an ad" → if overlapping an existing segment, sets `dismissed = true`
- "Cancel" → exits region select mode

**Step 3: Implement in ViewModel**

`markAsAd(startMs, endMs)` and `dismissAdSegment(segmentId)` methods that call through to DAOs and fingerprinter.

**Step 4: Commit**

```bash
git add feature/player/src/
git commit -m "feat: add manual ad marking via long-press on seekbar"
```

---

### Task 29: Complete settings UI with storage info and clear data

**Files:**
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/SettingsViewModel.kt`

**Step 1: Add storage usage display**

Show model size + fingerprint DB size + cached transcripts size below the toggles.

**Step 2: Add "Clear all ad data" button**

Destructive action (red text) that:
- Deletes all `AdSegment` rows
- Deletes all `AdDetectionJob` rows
- Deletes all `AdFingerprint` rows
- Deletes cached transcript files
- Shows confirmation dialog before proceeding

**Step 3: Commit**

```bash
git add feature/settings/src/
git commit -m "feat: add storage info and clear data to ad detection settings"
```

---

### Task 30: Battery-aware processing

**Files:**
- Modify: `core/addetection/src/main/java/com/podbelly/core/addetection/AdDetectionWorker.kt`

**Step 1: Add battery level check**

At the start of `doWork()` and periodically during transcription:

```kotlin
val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
if (batteryLevel < 15) {
    Log.w(TAG, "Battery too low ($batteryLevel%), deferring ad detection")
    return Result.retry()
}
```

**Step 2: Add foreground notification**

Show a persistent notification during processing: "Analyzing [Episode Name]... X%"

Follow Android WorkManager `setForeground()` / `ForegroundInfo` pattern.

**Step 3: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add battery-aware processing and foreground notification for ad detection"
```

---

### Task 31: Memory-aware model selection

**Files:**
- Modify: `core/addetection/src/main/java/com/podbelly/core/addetection/engine/WhisperTranscriptionEngine.kt`

**Step 1: Check available memory before loading model**

```kotlin
private fun selectModel(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    val availableMb = memInfo.availMem / (1024 * 1024)

    return if (availableMb >= 512) {
        "ggml-base.en-q5_0.bin"
    } else {
        Log.w(TAG, "Low memory ($availableMb MB available), using tiny.en model")
        "ggml-tiny.en.bin"
    }
}
```

**Step 2: Bundle tiny.en as fallback**

Add `ggml-tiny.en.bin` (~75 MB) to assets as a fallback model.

**Step 3: Commit**

```bash
git add core/addetection/src/
git commit -m "feat: add memory-aware model selection (base.en → tiny.en fallback)"
```

---

### Task 32: Version bump and final integration test

**Files:**
- Modify: `app/build.gradle.kts` — bump versionCode and versionName

**Step 1: Bump version**

Increment `versionCode` and `versionName` per CLAUDE.md convention.

**Step 2: Run full test suite**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`
Expected: All tests PASS

**Step 3: Build and install on device**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Install APK and verify:
- Settings → Ad Detection section appears, toggles work
- Episode detail → "Detect ads" button appears for downloaded episodes
- Player → seekbar shows ad markers (after detection completes)
- Auto-skip works when enabled
- Undo snackbar works

**Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version for ad detection feature"
```
