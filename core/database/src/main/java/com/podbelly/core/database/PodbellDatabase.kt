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
    version = 2,
    exportSchema = false
)
abstract class PodbellDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun queueDao(): QueueDao
    abstract fun listeningSessionDao(): ListeningSessionDao
    abstract fun downloadErrorDao(): DownloadErrorDao

    companion object {
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
