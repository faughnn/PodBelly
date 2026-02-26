# Enhanced Listening Stats

## Overview

Add four new stat categories to the existing StatsScreen: streaks, time-based breakdown, completion rates, and listening habits. All derived from existing `ListeningSessionEntity` data — no new tables needed.

## New Stats

### 1. Streaks
- **Current streak**: consecutive days with listening activity ending today (or yesterday)
- **Longest streak**: all-time record
- DAO query groups sessions by calendar date, streak logic computed in ViewModel

### 2. Time-based Breakdown
- **This week**: total listening time for the past 7 days
- **This month**: total listening time for the past 30 days
- DAO queries sum `listenedMs` with a `startedAt >= threshold` filter

### 3. Completion Rates
- **Average completion rate**: mean of (totalListenedMs / episodeDuration) across episodes with at least one session
- **Finished episodes**: count with >= 90% listened
- **Abandoned episodes**: count with < 25% listened
- DAO query joins `listening_sessions` (grouped by episodeId) with `episodes.duration`

### 4. Listening Habits
- **Average session length**: mean of `listenedMs` across all sessions
- **Most active day of week**: day with highest total listening (derived from `startedAt`)
- **Most active hour of day**: hour with highest total listening (derived from `startedAt`)

## UI

All new stats added to the existing `StatsScreen` as card groups. Order:
1. Existing summary cards (total time, speed saved, silence saved)
2. **Streaks** (new)
3. **Time-based breakdown** (new)
4. **Listening habits** (new)
5. **Completion rates** (new)
6. Existing top-10 lists

Reuse the existing `StatsSummaryCard` composable for all new cards.

## Data Layer Changes

### ListeningSessionDao — new queries
- `getListeningDates()`: `Flow<List<Long>>` — distinct calendar dates (as epoch day) with listening activity
- `getListenedMsSince(since: Long)`: `Flow<Long>` — total listenedMs where startedAt >= since
- `getAverageSessionLengthMs()`: `Flow<Long>` — AVG(listenedMs)
- `getListeningMsByDayOfWeek()`: `Flow<List<DayOfWeekStat>>` — sum listenedMs grouped by day-of-week
- `getListeningMsByHourOfDay()`: `Flow<List<HourOfDayStat>>` — sum listenedMs grouped by hour
- `getEpisodeCompletionStats()`: `Flow<List<EpisodeCompletionStat>>` — episodeId, totalListenedMs joined with episode duration

### StatsViewModel — new logic
- Streak calculation from listening dates (pure Kotlin, no SQL)
- Completion rate aggregation from episode completion stats
- Map day-of-week/hour integers to display strings

## Testing
- DAO tests for each new query
- ViewModel tests for streak calculation logic and completion rate aggregation
