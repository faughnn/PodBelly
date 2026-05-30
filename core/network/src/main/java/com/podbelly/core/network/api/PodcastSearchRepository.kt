package com.podbelly.core.network.api

import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchResult
import com.podbelly.core.network.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.charset.Charset
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
            val source = body.source()

            // Cap how much of an (untrusted) feed we buffer into memory so a hostile or
            // misconfigured server can't OOM the app. request() tries to buffer one byte
            // past the limit; if it succeeds the feed is over budget.
            source.request(MAX_FEED_BYTES + 1)
            if (source.buffer.size > MAX_FEED_BYTES) {
                throw IOException("Feed exceeds ${MAX_FEED_BYTES / (1024 * 1024)}MB limit: $feedUrl")
            }

            // Decode using the feed's actual charset rather than assuming UTF-8. Many
            // real feeds are served as ISO-8859-1 / windows-1252; force-decoding those as
            // UTF-8 turns accents, smart quotes and em dashes into garbage. Prefer the
            // HTTP Content-Type charset, then the <?xml encoding="…"?> declaration,
            // defaulting to UTF-8 only when neither is present.
            val bytes = source.readByteArray()
            val charset = body.contentType()?.charset()
                ?: detectXmlDeclCharset(bytes)
                ?: Charsets.UTF_8

            rssParser.parse(feedUrl, String(bytes, charset))
        }
    }

    /**
     * Sniffs the charset declared in an XML prolog (`<?xml … encoding="…"?>`). The
     * declaration itself is always ASCII-compatible, so a short prefix can be read as
     * ISO-8859-1 to recover the encoding attribute. Returns null if absent/unknown.
     */
    private fun detectXmlDeclCharset(bytes: ByteArray): Charset? {
        val prefix = String(bytes, 0, minOf(bytes.size, 256), Charsets.ISO_8859_1)
        if (!prefix.startsWith("<?xml")) return null
        val declEnd = prefix.indexOf("?>")
        if (declEnd < 0) return null
        val match = ENCODING_REGEX.find(prefix.substring(0, declEnd)) ?: return null
        return try {
            Charset.forName(match.groupValues[1])
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val USER_AGENT =
            "Podbelly/1.0 (Android; Podcast App) OkHttp"

        private val ENCODING_REGEX = Regex("""encoding\s*=\s*["']([^"']+)["']""")

        /** Maximum feed size we will buffer into memory (10 MB). */
        private const val MAX_FEED_BYTES = 10L * 1024 * 1024
    }
}
