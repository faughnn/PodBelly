# Enhanced Listening Stats Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add streaks, time-based breakdown, completion rates, and listening habits to the existing StatsScreen.

**Architecture:** All data derived from existing `ListeningSessionEntity` via new DAO queries. Streak logic and completion rate aggregation computed in `StatsViewModel`. New cards added to existing `StatsScreen` LazyColumn.

**Tech Stack:** Room (DAO queries), Kotlin Flow, Jetpack Compose, Hilt, Turbine + MockK + Robolectric for tests.

---

### Task 1: Add time-based DAO queries

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt`
- Test: `core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt`

**Step 1: Write failing tests for `getListenedMsSince`**

Add to `ListeningSessionDaoTest.kt` after the last test (line 318):

```kotlin
@Test
fun `getListenedMsSince returns total for sessions after threshold`() = runTest {
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = 1000L, listenedMs = 60000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId2, podcastId = podcastId1,
            startedAt = 5000L, listenedMs = 30000L,
        )
    )

    listeningSessionDao.getListenedMsSince(3000L).test {
        assertEquals(30000L, awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}

@Test
fun `getListenedMsSince returns zero when no sessions after threshold`() = runTest {
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = 1000L, listenedMs = 60000L,
        )
    )

    listeningSessionDao.getListenedMsSince(5000L).test {
        assertEquals(0L, awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:database:testDebugUnitTest --tests "com.podbelly.core.database.dao.ListeningSessionDaoTest" -v`
Expected: FAIL — `getListenedMsSince` not defined

**Step 3: Implement `getListenedMsSince` in ListeningSessionDao**

Add to `ListeningSessionDao.kt` after `getTotalSilenceTrimmedMs()` (after line 54):

```kotlin
@Query("SELECT COALESCE(SUM(listenedMs), 0) FROM listening_sessions WHERE startedAt >= :since")
fun getListenedMsSince(since: Long): Flow<Long>
```

**Step 4: Run tests to verify they pass**

Run: same command as step 2
Expected: PASS

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt \
       core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt
git commit -m "feat: add getListenedMsSince DAO query with tests"
```

---

### Task 2: Add listening dates DAO query (for streaks)

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt`
- Test: `core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt`

**Step 1: Write failing tests for `getListeningDays`**

Add to `ListeningSessionDaoTest.kt`:

```kotlin
@Test
fun `getListeningDays returns distinct epoch days with activity`() = runTest {
    val day1Start = 86400000L * 100  // day 100
    val day2Start = 86400000L * 102  // day 102
    // Two sessions on day 100, one on day 102
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = day1Start, listenedMs = 60000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId2, podcastId = podcastId1,
            startedAt = day1Start + 3600000L, listenedMs = 30000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId3, podcastId = podcastId2,
            startedAt = day2Start, listenedMs = 45000L,
        )
    )

    listeningSessionDao.getListeningDays().test {
        val days = awaitItem()
        assertEquals(2, days.size)
        assertEquals(100L, days[0])
        assertEquals(102L, days[1])
        cancelAndConsumeRemainingEvents()
    }
}

