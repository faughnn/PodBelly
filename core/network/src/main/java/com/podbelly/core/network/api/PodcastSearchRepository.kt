package com.podbelly.core.network.api

import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchResult
import com.podbelly.core.network.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class PodcastSearchRepository @Inject constructor(
    private val itunesSearchApi: ItunesSearchApi,
    private val okHttpClient: OkHttpClient,
    private val rssParser: RssParser
) {

    /**
     * Searches for podcasts using the iTunes Search API and maps
     * the results to our domain [SearchResult] model.
     */
    suspend fun search(query: String): List<SearchResult> {
        val response = itunesSearchApi.searchPodcasts(term = query)
        return response.results
            .filter { !it.feedUrl.isNullOrBlank() }
            .map { podcast ->
                SearchResult(
                    feedUrl = podcast.feedUrl.orEmpty().trim(),
                    title = (podcast.trackName ?: podcast.collectionName).orEmpty(),
                    author = podcast.artistName.orEmpty(),
                    artworkUrl = podcast.artworkUrl600.orEmpty()
                )
            }
            .distinctBy { it.feedUrl }
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
            .map { podcast ->
                SearchResult(
                    feedUrl = podcast.feedUrl.orEmpty().trim(),
                    title = (podcast.trackName ?: podcast.collectionName).orEmpty(),
                    author = podcast.artistName.orEmpty(),
                    artworkUrl = podcast.artworkUrl600.orEmpty()
                )
            }
            .distinctBy { it.feedUrl }
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

        /** Maximum bytes we will read from a single feed (10 MB). */
        private const val MAX_FEED_BYTES = 10L * 1024 * 1024
    }
}
