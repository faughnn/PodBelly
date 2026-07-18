package com.podbelly.core.database.di

import android.content.Context
import androidx.room.Room
import com.podbelly.core.database.PodbellDatabase
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.DuplicateMergeDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.dao.ListeningSessionDao
import com.podbelly.core.database.dao.PodcastDao
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
        )
            .addMigrations(
                PodbellDatabase.MIGRATION_1_2,
                PodbellDatabase.MIGRATION_2_3,
                PodbellDatabase.MIGRATION_3_4,
                PodbellDatabase.MIGRATION_4_5,
                PodbellDatabase.MIGRATION_5_6,
                PodbellDatabase.MIGRATION_6_7,
                PodbellDatabase.MIGRATION_7_8,
                PodbellDatabase.MIGRATION_8_9,
            )
            .build()
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
    fun provideListeningSessionDao(database: PodbellDatabase): ListeningSessionDao {
        return database.listeningSessionDao()
    }

    @Provides
    @Singleton
    fun provideDuplicateMergeDao(database: PodbellDatabase): DuplicateMergeDao {
        return database.duplicateMergeDao()
    }

    @Provides
    @Singleton
    fun provideDownloadErrorDao(database: PodbellDatabase): DownloadErrorDao {
        return database.downloadErrorDao()
    }
}