@Test
fun `getListeningDays returns empty when no sessions`() = runTest {
    listeningSessionDao.getListeningDays().test {
        assertEquals(emptyList<Long>(), awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :core:database:testDebugUnitTest --tests "com.podbelly.core.database.dao.ListeningSessionDaoTest" -v`
Expected: FAIL

**Step 3: Implement `getListeningDays`**

Add to `ListeningSessionDao.kt`:

```kotlin
@Query("SELECT DISTINCT startedAt / 86400000 AS epochDay FROM listening_sessions ORDER BY epochDay ASC")
fun getListeningDays(): Flow<List<Long>>
```

**Step 4: Run tests to verify they pass**

Expected: PASS

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt \
       core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt
git commit -m "feat: add getListeningDays DAO query with tests"
```

---

### Task 3: Add listening habits DAO queries

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt`
- Test: `core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt`

**Step 1: Write failing tests for average session length, day-of-week, and hour-of-day**

Add data classes before the `@Dao` interface in `ListeningSessionDao.kt` (needed for the test to compile):

```kotlin
data class DayOfWeekStat(
    val dayOfWeek: Int,
    val totalListenedMs: Long,
)

data class HourOfDayStat(
    val hour: Int,
    val totalListenedMs: Long,
)
```

Add tests to `ListeningSessionDaoTest.kt`:

```kotlin
@Test
fun `getAverageSessionLengthMs returns average across all sessions`() = runTest {
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = 1000L, listenedMs = 60000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId2, podcastId = podcastId1,
            startedAt = 2000L, listenedMs = 30000L,
        )
    )

    listeningSessionDao.getAverageSessionLengthMs().test {
        assertEquals(45000L, awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}

@Test
fun `getAverageSessionLengthMs returns zero when no sessions`() = runTest {
    listeningSessionDao.getAverageSessionLengthMs().test {
        assertEquals(0L, awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}

@Test
fun `getListeningMsByDayOfWeek groups by day of week`() = runTest {
    // Thursday = day 4 (epoch day 0 = Thursday 1970-01-01)
    val thursdayMs = 86400000L * 7  // day 7 = also Thursday
    val fridayMs = 86400000L * 8   // day 8 = Friday

    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = thursdayMs, listenedMs = 60000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId2, podcastId = podcastId1,
            startedAt = fridayMs, listenedMs = 30000L,
        )
    )

    listeningSessionDao.getListeningMsByDayOfWeek().test {
        val stats = awaitItem()
        assertEquals(2, stats.size)
        cancelAndConsumeRemainingEvents()
    }
}

@Test
fun `getListeningMsByHourOfDay groups by hour`() = runTest {
    val hour10Ms = 86400000L * 100 + 10 * 3600000L  // day 100, 10:00 UTC
    val hour14Ms = 86400000L * 100 + 14 * 3600000L  // day 100, 14:00 UTC

    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = hour10Ms, listenedMs = 60000L,
        )
    )
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId2, podcastId = podcastId1,
            startedAt = hour14Ms, listenedMs = 30000L,
        )
    )

    listeningSessionDao.getListeningMsByHourOfDay().test {
        val stats = awaitItem()
        assertEquals(2, stats.size)
        cancelAndConsumeRemainingEvents()
    }
}
```

**Step 2: Run tests to verify they fail**

Expected: FAIL

**Step 3: Implement the three queries**

Add to `ListeningSessionDao.kt`:

```kotlin
@Query("SELECT COALESCE(AVG(listenedMs), 0) FROM listening_sessions")
fun getAverageSessionLengthMs(): Flow<Long>

@Query(
    """
    SELECT CAST((startedAt / 86400000 + 4) % 7 AS INTEGER) AS dayOfWeek,
           SUM(listenedMs) AS totalListenedMs
    FROM listening_sessions
    GROUP BY dayOfWeek
    ORDER BY totalListenedMs DESC
    """
)
fun getListeningMsByDayOfWeek(): Flow<List<DayOfWeekStat>>

@Query(
    """
    SELECT CAST((startedAt % 86400000) / 3600000 AS INTEGER) AS hour,
           SUM(listenedMs) AS totalListenedMs
    FROM listening_sessions
    GROUP BY hour
    ORDER BY totalListenedMs DESC
    """
)
fun getListeningMsByHourOfDay(): Flow<List<HourOfDayStat>>
```

Note on day-of-week: `(epochDay + 4) % 7` maps to 0=Monday..6=Sunday because epoch day 0 (1970-01-01) was a Thursday (day index 3 from Monday). `startedAt / 86400000` gives epoch day, adding 4 then mod 7 gives 0=Mon.

**Step 4: Run tests to verify they pass**

Expected: PASS

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt \
       core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt
git commit -m "feat: add listening habits DAO queries with tests"
```

---

### Task 4: Add episode completion DAO query

**Files:**
- Modify: `core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt`
- Test: `core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt`

**Step 1: Write failing test**

Add data class before `@Dao` in `ListeningSessionDao.kt`:

```kotlin
data class EpisodeCompletionStat(
    val episodeId: Long,
    val totalListenedMs: Long,
    val durationMs: Long,
)
```

Add test to `ListeningSessionDaoTest.kt`. Need to use episodes with known durations — update `createEpisode` to accept durationSeconds:

```kotlin
// Update createEpisode helper to accept durationSeconds:
private fun createEpisode(
    podcastId: Long,
    guid: String,
    title: String = "Episode",
    durationSeconds: Int = 0,
) = EpisodeEntity(
    podcastId = podcastId, guid = guid, title = title,
    description = "Desc", audioUrl = "https://example.com/$guid.mp3",
    publicationDate = 1700000000000L,
    durationSeconds = durationSeconds,
)
```

Then update the `setUp()` seed data to include durations:

