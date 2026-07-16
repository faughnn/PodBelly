package com.podbelly.core.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class LibrarySortOrder {
    NAME_A_TO_Z,
    RECENTLY_ADDED,
    EPISODE_COUNT,
    MOST_RECENT_EPISODE,
    MOST_LISTENED;

    companion object {
        fun fromString(value: String): LibrarySortOrder {
            return entries.firstOrNull { it.name == value } ?: NAME_A_TO_Z
        }
    }
}

enum class DownloadsSortOrder {
    DATE_NEWEST,
    DATE_OLDEST,
    NAME_A_TO_Z,
    PODCAST_NAME;

    companion object {
        fun fromString(value: String): DownloadsSortOrder {
            return entries.firstOrNull { it.name == value } ?: DATE_NEWEST
        }
    }
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    OLED_DARK,
    HIGH_CONTRAST;

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

/** Kept as a typealias for backward-compatibility with existing references. */
@Deprecated("Use AppTheme instead", ReplaceWith("AppTheme"))
typealias DarkThemeMode = AppTheme

enum class LibraryViewMode {
    GRID,
    LIST;

    companion object {
        fun fromString(value: String): LibraryViewMode {
            return entries.firstOrNull { it.name == value } ?: GRID
        }
    }
}

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val FEED_REFRESH_INTERVAL_MINUTES = intPreferencesKey("feed_refresh_interval_minutes")
        val AUTO_DOWNLOAD_ENABLED = booleanPreferencesKey("auto_download_enabled")
        val AUTO_DOWNLOAD_EPISODE_COUNT = intPreferencesKey("auto_download_episode_count")
        val AUTO_DELETE_PLAYED = booleanPreferencesKey("auto_delete_played")
        val DOWNLOAD_ON_WIFI_ONLY = booleanPreferencesKey("download_on_wifi_only")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
        val DOWNLOADS_SORT_ORDER = stringPreferencesKey("downloads_sort_order")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val PAUSED_AT = longPreferencesKey("paused_at")
        val QUEUE_ENABLED = booleanPreferencesKey("queue_enabled")
        val LAST_SEEN_VERSION_CODE = intPreferencesKey("last_seen_version_code")
        val HOME_NEW_EPISODES_CUTOFF = longPreferencesKey("home_new_episodes_cutoff")
        val HOME_NEW_DISMISSED_AT = longPreferencesKey("home_new_dismissed_at")
        val LAST_FEED_REFRESH_AT = longPreferencesKey("last_feed_refresh_at")
        val CHART_COUNTRY = stringPreferencesKey("chart_country")
    }

    // ── Flows ────────────────────────────────────────────────────────────

    val feedRefreshIntervalMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.FEED_REFRESH_INTERVAL_MINUTES] ?: 60
    }

    val autoDownloadEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DOWNLOAD_ENABLED] ?: false
    }

    val autoDownloadEpisodeCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DOWNLOAD_EPISODE_COUNT] ?: 3
    }

    val autoDeletePlayed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DELETE_PLAYED] ?: false
    }

    val downloadOnWifiOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DOWNLOAD_ON_WIFI_ONLY] ?: true
    }

    val appTheme: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.fromString(prefs[Keys.DARK_THEME_MODE] ?: AppTheme.SYSTEM.name)
    }

    /** Backward-compatible alias. */
    val darkThemeMode: Flow<AppTheme> get() = appTheme

    val playbackSpeed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.PLAYBACK_SPEED] ?: 1.0f
    }

    val skipSilence: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SKIP_SILENCE] ?: false
    }

    val volumeBoost: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.VOLUME_BOOST] ?: false
    }

    val sleepTimerMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_TIMER_MINUTES] ?: 0
    }

    val librarySortOrder: Flow<LibrarySortOrder> = dataStore.data.map { prefs ->
        LibrarySortOrder.fromString(prefs[Keys.LIBRARY_SORT_ORDER] ?: LibrarySortOrder.NAME_A_TO_Z.name)
    }

    val downloadsSortOrder: Flow<DownloadsSortOrder> = dataStore.data.map { prefs ->
        DownloadsSortOrder.fromString(prefs[Keys.DOWNLOADS_SORT_ORDER] ?: DownloadsSortOrder.DATE_NEWEST.name)
    }

    val libraryViewMode: Flow<LibraryViewMode> = dataStore.data.map { prefs ->
        LibraryViewMode.fromString(prefs[Keys.LIBRARY_VIEW_MODE] ?: LibraryViewMode.GRID.name)
    }

    val pausedAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.PAUSED_AT] ?: 0L
    }

    val queueEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.QUEUE_ENABLED] ?: true
    }

    /**
     * Episodes with addedAt above this value count as "new" on Home. Advanced to
     * the highest addedAt the user has had on screen, so the New section dissolves
     * on the next visit once its contents have been seen.
     */
    val homeNewEpisodesCutoff: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.HOME_NEW_EPISODES_CUTOFF] ?: 0L
    }

    /**
     * When the user last explicitly dismissed the New section by tapping its
     * header (epoch ms, 0 = never). Unlike [homeNewEpisodesCutoff], this is a
     * hard cutoff: the recency floor does not resurrect episodes below it.
     */
    val homeNewDismissedAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.HOME_NEW_DISMISSED_AT] ?: 0L
    }

    /**
     * ISO country code for the Discover top charts ("ie", "us", ...). Blank means
     * "follow the device locale".
     */
    val chartCountry: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CHART_COUNTRY] ?: ""
    }

    /** When feeds last refreshed successfully (epoch ms, 0 = never). */
    val lastFeedRefreshAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_FEED_REFRESH_AT] ?: 0L
    }

    // ── Setters ──────────────────────────────────────────────────────────

    suspend fun setFeedRefreshIntervalMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.FEED_REFRESH_INTERVAL_MINUTES] = minutes
        }
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_DOWNLOAD_ENABLED] = enabled
        }
    }

    suspend fun setAutoDownloadEpisodeCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_DOWNLOAD_EPISODE_COUNT] = count
        }
    }

    suspend fun setAutoDeletePlayed(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_DELETE_PLAYED] = enabled
        }
    }

    suspend fun setDownloadOnWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DOWNLOAD_ON_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME_MODE] = theme.name
        }
    }

    /** Backward-compatible alias. */
    suspend fun setDarkThemeMode(mode: AppTheme) = setAppTheme(mode)

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SKIP_SILENCE] = enabled
        }
    }

    suspend fun setVolumeBoost(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.VOLUME_BOOST] = enabled
        }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.SLEEP_TIMER_MINUTES] = minutes
        }
    }

    suspend fun setLibrarySortOrder(sortOrder: LibrarySortOrder) {
        dataStore.edit { prefs ->
            prefs[Keys.LIBRARY_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun setDownloadsSortOrder(sortOrder: DownloadsSortOrder) {
        dataStore.edit { prefs ->
            prefs[Keys.DOWNLOADS_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun setLibraryViewMode(viewMode: LibraryViewMode) {
        dataStore.edit { prefs ->
            prefs[Keys.LIBRARY_VIEW_MODE] = viewMode.name
        }
    }

    suspend fun setPausedAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.PAUSED_AT] = timestamp
        }
    }

    suspend fun setQueueEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.QUEUE_ENABLED] = enabled
        }
    }

    suspend fun getLastSeenVersionCode(): Int {
        return dataStore.data.first()[Keys.LAST_SEEN_VERSION_CODE] ?: 0
    }

    suspend fun setLastSeenVersionCode(code: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SEEN_VERSION_CODE] = code
        }
    }

    suspend fun setHomeNewEpisodesCutoff(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.HOME_NEW_EPISODES_CUTOFF] = timestamp
        }
    }

    suspend fun setChartCountry(countryCode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CHART_COUNTRY] = countryCode
        }
    }

    suspend fun setHomeNewDismissedAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.HOME_NEW_DISMISSED_AT] = timestamp
        }
    }

    suspend fun setLastFeedRefreshAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_FEED_REFRESH_AT] = timestamp
        }
    }
}
