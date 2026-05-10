package com.podbelly.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return ImageLoader.Builder(context)
            .crossfade(true)
            .callFactory(okHttpClient)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("ImageCache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
