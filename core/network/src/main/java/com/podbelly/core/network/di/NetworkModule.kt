package com.podbelly.core.network.di

import com.podbelly.core.network.api.ItunesSearchApi
import com.podbelly.core.network.rss.RssParser
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                // Only set a default User-Agent when the caller didn't specify one,
                // so per-request User-Agent values aren't silently overwritten.
                val request = if (original.header("User-Agent") == null) {
                    original.newBuilder()
                        .header("User-Agent", "Podbelly/1.0 (Android; Podcast App)")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            })
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            // Bound total call time so a slow-drip body can't hang a feed fetch forever.
            .callTimeout(90L, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideItunesSearchApi(retrofit: Retrofit): ItunesSearchApi {
        return retrofit.create(ItunesSearchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRssParser(): RssParser {
        return RssParser()
    }
}
