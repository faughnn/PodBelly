package com.podbelly.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        // Artwork gets its own dispatcher and connection pool. Sharing the feed
        // client meant image requests queued behind the 32-way feed refresh at app
        // open, leaving the whole home screen on placeholder icons until the
        // refresh wound down.
        val imageClient = okHttpClient.newBuilder()
            .dispatcher(Dispatcher())
            .connectionPool(ConnectionPool())
            .build()
        return ImageLoader.Builder(context)
            .crossfade(true)
            .callFactory(imageClient)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("ImageCache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
