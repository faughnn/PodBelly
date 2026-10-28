package com.podbelly.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ItunesSearchApi {

    /**
     * [attribute] narrows which field [term] is matched against ("titleTerm",
     * "artistTerm"); null searches everything. [country] picks the storefront (shows
     * only listed in some countries are missing from the default US one). Null query
     * parameters are left out of the request. 200 is iTunes' documented maximum, but
     * it returns at most 100 podcasts in practice.
     */
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 100,
        @Query("attribute") attribute: String? = null,
        @Query("country") country: String? = null,
    ): ItunesSearchResponse

    /** Episode search: every result carries its show's feed URL and the episode GUID. */
    @GET("search")
    suspend fun searchEpisodes(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcastEpisode",
        @Query("limit") limit: Int = 50,
        @Query("country") country: String? = null,
    ): ItunesEpisodeSearchResponse

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

    /**
     * Apple's trending-episodes chart for a country, from the Marketing Tools feed
     * service (a different host, hence the full [url]). Entries carry no feed URL
     * or GUID, only the episode's Apple Podcasts link, which contains its show id.
     */
    @GET
    suspend fun trendingEpisodes(@Url url: String): AppleChartResponse
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

@JsonClass(generateAdapter = true)
data class ItunesEpisodeSearchResponse(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<ItunesEpisode>
)

@JsonClass(generateAdapter = true)
data class ItunesEpisode(
    @Json(name = "trackName") val trackName: String?,
    @Json(name = "collectionName") val collectionName: String?,
    @Json(name = "feedUrl") val feedUrl: String?,
    @Json(name = "episodeGuid") val episodeGuid: String?,
    /** ISO-8601, e.g. "2026-10-11T10:00:00Z". */
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long?,
    @Json(name = "artworkUrl600") val artworkUrl600: String?,
    @Json(name = "episodeUrl") val episodeUrl: String?,
)

@JsonClass(generateAdapter = true)
data class AppleChartResponse(
    @Json(name = "feed") val feed: AppleChartFeed
)

@JsonClass(generateAdapter = true)
data class AppleChartFeed(
    @Json(name = "results") val results: List<AppleChartEntry>?
)

@JsonClass(generateAdapter = true)
data class AppleChartEntry(
    @Json(name = "name") val name: String?,
    @Json(name = "artistName") val artistName: String?,
    @Json(name = "artworkUrl100") val artworkUrl100: String?,
    /** Apple Podcasts link, e.g. ".../podcast/some-title/id1692381473?i=1000798385938". */
    @Json(name = "url") val url: String?,
)
