package com.podbelly.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcher
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
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
            // Coil 3 has no callFactory(): OkHttp is supplied as a network fetcher
            // component from the coil-network-okhttp module. Registering the tuned
            // client here keeps the separate dispatcher/connection pool above in play.
            .components { add(OkHttpNetworkFetcher.factory(imageClient)) }
            .diskCache {
                DiskCache.Builder()
                    // DiskCache takes an okio Path in 3.x, not a java.io.File.
                    .directory(context.cacheDir.resolve("ImageCache").toOkioPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