```kotlin
val eps = episodeDao.insertAll(listOf(
    createEpisode(podcastId = podcastId1, guid = "ep1", title = "Episode 1", durationSeconds = 600),
    createEpisode(podcastId = podcastId1, guid = "ep2", title = "Episode 2", durationSeconds = 1200),
    createEpisode(podcastId = podcastId2, guid = "ep3", title = "Episode 3", durationSeconds = 1800),
))
```

Add test:

```kotlin
@Test
fun `getEpisodeCompletionStats returns listened vs duration per episode`() = runTest {
    // Episode 1: 600s = 600000ms duration, listened 540000ms (90%)
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId1, podcastId = podcastId1,
            startedAt = 1000L, listenedMs = 540000L,
        )
    )
    // Episode 3: 1800s = 1800000ms duration, listened 200000ms (~11%)
    listeningSessionDao.insert(
        ListeningSessionEntity(
            episodeId = episodeId3, podcastId = podcastId2,
            startedAt = 2000L, listenedMs = 200000L,
        )
    )

    listeningSessionDao.getEpisodeCompletionStats().test {
        val stats = awaitItem()
        assertEquals(2, stats.size)

        val ep1 = stats.find { it.episodeId == episodeId1 }!!
        assertEquals(540000L, ep1.totalListenedMs)
        assertEquals(600000L, ep1.durationMs)

        val ep3 = stats.find { it.episodeId == episodeId3 }!!
        assertEquals(200000L, ep3.totalListenedMs)
        assertEquals(1800000L, ep3.durationMs)

        cancelAndConsumeRemainingEvents()
    }
}
```

**Step 2: Run tests to verify they fail**

Expected: FAIL

**Step 3: Implement `getEpisodeCompletionStats`**

Add to `ListeningSessionDao.kt`:

```kotlin
@Query(
    """
    SELECT ls.episodeId,
           SUM(ls.listenedMs) AS totalListenedMs,
           CAST(e.durationSeconds AS INTEGER) * 1000 AS durationMs
    FROM listening_sessions ls
    INNER JOIN episodes e ON ls.episodeId = e.id
    WHERE e.durationSeconds > 0
    GROUP BY ls.episodeId
    """
)
fun getEpisodeCompletionStats(): Flow<List<EpisodeCompletionStat>>
```

**Step 4: Run tests to verify they pass**

Expected: PASS

**Step 5: Commit**

```bash
git add core/database/src/main/java/com/podbelly/core/database/dao/ListeningSessionDao.kt \
       core/database/src/test/java/com/podbelly/core/database/dao/ListeningSessionDaoTest.kt
git commit -m "feat: add episode completion stats DAO query with tests"
```

---

### Task 5: Update StatsViewModel with new stats

**Files:**
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/StatsViewModel.kt`
- Test: `feature/settings/src/test/java/com/podbelly/feature/settings/StatsViewModelTest.kt`

**Step 1: Write failing tests for new UI state fields**

Update `StatsUiState` to include new fields (needed for tests to compile):

```kotlin
data class StatsUiState(
    val totalListenedMs: Long = 0L,
    val timeSavedBySpeedMs: Long = 0L,
    val silenceTrimmedMs: Long = 0L,
    // Streaks
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    // Time-based
    val listenedThisWeekMs: Long = 0L,
    val listenedThisMonthMs: Long = 0L,
    // Listening habits
    val averageSessionLengthMs: Long = 0L,
    val mostActiveDay: String = "",
    val mostActiveHour: String = "",
    // Completion rates
    val averageCompletionPercent: Int = 0,
    val finishedEpisodes: Int = 0,
    val abandonedEpisodes: Int = 0,
    // Existing
    val mostListenedPodcasts: List<PodcastListeningStat> = emptyList(),
    val mostListenedEpisodes: List<EpisodeListeningStat> = emptyList(),
    val mostDownloadedPodcasts: List<PodcastDownloadStat> = emptyList(),
)
```

Add mock flows and setup in `StatsViewModelTest.kt`:

```kotlin
// New mock flows (add as class fields)
private val listenedThisWeekFlow = MutableStateFlow(0L)
private val listenedThisMonthFlow = MutableStateFlow(0L)
private val listeningDaysFlow = MutableStateFlow<List<Long>>(emptyList())
private val averageSessionFlow = MutableStateFlow(0L)
private val dayOfWeekFlow = MutableStateFlow<List<DayOfWeekStat>>(emptyList())
private val hourOfDayFlow = MutableStateFlow<List<HourOfDayStat>>(emptyList())
private val completionStatsFlow = MutableStateFlow<List<EpisodeCompletionStat>>(emptyList())
```

Add to `setUp()`:

```kotlin
every { listeningSessionDao.getListenedMsSince(any()) } returns listenedThisWeekFlow
every { listeningSessionDao.getListeningDays() } returns listeningDaysFlow
every { listeningSessionDao.getAverageSessionLengthMs() } returns averageSessionFlow
every { listeningSessionDao.getListeningMsByDayOfWeek() } returns dayOfWeekFlow
every { listeningSessionDao.getListeningMsByHourOfDay() } returns hourOfDayFlow
every { listeningSessionDao.getEpisodeCompletionStats() } returns completionStatsFlow
```

Note: `getListenedMsSince` is called twice (7-day and 30-day). We'll need two separate flows. Update:

```kotlin
private val listenedThisWeekFlow = MutableStateFlow(0L)
private val listenedThisMonthFlow = MutableStateFlow(0L)

