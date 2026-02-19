package com.podbelly.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesSearchApi {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 30
    ): ItunesSearchResponse
}

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<ItunesPodcast>
)

@JsonClass(generateAdapter = true)
data class ItunesPodcast(
    @Json(name = "trackName") val trackName: String?,
    @Json(name = "artistName") val artistName: String?,
    @Json(name = "feedUrl") val feedUrl: String?,
    @Json(name = "artworkUrl600") val artworkUrl600: String?,
    @Json(name = "collectionName") val collectionName: String?
)
