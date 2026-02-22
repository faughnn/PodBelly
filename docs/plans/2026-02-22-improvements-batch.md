# Improvements Batch Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Ship 5 improvements: stronger CLAUDE.md, hybrid speed picker, library image fix, refresh-on-resume, and top refresh banner.

**Architecture:** All changes are independent — each task can be built, tested, and committed separately. No shared state between tasks.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Lifecycle APIs, Material 3

---

## Task 1: Update CLAUDE.md with explicit reference app research instructions

**Files:**
- Modify: `CLAUDE.md`

**Step 1: Replace CLAUDE.md content**

```markdown
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
```

**Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: add explicit reference app search instructions to CLAUDE.md"
```

---

## Task 2: Hybrid speed picker (grid + slider)

**Files:**
- Modify: `feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt` (SpeedPickerContent, lines ~733-795)

**Step 1: Replace SpeedPickerContent**

Replace the entire `SpeedPickerContent` composable (from `@Composable internal fun SpeedPickerContent` through its closing brace) with:

```kotlin
@Composable
internal fun SpeedPickerContent(
    currentSpeed: Float,
    podcastTitle: String = "",
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf(1.0f, 1.2f, 1.4f, 1.5f, 1.6f, 1.8f, 2.0f, 2.5f, 3.0f)

    // Local slider state — snaps to 0.1 increments
    var sliderSpeed by remember { mutableFloatStateOf(currentSpeed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = if (podcastTitle.isNotBlank()) "Speed for $podcastTitle" else "Playback Speed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        // Preset buttons grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(presets) { speed ->
                val isSelected = speed == sliderSpeed
                FilledTonalButton(
                    onClick = {
                        sliderSpeed = speed
                        onSelect(speed)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        text = formatSpeed(speed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current speed display
        Text(
            text = formatSpeed(sliderSpeed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fine-tune slider (0.5x to 3.0x, snapped to 0.1 increments)
        Slider(
            value = sliderSpeed,
            onValueChange = { raw ->
                sliderSpeed = (Math.round(raw * 10f) / 10f)
            },
            onValueChangeFinished = {
                onSelect(sliderSpeed)
                onDismiss()
            },
            valueRange = 0.5f..3.0f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Range labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0.5x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "3.0x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

Note: `mutableFloatStateOf` and `Slider`/`SliderDefaults` are already imported in PlayerScreen.kt.

**Step 2: Build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Run player tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :feature:player:testDebugUnitTest`
Expected: All tests pass

**Step 4: Commit**

```bash
git add feature/player/src/main/java/com/podbelly/feature/player/PlayerScreen.kt
git commit -m "feat: hybrid speed picker with preset grid and fine-tune slider"
```

---

## Task 3: Fix library image aspect ratio

**Files:**
- Modify: `app/src/main/java/com/podbelly/ui/LibraryScreen.kt` (PodcastGridItem, line ~296-299)

**Step 1: Add aspectRatio modifier**

In `PodcastGridItem`, change:

```kotlin
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
```

to:

```kotlin
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp)),
```

This requires adding `import androidx.compose.foundation.layout.aspectRatio` if not already present.

**Step 2: Build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/podbelly/ui/LibraryScreen.kt
git commit -m "fix: enforce square aspect ratio on library grid artwork"
```

---

## Task 4: Refresh on resume with 15-minute cooldown

**Files:**
- Modify: `app/src/main/java/com/podbelly/AppViewModel.kt`
- Modify: `app/src/main/java/com/podbelly/MainActivity.kt`

**Step 1: Add lastRefreshTime tracking to AppViewModel**

In `AppViewModel`, add a `lastRefreshTime` field and expose a method for lifecycle-triggered refresh:

Replace the full `AppViewModel` class with:

```kotlin
@HiltViewModel
class AppViewModel @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val searchRepository: PodcastSearchRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshResult = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val refreshResult: SharedFlow<Int> = _refreshResult.asSharedFlow()

    private var lastRefreshTime = 0L

    init {
        refreshFeeds()
    }

    /**
     * Called from lifecycle observer when the app comes to the foreground.
     * Only refreshes if 15+ minutes have passed since the last refresh.
     */
    fun refreshIfStale() {
        val elapsed = System.currentTimeMillis() - lastRefreshTime
        val fifteenMinutes = 15 * 60 * 1000L
        if (elapsed >= fifteenMinutes) {
            refreshFeeds()
        }
    }

    fun refreshFeeds() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true

        viewModelScope.launch {
            var newEpisodeCount = 0
            try {
                val podcasts: List<PodcastEntity> = podcastDao.getAll().first()
                val semaphore = Semaphore(5)
                val insertCounts = java.util.concurrent.atomic.AtomicInteger(0)
                podcasts.map { podcast ->
                    launch {
                        semaphore.withPermit {
                            try {
                                val rssFeed = searchRepository.fetchFeed(podcast.feedUrl)

                                val episodeEntities = rssFeed.episodes.map { rssEpisode ->
                                    EpisodeEntity(
                                        podcastId = podcast.id,
                                        guid = rssEpisode.guid,
                                        title = rssEpisode.title,
                                        description = rssEpisode.description,
                                        audioUrl = rssEpisode.audioUrl,
                                        publicationDate = rssEpisode.publishedAt,
                                        durationSeconds = (rssEpisode.duration / 1000).toInt(),
                                        artworkUrl = rssEpisode.artworkUrl ?: "",
                                        fileSize = rssEpisode.fileSize
                                    )
                                }

                                val inserted = episodeDao.insertAll(episodeEntities)
                                insertCounts.addAndGet(inserted.count { it != -1L })

                                podcastDao.update(
                                    podcast.copy(lastRefreshedAt = System.currentTimeMillis())
                                )
                            } catch (_: Exception) {
                                // Skip this feed and continue with the next one.
                            }
                        }
                    }
                }.forEach { it.join() }

                newEpisodeCount = insertCounts.get()
            } finally {
                _isRefreshing.value = false
                lastRefreshTime = System.currentTimeMillis()
                _refreshResult.tryEmit(newEpisodeCount)
            }
        }
    }
}
```

**Step 2: Add lifecycle observer to MainActivity**

In `MainActivity`, add a `LifecycleEventObserver` that calls `appViewModel.refreshIfStale()` on `ON_START`. Since `AppViewModel` is scoped to the nav host, we need to access it via the activity's ViewModelStore.

Add these imports at the top of `MainActivity.kt`:

```kotlin
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
```

Add the viewModel property and lifecycle observer to the `MainActivity` class:

```kotlin
    private val appViewModel: AppViewModel by viewModels()

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            appViewModel.refreshIfStale()
        }
    }
```

In `onCreate`, after `enableEdgeToEdge()`, add:

```kotlin
        lifecycle.addObserver(lifecycleObserver)
```

In `onDestroy`, add before `super`:

```kotlin
        lifecycle.removeObserver(lifecycleObserver)
```

**Step 3: Build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Run app tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :app:testDebugUnitTest`
Expected: All tests pass

**Step 5: Commit**

```bash
git add app/src/main/java/com/podbelly/AppViewModel.kt app/src/main/java/com/podbelly/MainActivity.kt
git commit -m "feat: refresh feeds on app resume with 15-minute cooldown"
```

---

## Task 5: Move refresh result to animated top banner

**Files:**
- Modify: `app/src/main/java/com/podbelly/navigation/PodbellNavHost.kt`

**Step 1: Replace snackbar with top banner state**

In `PodbellNavHost`, add these imports (if not already present):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
```

Replace the snackbar-related code. Change:

```kotlin
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appViewModel.refreshResult.collect { newCount ->
            val message = if (newCount > 0) {
                "$newCount new episode${if (newCount == 1) "" else "s"} found"
            } else {
                "Everything up to date"
            }
            snackbarHostState.showSnackbar(message)
        }
    }
```

to:

```kotlin
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        appViewModel.refreshResult.collect { newCount ->
            bannerMessage = if (newCount > 0) {
                "$newCount new episode${if (newCount == 1) "" else "s"} found"
            } else {
                "Everything up to date"
            }
            delay(3000L)
            bannerMessage = null
        }
    }
```

**Step 2: Remove snackbarHost from Scaffold**

Change:

```kotlin
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
```

to (remove the line entirely, or replace with empty):

```kotlin
```

**Step 3: Add animated banner above NavHost content**

In the Scaffold's content lambda (after `{ innerPadding ->`), wrap the existing `NavHost` in a `Column` with the banner above it:

Change:

```kotlin
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
```

to:

```kotlin
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated refresh result banner
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = bannerMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
```

Close the wrapping `Column` after the `NavHost` closing brace. Find the `NavHost` closing brace (the one that closes all the `composable { }` blocks) and add `}` after it to close the `Column`.

Also add the missing import:

```kotlin
import androidx.compose.ui.text.style.TextAlign
```

**Step 4: Remove unused SnackbarHostState import if needed**

Remove `import androidx.compose.material3.SnackbarHost` and `import androidx.compose.material3.SnackbarHostState` if they're no longer used.

**Step 5: Build**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/podbelly/navigation/PodbellNavHost.kt
git commit -m "feat: replace bottom snackbar with animated top refresh banner"
```

---

## Task 6: Final verification

**Step 1: Run full test suite**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`
Expected: All tests pass

**Step 2: Record Paparazzi baselines**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew recordPaparazziDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Install on device**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew installDebug`
Expected: Installed successfully

**Step 4: Push**

```bash
git push
```