// In setUp, match on the argument range:
// Since we can't easily match on exact timestamp, use returnsMany or match ordering.
// Simpler: the ViewModel will call getListenedMsSince with two different thresholds.
// MockK relaxed will return listenedThisWeekFlow for any call.
// Better approach: use two separate queries or answer based on arg.
// Simplest: use answers {} block:
every { listeningSessionDao.getListenedMsSince(any()) } answers {
    val since = firstArg<Long>()
    // 7 days ~ 604800000ms. Use a threshold to distinguish.
    if (System.currentTimeMillis() - since < 8 * 86400000L) listenedThisWeekFlow
    else listenedThisMonthFlow
}
```

Add tests:

```kotlin
@Test
fun `streak calculation from listening days`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {
        awaitItem() // initial

        // Days 10, 11, 12 (3-day streak), gap, then 15, 16 (2-day streak)
        // If today is day 16, current=2, longest=3
        // We'll test the logic by setting days and checking output
        val today = System.currentTimeMillis() / 86400000L
        listeningDaysFlow.value = listOf(today - 5, today - 4, today - 3, today - 1, today)

        var state = awaitItem()
        // current streak = 2 (today + yesterday)
        assertEquals(2, state.currentStreak)
        // longest streak = 3 (days -5, -4, -3)
        assertEquals(3, state.longestStreak)
    }
}

@Test
fun `completion rates calculated from episode stats`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {
        awaitItem()

        completionStatsFlow.value = listOf(
            EpisodeCompletionStat(1L, 900000L, 1000000L),  // 90% - finished
            EpisodeCompletionStat(2L, 500000L, 1000000L),  // 50% - neither
            EpisodeCompletionStat(3L, 100000L, 1000000L),  // 10% - abandoned
        )

        var state = awaitItem()
        while (state.averageCompletionPercent == 0) state = awaitItem()

        assertEquals(50, state.averageCompletionPercent)
        assertEquals(1, state.finishedEpisodes)
        assertEquals(1, state.abandonedEpisodes)
    }
}

@Test
fun `most active day derived from day of week stats`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {
        awaitItem()

        dayOfWeekFlow.value = listOf(
            DayOfWeekStat(dayOfWeek = 0, totalListenedMs = 500000L), // Monday
            DayOfWeekStat(dayOfWeek = 4, totalListenedMs = 300000L), // Friday
        )

        var state = awaitItem()
        while (state.mostActiveDay.isEmpty()) state = awaitItem()

        assertEquals("Monday", state.mostActiveDay)
    }
}

