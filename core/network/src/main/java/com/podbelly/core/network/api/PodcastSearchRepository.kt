package com.podbelly.core.network.api

import com.podbelly.core.network.model.EpisodeSearchResult
import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchField
import com.podbelly.core.network.model.SearchResult
import com.podbelly.core.network.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

class PodcastSearchRepository @Inject constructor(
    private val itunesSearchApi: ItunesSearchApi,
    private val okHttpClient: OkHttpClient,
    private val rssParser: RssParser
) {

    /**
     * Searches for podcasts using the iTunes Search API and maps
     * the results to our domain [SearchResult] model.
     *
     * @param field which field to match [query] against.
     * @param country storefront to search ("ie", "gb", …); null uses iTunes' default (US).
     */
    suspend fun search(
        query: String,
        field: SearchField = SearchField.ANYTHING,
        country: String? = null,
    ): List<SearchResult> {
        val attribute = when (field) {
            SearchField.ANYTHING -> null
            SearchField.TITLE -> "titleTerm"
            SearchField.AUTHOR -> "artistTerm"
        }
        val response = itunesSearchApi.searchPodcasts(
            term = query,
            attribute = attribute,
            country = country,
        )
        return response.results
            .filter { !it.feedUrl.isNullOrBlank() }
            .map { it.toSearchResult() }
            .distinctBy { it.feedUrl }
    }

    /**
     * Searches episodes across all podcasts (e.g. a guest's name finds every show
     * they appeared on). Results without a feed URL can't be opened and are dropped.
     */
    suspend fun searchEpisodes(query: String, country: String? = null): List<EpisodeSearchResult> {
        val response = itunesSearchApi.searchEpisodes(term = query, country = country)
        return response.results
            .filter { !it.feedUrl.isNullOrBlank() && !it.trackName.isNullOrBlank() }
            .map { episode ->
                EpisodeSearchResult(
                    title = episode.trackName.orEmpty(),
                    podcastTitle = episode.collectionName.orEmpty(),
                    feedUrl = episode.feedUrl.orEmpty().trim(),
                    artworkUrl = episode.artworkUrl600.orEmpty(),
                    episodeGuid = episode.episodeGuid?.takeIf { it.isNotBlank() },
                    audioUrl = episode.episodeUrl?.takeIf { it.isNotBlank() },
                    publishedAt = episode.releaseDate?.let(::parseInstantMillis) ?: 0L,
                    durationSeconds = ((episode.trackTimeMillis ?: 0L) / 1000).toInt(),
                )
            }
    }

    /**
     * Apple's trending-episodes chart for [country]. Entries only name the episode
     * and link to it on Apple Podcasts, so their shows' feed URLs come from one
     * batched lookup of the show ids in those links; entries whose show can't be
     * resolved are dropped. Chart order is preserved.
     */
    suspend fun trendingEpisodes(country: String, limit: Int = 50): List<EpisodeSearchResult> {
        val chart = itunesSearchApi.trendingEpisodes(
            "$APPLE_CHARTS_BASE_URL/$country/podcasts/top/$limit/podcast-episodes.json"
        )
        val entries = chart.feed.results.orEmpty().mapNotNull { entry ->
            val showId = entry.url?.let { SHOW_ID_IN_URL.find(it)?.groupValues?.get(1) }
            if (showId == null || entry.name.isNullOrBlank()) null else showId to entry
        }
        if (entries.isEmpty()) return emptyList()

        val shows = itunesSearchApi
            .lookupPodcasts(ids = entries.map { it.first }.distinct().joinToString(","))
            .results
            .filter { !it.feedUrl.isNullOrBlank() }
            .associateBy { it.collectionId?.toString() }

        return entries.mapNotNull { (showId, entry) ->
            val show = shows[showId] ?: return@mapNotNull null
            EpisodeSearchResult(
                title = entry.name.orEmpty(),
                podcastTitle = (show.collectionName ?: show.trackName ?: entry.artistName).orEmpty(),
                feedUrl = show.feedUrl.orEmpty().trim(),
                // The chart links 100px artwork; Apple serves other sizes from the same path.
                artworkUrl = entry.artworkUrl100?.replace("100x100", "600x600")
                    ?: show.artworkUrl600.orEmpty(),
            )
        }
    }

    /**
     * Fetches the iTunes top-podcasts chart for [country] and [genreId] (26 = the
     * overall chart) and maps it to [SearchResult]s, preserving chart order.
     *
     * The chart feed doesn't include feed URLs, so entries are enriched with one
     * batched lookup call; entries iTunes can't resolve (or that lack a feed URL)
     * are dropped.
     */
    suspend fun topPodcasts(country: String, genreId: Int, limit: Int = 25): List<SearchResult> {
        val chart = itunesSearchApi.topPodcasts(country = country, genre = genreId, limit = limit)
        val chartIds = chart.feed.entry.orEmpty()
            .mapNotNull { it.id?.attributes?.imId }
            .filter { it.isNotBlank() }
        if (chartIds.isEmpty()) return emptyList()

        // lookup returns results in arbitrary order; restore the chart ranking.
        val rankById: Map<String, Int> = chartIds.withIndex().associate { (i, id) -> id to i }
        val lookup = itunesSearchApi.lookupPodcasts(ids = chartIds.joinToString(","))

        return lookup.results
            .filter { !it.feedUrl.isNullOrBlank() }
            .sortedBy { rankById[it.collectionId?.toString()] ?: Int.MAX_VALUE }
            .map { it.toSearchResult() }
            .distinctBy { it.feedUrl }
    }

    private fun ItunesPodcast.toSearchResult() = SearchResult(
        feedUrl = feedUrl.orEmpty().trim(),
        title = (trackName ?: collectionName).orEmpty(),
        author = artistName.orEmpty(),
        artworkUrl = artworkUrl600.orEmpty()
    )

    private fun parseInstantMillis(iso: String): Long? =
        try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            null
        }

    /**
     * Fetches the RSS feed at the given [feedUrl] and parses it into
     * an [RssFeed] domain model. Follows redirects and sets a proper User-Agent.
     */
    suspend fun fetchFeed(feedUrl: String): RssFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .get()
            .build()

        // Use `response.use { }` so the body/connection is always returned to the
        // pool, including the error and empty-body paths. Without this, every feed
        // that returns an HTTP error leaked a connection on each periodic refresh.
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch feed: HTTP ${response.code} for $feedUrl")
            }

            val body = response.body
                ?: throw IOException("Empty response body for $feedUrl")

            // Stream the body straight into the XML parser instead of buffering the
            // whole document: peak memory per in-flight feed drops from roughly
            // 3-4x the file size (raw bytes + decoded String + parsed text) to just
            // the parsed episode objects, which is what makes a high refresh
            // parallelism safe. Same idea as AntennaPod, which streams feeds
            // through SAX rather than holding them in memory. The byte cap still
            // applies so a hostile or endless feed can't run away.
            //
            // Charset precedence is preserved from the buffered implementation:
            // HTTP Content-Type header first; otherwise the parser sniffs the BOM /
            // <?xml encoding="…"?> prolog, defaulting to UTF-8. Many real feeds are
            // served as ISO-8859-1 / windows-1252, and force-decoding those as
            // UTF-8 turns accents, smart quotes and em dashes into garbage.
            val limited = LimitedInputStream(body.byteStream(), MAX_FEED_BYTES, feedUrl)
            val headerCharset = body.contentType()?.charset()?.name()

            rssParser.parse(feedUrl, limited, headerCharset)
        }
    }

    /**
     * Throws once more than [limit] bytes have been read, bounding how much of an
     * (untrusted) feed we will ever pull off the network.
     */
    private class LimitedInputStream(
        stream: InputStream,
        private val limit: Long,
        private val feedUrl: String,
    ) : FilterInputStream(stream) {
        private var bytesRead = 0L

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) count(1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) count(n.toLong())
            return n
        }

        private fun count(n: Long) {
            bytesRead += n
            if (bytesRead > limit) {
                throw IOException("Feed exceeds ${limit / (1024 * 1024)}MB limit: $feedUrl")
            }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Podbelly/1.0 (Android; Podcast App) OkHttp"

        private const val APPLE_CHARTS_BASE_URL = "https://rss.marketingtools.apple.com/api/v2"

        /** The show id in an Apple Podcasts link: ".../id1692381473?i=…". */
        private val SHOW_ID_IN_URL = Regex("""/id(\d+)""")

        /**
         * Maximum bytes we will read from a single feed (50 MB). Only a guard against
         * runaway or hostile streams, not a sanity check on real feeds: Substack
         * embeds every episode's full show notes, so a long-running show's feed
         * passes 10 MB (Latent Space: ~14 MB for 231 episodes) and keeps growing.
         * AntennaPod has no cap at all. Streaming parsing keeps memory to the
         * parsed episodes, not the document.
         */
        internal const val MAX_FEED_BYTES = 50L * 1024 * 1024
    }
}
