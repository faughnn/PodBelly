package com.podbelly.core.database.di

import android.content.Context
import androidx.room.Room
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.dao.QueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PodbellDatabase {
        return Room.databaseBuilder(
            context,
            PodbellDatabase::class.java,
            "podbelly.db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePodcastDao(database: PodbellDatabase): PodcastDao {
        return database.podcastDao()
    }

    @Provides
    @Singleton
    fun provideEpisodeDao(database: PodbellDatabase): EpisodeDao {
        return database.episodeDao()
    }

    @Provides
    @Singleton
    fun provideQueueDao(database: PodbellDatabase): QueueDao {
        return database.queueDao()
    }
}