@Test
fun `most active hour derived from hour of day stats`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {
        awaitItem()

        hourOfDayFlow.value = listOf(
            HourOfDayStat(hour = 8, totalListenedMs = 500000L),
            HourOfDayStat(hour = 20, totalListenedMs = 300000L),
        )

        var state = awaitItem()
        while (state.mostActiveHour.isEmpty()) state = awaitItem()

        assertEquals("8 AM", state.mostActiveHour)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :feature:settings:testDebugUnitTest --tests "com.podbelly.feature.settings.StatsViewModelTest" -v`
Expected: FAIL

**Step 3: Implement the ViewModel changes**

Update `StatsViewModel.kt`. The combine chain needs to incorporate all the new flows. Since `combine()` supports max 5 flows, chain multiple combines:

```kotlin
@HiltViewModel
class StatsViewModel @Inject constructor(
    listeningSessionDao: ListeningSessionDao,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        listeningSessionDao.getTotalListenedMs(),
        listeningSessionDao.getTimeSavedBySpeed(),
        listeningSessionDao.getTotalSilenceTrimmedMs(),
        listeningSessionDao.getMostListenedPodcasts(10),
        listeningSessionDao.getMostListenedEpisodes(10),
    ) { totalListened, timeSavedBySpeed, silenceTrimmed, mostPodcasts, mostEpisodes ->
        PartialBase(totalListened, timeSavedBySpeed, silenceTrimmed, mostPodcasts, mostEpisodes)
    }.combine(
        listeningSessionDao.getMostDownloadedPodcasts(10),
    ) { base, mostDownloaded ->
        base to mostDownloaded
    }.combine(combine(
        listeningSessionDao.getListenedMsSince(System.currentTimeMillis() - 7 * 86400000L),
        listeningSessionDao.getListenedMsSince(System.currentTimeMillis() - 30 * 86400000L),
        listeningSessionDao.getListeningDays(),
        listeningSessionDao.getAverageSessionLengthMs(),
        listeningSessionDao.getListeningMsByDayOfWeek(),
    ) { week, month, days, avgSession, dayOfWeek ->
        PartialNew(week, month, days, avgSession, dayOfWeek)
    }) { (base, mostDownloaded), newStats ->
        Triple(base, mostDownloaded, newStats)
    }.combine(combine(
        listeningSessionDao.getListeningMsByHourOfDay(),
        listeningSessionDao.getEpisodeCompletionStats(),
    ) { hours, completion -> hours to completion }
    ) { (base, mostDownloaded, newStats), (hours, completion) ->
        val (currentStreak, longestStreak) = calculateStreaks(newStats.listeningDays)
        val (avgCompletion, finished, abandoned) = calculateCompletion(completion)
        val mostActiveDay = newStats.dayOfWeekStats.firstOrNull()?.let { dayName(it.dayOfWeek) } ?: ""
        val mostActiveHour = hours.firstOrNull()?.let { hourName(it.hour) } ?: ""

        StatsUiState(
            totalListenedMs = base.totalListenedMs,
            timeSavedBySpeedMs = base.timeSavedBySpeedMs,
            silenceTrimmedMs = base.silenceTrimmedMs,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            listenedThisWeekMs = newStats.listenedThisWeekMs,
            listenedThisMonthMs = newStats.listenedThisMonthMs,
            averageSessionLengthMs = newStats.averageSessionLengthMs,
            mostActiveDay = mostActiveDay,
            mostActiveHour = mostActiveHour,
            averageCompletionPercent = avgCompletion,
            finishedEpisodes = finished,
            abandonedEpisodes = abandoned,
            mostListenedPodcasts = base.mostListenedPodcasts,
            mostListenedEpisodes = base.mostListenedEpisodes,
            mostDownloadedPodcasts = mostDownloaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    private data class PartialBase(
        val totalListenedMs: Long,
        val timeSavedBySpeedMs: Long,
        val silenceTrimmedMs: Long,
        val mostListenedPodcasts: List<PodcastListeningStat>,
        val mostListenedEpisodes: List<EpisodeListeningStat>,
    )

    private data class PartialNew(
        val listenedThisWeekMs: Long,
        val listenedThisMonthMs: Long,
        val listeningDays: List<Long>,
        val averageSessionLengthMs: Long,
        val dayOfWeekStats: List<DayOfWeekStat>,
    )

    companion object {
        fun calculateStreaks(sortedDays: List<Long>): Pair<Int, Int> {
            if (sortedDays.isEmpty()) return 0 to 0

            val today = System.currentTimeMillis() / 86400000L
            var currentStreak = 0
            var longestStreak = 1
            var streak = 1

            for (i in sortedDays.lastIndex downTo 1) {
                if (sortedDays[i] - sortedDays[i - 1] == 1L) {
                    streak++
                } else {
                    longestStreak = maxOf(longestStreak, streak)
                    streak = 1
                }
            }
            longestStreak = maxOf(longestStreak, streak)

            // Current streak: count consecutive days ending at today or yesterday
            val lastDay = sortedDays.last()
            if (lastDay == today || lastDay == today - 1) {
                currentStreak = 1
                for (i in sortedDays.lastIndex downTo 1) {
                    if (sortedDays[i] - sortedDays[i - 1] == 1L) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }

            return currentStreak to longestStreak
        }

        fun calculateCompletion(stats: List<EpisodeCompletionStat>): Triple<Int, Int, Int> {
            if (stats.isEmpty()) return Triple(0, 0, 0)

            val percentages = stats.map { stat ->
                ((stat.totalListenedMs.toDouble() / stat.durationMs) * 100).coerceAtMost(100.0)
            }
            val avg = percentages.average().toInt()
            val finished = percentages.count { it >= 90.0 }
            val abandoned = percentages.count { it < 25.0 }

            return Triple(avg, finished, abandoned)
        }

        fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            5 -> "Saturday"
            6 -> "Sunday"
            else -> ""
        }

        fun hourName(hour: Int): String = when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
```

**Step 4: Run tests to verify they pass**

Expected: PASS

**Step 5: Commit**

```bash
git add feature/settings/src/main/java/com/podbelly/feature/settings/StatsViewModel.kt \
       feature/settings/src/test/java/com/podbelly/feature/settings/StatsViewModelTest.kt
git commit -m "feat: add streaks, habits, and completion stats to StatsViewModel"
```

---

### Task 6: Update StatsScreen UI

**Files:**
- Modify: `feature/settings/src/main/java/com/podbelly/feature/settings/StatsScreen.kt`

**Step 1: Add new card sections to the LazyColumn**

Insert after the existing summary cards (after line 109, before the "Most Listened Podcasts" section). Add these new sections:

```kotlin
// ── Streaks ────────────────────────────────────────────
if (uiState.currentStreak > 0 || uiState.longestStreak > 0) {
    item { SectionHeader(title = "Streaks") }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsSummaryCard(
                title = "Current Streak",
                value = "${uiState.currentStreak} day${if (uiState.currentStreak != 1) "s" else ""}",
                modifier = Modifier.weight(1f),
            )
            StatsSummaryCard(
                title = "Longest Streak",
                value = "${uiState.longestStreak} day${if (uiState.longestStreak != 1) "s" else ""}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── This Week / This Month ─────────────────────────────
if (uiState.listenedThisWeekMs > 0 || uiState.listenedThisMonthMs > 0) {
    item { SectionHeader(title = "Recent Activity") }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsSummaryCard(
                title = "This Week",
                value = formatDurationMs(uiState.listenedThisWeekMs),
                modifier = Modifier.weight(1f),
            )
            StatsSummaryCard(
                title = "This Month",
                value = formatDurationMs(uiState.listenedThisMonthMs),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Listening Habits ───────────────────────────────────
if (uiState.averageSessionLengthMs > 0) {
    item { SectionHeader(title = "Listening Habits") }

    item {
        StatsSummaryCard(
            title = "Average Session",
            value = formatDurationMs(uiState.averageSessionLengthMs),
        )
    }

    if (uiState.mostActiveDay.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatsSummaryCard(
                    title = "Most Active Day",
                    value = uiState.mostActiveDay,
                    modifier = Modifier.weight(1f),
                )
                StatsSummaryCard(
                    title = "Peak Hour",
                    value = uiState.mostActiveHour,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Completion Rates ───────────────────────────────────
if (uiState.averageCompletionPercent > 0) {
    item { SectionHeader(title = "Completion") }

    item {
        StatsSummaryCard(
            title = "Average Completion",
            value = "${uiState.averageCompletionPercent}%",
        )
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsSummaryCard(
                title = "Finished",
                value = "${uiState.finishedEpisodes} episode${if (uiState.finishedEpisodes != 1) "s" else ""}",
                modifier = Modifier.weight(1f),
            )
            StatsSummaryCard(
                title = "Abandoned",
                value = "${uiState.abandonedEpisodes} episode${if (uiState.abandonedEpisodes != 1) "s" else ""}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
```

Also update `StatsSummaryCard` to accept an optional `modifier` parameter:

```kotlin
@Composable
internal fun StatsSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        // ... rest unchanged
    )
```

Also add the `SectionHeader` composable if it doesn't already exist as a separate function (check first — it may be inline in the LazyColumn currently). If needed:

```kotlin
@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}
```

**Step 2: Build to verify compilation**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew :feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add feature/settings/src/main/java/com/podbelly/feature/settings/StatsScreen.kt
git commit -m "feat: add streaks, habits, and completion cards to StatsScreen"
```

---

### Task 7: Run full test suite, bump version, and push

**Step 1: Run all tests**

Run: `JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot" ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

**Step 2: Fix any failures**

If any test fails, investigate and fix before proceeding.

**Step 3: Bump version in `app/build.gradle.kts`**

Change `versionCode` to 7 and `versionName` to `"1.0.6"`.

**Step 4: Commit and push**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.0.6"
git push origin develop
```
