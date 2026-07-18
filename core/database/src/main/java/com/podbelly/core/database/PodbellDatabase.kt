package com.podbelly.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.DownloadErrorEntity
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.ListeningSessionEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        QueueItemEntity::class,
        ListeningSessionEntity::class,
        DownloadErrorEntity::class,
    ],
    version = 9,
    exportSchema = false
)
abstract class PodbellDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun queueDao(): QueueDao
    abstract fun listeningSessionDao(): ListeningSessionDao
    abstract fun downloadErrorDao(): DownloadErrorDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Time saved by per-podcast intro/outro auto-skip, accumulated onto
                // the listening session it happened in (powers the Stats
                // "Time saved" breakdown).
                db.execSQL(
                    "ALTER TABLE listening_sessions ADD COLUMN skipSavedMs INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Podcasting 2.0 episode transcripts (<podcast:transcript>). Existing
                // rows start empty and are backfilled by the next feed refresh via
                // updateFeedFields.
                db.execSQL(
                    "ALTER TABLE episodes ADD COLUMN transcriptUrl TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE episodes ADD COLUMN transcriptType TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Per-podcast intro/outro auto-skip (seconds; 0 = disabled), the
                // AntennaPod "Skip introduction / ending" per-feed pattern.
                db.execSQL(
                    "ALTER TABLE podcasts ADD COLUMN skipIntroSeconds INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE podcasts ADD COLUMN skipOutroSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Records when a feed refresh first discovered the episode, powering
                // the "New" section on Home. Existing rows keep 0 so nothing floods
                // the section on first launch after the update.
                db.execSQL(
                    "ALTER TABLE episodes ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // GUIDs are only unique within a feed. Replace the global unique
                // index on guid with a composite unique index on (podcastId, guid)
                // so episodes from different feeds that share a GUID are no longer
                // silently dropped on insert.
                db.execSQL("DROP INDEX IF EXISTS index_episodes_guid")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_podcastId_guid ON episodes (podcastId, guid)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Seed lastPlayedAt with publicationDate so existing in-progress
                // episodes retain a sensible relative order on first launch.
                db.execSQL(
                    "ALTER TABLE episodes ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE episodes SET lastPlayedAt = publicationDate WHERE playbackPosition > 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE podcasts ADD COLUMN playbackSpeed REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add notifyNewEpisodes column to podcasts
                db.execSQL(
                    "ALTER TABLE podcasts ADD COLUMN notifyNewEpisodes INTEGER NOT NULL DEFAULT 1"
                )

                // Create listening_sessions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        episodeId INTEGER NOT NULL,
                        podcastId INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL DEFAULT 0,
                        listenedMs INTEGER NOT NULL DEFAULT 0,
                        playbackSpeed REAL NOT NULL DEFAULT 1.0,
                        silenceTrimmedMs INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (episodeId) REFERENCES episodes(id) ON DELETE CASCADE,
                        FOREIGN KEY (podcastId) REFERENCES podcasts(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_sessions_episodeId ON listening_sessions(episodeId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_sessions_podcastId ON listening_sessions(podcastId)"
                )

                // Create download_errors table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS download_errors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        episodeId INTEGER NOT NULL,
                        errorMessage TEXT NOT NULL,
                        errorCode INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (episodeId) REFERENCES episodes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_download_errors_episodeId ON download_errors(episodeId)"
                )
            }
        }
    }
}
