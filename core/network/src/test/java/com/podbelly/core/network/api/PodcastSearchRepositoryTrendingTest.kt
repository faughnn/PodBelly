package com.podbelly.core.network.api

import com.podbelly.core.network.rss.RssParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastSearchRepositoryTrendingTest {

    private val api = mockk<ItunesSearchApi>()
    private val repository = PodcastSearchRepository(api, OkHttpClient(), RssParser())

    private fun entry(name: String, showId: String) = AppleChartEntry(
        name = name,
        artistName = "Publisher",
        artworkUrl100 = "https://is1.example.com/art/100x100bb.png",
        url = "https://podcasts.apple.com/ie/podcast/some-show/id$showId?i=1000000000001",
    )

    private fun show(id: Long, title: String, feedUrl: String?) = ItunesPodcast(
        collectionId = id,
        trackName = title,
        artistName = "Publisher",
        feedUrl = feedUrl,
        artworkUrl600 = "https://is1.example.com/art/600x600bb.png",
        collectionName = title,
    )

    @Test
    fun `trending episodes resolve their shows' feeds in one lookup, keeping chart order`() = runTest {
        coEvery { api.trendingEpisodes(any()) } returns AppleChartResponse(
            AppleChartFeed(
                listOf(
                    entry("Episode B", "222"),
                    entry("Episode A", "111"),
                    entry("Episode B2", "222"),
                    entry("No Feed", "333"),
                    AppleChartEntry(name = "No Link", artistName = null, artworkUrl100 = null, url = null),
                )
            )
        )
        coEvery { api.lookupPodcasts(any(), any()) } returns ItunesSearchResponse(
            resultCount = 3,
            results = listOf(
                show(111, "Show A", "https://feeds.example.com/a"),
                show(222, "Show B", "https://feeds.example.com/b"),
                show(333, "Show C", null),
            ),
        )

        val results = repository.trendingEpisodes("ie", 25)

        assertEquals(listOf("Episode B", "Episode A", "Episode B2"), results.map { it.title })
        assertEquals("https://feeds.example.com/b", results[0].feedUrl)
        assertEquals("Show B", results[0].podcastTitle)
        assertEquals("https://is1.example.com/art/600x600bb.png", results[0].artworkUrl)
        coVerify(exactly = 1) { api.lookupPodcasts("222,111,333", any()) }
        coVerify {
            api.trendingEpisodes(
                match { it.endsWith("/ie/podcasts/top/25/podcast-episodes.json") && it.startsWith("https://") }
            )
        }
    }

    @Test
    fun `an empty trending chart makes no lookup`() = runTest {
        coEvery { api.trendingEpisodes(any()) } returns AppleChartResponse(AppleChartFeed(null))

        assertTrue(repository.trendingEpisodes("ie").isEmpty())
        coVerify(exactly = 0) { api.lookupPodcasts(any(), any()) }
    }
}
