package com.podbelly.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ItunesSearchApi {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 30
    ): ItunesSearchResponse

    /**
     * Looks up podcasts by their iTunes collection ids ([ids] is comma-separated).
     * The chart feed below doesn't include feed URLs, so chart entries are enriched
     * through this endpoint; results come back in arbitrary order.
     */
    @GET("lookup")
    suspend fun lookupPodcasts(
        @Query("id") ids: String,
        @Query("entity") entity: String = "podcast"
    ): ItunesSearchResponse

    /**
     * iTunes top-podcasts chart for a country ("us", "ie", …) and genre. Genre 26
     * is the root "Podcasts" genre, i.e. the overall chart.
     */
    @GET("{country}/rss/toppodcasts/limit={limit}/genre={genre}/json")
    suspend fun topPodcasts(
        @Path("country") country: String,
        @Path("genre") genre: Int,
        @Path("limit") limit: Int = 25
    ): ItunesTopPodcastsResponse
}

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<ItunesPodcast>
)

@JsonClass(generateAdapter = true)
data class ItunesPodcast(
    @Json(name = "collectionId") val collectionId: Long?,
    @Json(name = "trackName") val trackName: String?,
    @Json(name = "artistName") val artistName: String?,
    @Json(name = "feedUrl") val feedUrl: String?,
    @Json(name = "artworkUrl600") val artworkUrl600: String?,
    @Json(name = "collectionName") val collectionName: String?
)

// The chart feed's JSON only carries display metadata and the collection id; feed
// URLs come from a follow-up lookup call. Note: iTunes serializes a single-entry
// feed as an object rather than an array, which would fail to decode here — with
// chart limits of 25 that never happens in practice.
@JsonClass(generateAdapter = true)
data class ItunesTopPodcastsResponse(
    @Json(name = "feed") val feed: ItunesTopFeed
)

@JsonClass(generateAdapter = true)
data class ItunesTopFeed(
    @Json(name = "entry") val entry: List<ItunesTopEntry>?
)

@JsonClass(generateAdapter = true)
data class ItunesTopEntry(
    @Json(name = "id") val id: ItunesTopEntryId?
)

@JsonClass(generateAdapter = true)
data class ItunesTopEntryId(
    @Json(name = "attributes") val attributes: ItunesTopEntryIdAttributes?
)

@JsonClass(generateAdapter = true)
data class ItunesTopEntryIdAttributes(
    @Json(name = "im:id") val imId: String?
)
