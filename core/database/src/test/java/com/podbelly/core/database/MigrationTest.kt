package com.podbelly.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the hand-written [PodbellDatabase] migrations against real SQLite.
 *
 * The DAO tests always build a fresh latest-schema database, so without these
 * the ALTER TABLE statements that run against real devices' data never execute
 * in any test. Historical schemas are recreated by hand below (schema export is
 * off, so Room's MigrationTestHelper isn't available); that's fine for what
 * these verify — each migration's SQL runs cleanly, adds the intended columns
 * with the intended defaults, and preserves existing rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    // -----------------------------------------------------------------------
    // Historical schema (as of version 5) for the tables the migrations touch.
    // Column sets match the entities at that version: episodes before addedAt
    // (v6) / transcripts (v8) / autoDownloaded (v10); podcasts before the skip
    // settings (v7) / autoDownloadMode (v10); listening_sessions before
    // skipSavedMs (v9).
    // -----------------------------------------------------------------------

    private fun createV5Schema() {
        db.execSQL(
            """
            CREATE TABLE podcasts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                feedUrl TEXT NOT NULL,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                description TEXT NOT NULL,
                artworkUrl TEXT NOT NULL,
                link TEXT NOT NULL,
                language TEXT NOT NULL,
                lastBuildDate INTEGER NOT NULL,
                subscribed INTEGER NOT NULL DEFAULT 1,
                subscribedAt INTEGER NOT NULL,
                lastRefreshedAt INTEGER NOT NULL DEFAULT 0,
                episodeCount INTEGER NOT NULL DEFAULT 0,
                notifyNewEpisodes INTEGER NOT NULL DEFAULT 1,
                playbackSpeed REAL NOT NULL DEFAULT 0.0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE episodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                podcastId INTEGER NOT NULL,
                guid TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                audioUrl TEXT NOT NULL,
                publicationDate INTEGER NOT NULL,
                durationSeconds INTEGER NOT NULL,
                artworkUrl TEXT NOT NULL,
                played INTEGER NOT NULL DEFAULT 0,
                playbackPosition INTEGER NOT NULL DEFAULT 0,
                lastPlayedAt INTEGER NOT NULL DEFAULT 0,
                downloadPath TEXT NOT NULL DEFAULT '',
                downloadedAt INTEGER NOT NULL DEFAULT 0,
                fileSize INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX index_episodes_podcastId_guid ON episodes (podcastId, guid)"
        )
        db.execSQL(
            """
            CREATE TABLE listening_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                episodeId INTEGER NOT NULL,
                podcastId INTEGER NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL DEFAULT 0,
                listenedMs INTEGER NOT NULL DEFAULT 0,
                playbackSpeed REAL NOT NULL DEFAULT 1.0,
                silenceTrimmedMs INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun insertV5Fixtures() {
        db.execSQL(
            """
            INSERT INTO podcasts (feedUrl, title, author, description, artworkUrl, link,
                language, lastBuildDate, subscribedAt)
            VALUES ('https://a.com/feed', 'Show A', 'Author', 'Desc', '', '', 'en', 0, 1000)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO episodes (podcastId, guid, title, description, audioUrl,
                publicationDate, durationSeconds, artworkUrl, downloadPath)
            VALUES (1, 'g1', 'Episode 1', 'Desc', 'https://a.com/1.mp3', 1000, 60, '', '/f/1.mp3')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO listening_sessions (episodeId, podcastId, startedAt, listenedMs)
            VALUES (1, 1, 1000, 60000)
            """.trimIndent()
        )
    }

    private fun columnNames(table: String): Set<String> {
        val names = mutableSetOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names.add(cursor.getString(nameIndex))
        }
        return names
    }

    private fun queryLong(sql: String): Long =
        db.query(sql).use { cursor ->
            assertTrue("expected a row from: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun queryString(sql: String): String =
        db.query(sql).use { cursor ->
            assertTrue("expected a row from: $sql", cursor.moveToFirst())
            cursor.getString(0)
        }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    fun `migrations 5 through 10 run in sequence and preserve data`() {
        createV5Schema()
        insertV5Fixtures()

        PodbellDatabase.MIGRATION_5_6.migrate(db)
        PodbellDatabase.MIGRATION_6_7.migrate(db)
        PodbellDatabase.MIGRATION_7_8.migrate(db)
        PodbellDatabase.MIGRATION_8_9.migrate(db)
        PodbellDatabase.MIGRATION_9_10.migrate(db)

        // Every column each migration adds is present.
        val episodeColumns = columnNames("episodes")
        assertTrue("addedAt missing", "addedAt" in episodeColumns)
        assertTrue("transcriptUrl missing", "transcriptUrl" in episodeColumns)
        assertTrue("transcriptType missing", "transcriptType" in episodeColumns)
        assertTrue("autoDownloaded missing", "autoDownloaded" in episodeColumns)
        val podcastColumns = columnNames("podcasts")
        assertTrue("skipIntroSeconds missing", "skipIntroSeconds" in podcastColumns)
        assertTrue("skipOutroSeconds missing", "skipOutroSeconds" in podcastColumns)
        assertTrue("autoDownloadMode missing", "autoDownloadMode" in podcastColumns)
        assertTrue("skipSavedMs missing", "skipSavedMs" in columnNames("listening_sessions"))

        // Existing rows survive with sensible defaults in the new columns.
        assertEquals("Episode 1", queryString("SELECT title FROM episodes WHERE id = 1"))
        assertEquals("/f/1.mp3", queryString("SELECT downloadPath FROM episodes WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT addedAt FROM episodes WHERE id = 1"))
        assertEquals("", queryString("SELECT transcriptUrl FROM episodes WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT autoDownloaded FROM episodes WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT skipIntroSeconds FROM podcasts WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT autoDownloadMode FROM podcasts WHERE id = 1"))
        assertEquals(60000L, queryLong("SELECT listenedMs FROM listening_sessions WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT skipSavedMs FROM listening_sessions WHERE id = 1"))
    }

    @Test
    fun `migration 9 to 10 defaults new columns to zero on existing rows`() {
        createV5Schema()
        PodbellDatabase.MIGRATION_5_6.migrate(db)
        PodbellDatabase.MIGRATION_6_7.migrate(db)
        PodbellDatabase.MIGRATION_7_8.migrate(db)
        PodbellDatabase.MIGRATION_8_9.migrate(db)
        insertV5Fixtures()

        PodbellDatabase.MIGRATION_9_10.migrate(db)

        assertEquals(0L, queryLong("SELECT autoDownloadMode FROM podcasts WHERE id = 1"))
        assertEquals(0L, queryLong("SELECT autoDownloaded FROM episodes WHERE id = 1"))
        // The pre-existing download is still marked manual, so the keep-newest-N
        // cleanup can never see it.
        assertEquals(
            0L,
            queryLong("SELECT COUNT(*) FROM episodes WHERE autoDownloaded = 1"),
        )
    }

    @Test
    fun `migration 3 to 4 seeds lastPlayedAt from publicationDate for in-progress episodes`() {
        // v3 episodes: no lastPlayedAt yet.
        db.execSQL(
            """
            CREATE TABLE episodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                podcastId INTEGER NOT NULL,
                guid TEXT NOT NULL,
                title TEXT NOT NULL,
                publicationDate INTEGER NOT NULL,
                playbackPosition INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO episodes (podcastId, guid, title, publicationDate, playbackPosition) " +
                "VALUES (1, 'g1', 'In progress', 5000, 120)"
        )
        db.execSQL(
            "INSERT INTO episodes (podcastId, guid, title, publicationDate, playbackPosition) " +
                "VALUES (1, 'g2', 'Untouched', 6000, 0)"
        )

        PodbellDatabase.MIGRATION_3_4.migrate(db)

        assertEquals(5000L, queryLong("SELECT lastPlayedAt FROM episodes WHERE guid = 'g1'"))
        assertEquals(0L, queryLong("SELECT lastPlayedAt FROM episodes WHERE guid = 'g2'"))
    }

    @Test
    fun `migration 4 to 5 allows the same guid on different feeds but not within one`() {
        // v4 episodes: global unique index on guid alone.
        db.execSQL(
            """
            CREATE TABLE episodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                podcastId INTEGER NOT NULL,
                guid TEXT NOT NULL,
                title TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_episodes_guid ON episodes (guid)")
        db.execSQL("INSERT INTO episodes (podcastId, guid, title) VALUES (1, 'shared', 'A')")

        PodbellDatabase.MIGRATION_4_5.migrate(db)

        // A different feed may now reuse the guid...
        db.execSQL("INSERT INTO episodes (podcastId, guid, title) VALUES (2, 'shared', 'B')")
        assertEquals(2L, queryLong("SELECT COUNT(*) FROM episodes WHERE guid = 'shared'"))

        // ...but within one feed it stays unique.
        try {
            db.execSQL("INSERT INTO episodes (podcastId, guid, title) VALUES (1, 'shared', 'C')")
            fail("expected the composite unique index to reject a duplicate")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
    }
}
