package com.podbelly.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import com.podbelly.core.database.entity.QueueItemEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        QueueItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PodbellDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun queueDao(): QueueDao
}
