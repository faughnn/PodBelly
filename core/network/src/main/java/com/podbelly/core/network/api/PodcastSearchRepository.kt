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

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to fetch feed: HTTP ${response.code} for $feedUrl")
        }

        val body = response.body?.string()
            ?: throw IOException("Empty response body for $feedUrl")

        rssParser.parse(feedUrl, body)
    }

    companion object {
        private const val USER_AGENT =
            "Podbelly/1.0 (Android; Podcast App) OkHttp"
    }
}
