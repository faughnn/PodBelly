package com.podbelly.core.network.api

import com.podbelly.core.network.model.RssFeed
import com.podbelly.core.network.model.SearchResult
import com.podbelly.core.network.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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

            val source = response.body?.source()
                ?: throw IOException("Empty response body for $feedUrl")

            // Cap how much of an (untrusted) feed we buffer into memory so a hostile or
            // misconfigured server can't OOM the app. request() tries to buffer one byte
            // past the limit; if it succeeds the feed is over budget.
            source.request(MAX_FEED_BYTES + 1)
            if (source.buffer.size > MAX_FEED_BYTES) {
                throw IOException("Feed exceeds ${MAX_FEED_BYTES / (1024 * 1024)}MB limit: $feedUrl")
            }

            rssParser.parse(feedUrl, source.readUtf8())
        }
    }

    companion object {
        private const val USER_AGENT =
            "Podbelly/1.0 (Android; Podcast App) OkHttp"

        /** Maximum feed size we will buffer into memory (10 MB). */
        private const val MAX_FEED_BYTES = 10L * 1024 * 1024
    }
}
