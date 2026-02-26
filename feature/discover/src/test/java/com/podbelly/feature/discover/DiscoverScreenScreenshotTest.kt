package com.podbelly.feature.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class DiscoverScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun emptyState() {
        paparazzi.snapshot {
            MaterialTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        SearchSection(
                            query = "",
                            onQueryChange = {},
                            onSearch = {},
                        )

                        RssUrlSection(
                            feedUrl = "",
                            onFeedUrlChange = {},
                            onSubscribe = {},
                            isSubscribing = false,
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        DiscoverEmptyState(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Test
    fun searchResultsWithMixedSubscriptionState() {
        val results = listOf(
            DiscoverPodcastItem(
                title = "The Joe Rogan Experience",
                author = "Joe Rogan",
                artworkUrl = "",
                feedUrl = "https://example.com/jre/feed.xml",
                isSubscribed = true,
            ),
            DiscoverPodcastItem(
                title = "Serial",
                author = "Serial Productions",
                artworkUrl = "",
                feedUrl = "https://example.com/serial/feed.xml",
                isSubscribed = false,
            ),
            DiscoverPodcastItem(
                title = "Radiolab",
                author = "WNYC Studios",
                artworkUrl = "",
                feedUrl = "https://example.com/radiolab/feed.xml",
                isSubscribed = false,
            ),
        )

        paparazzi.snapshot {
            MaterialTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        SearchSection(
                            query = "podcast",
                            onQueryChange = {},
                            onSearch = {},
                        )

                        RssUrlSection(
                            feedUrl = "",
                            onFeedUrlChange = {},
                            onSubscribe = {},
                            isSubscribing = false,
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SearchResultsList(
                            results = results,
                            isSubscribing = false,
                            onSubscribe = {},
                            onPodcastClick = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun rssUrlInputWithSubscribing() {
        paparazzi.snapshot {
            MaterialTheme {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SearchSection(
                        query = "",
                        onQueryChange = {},
                        onSearch = {},
                    )

                    RssUrlSection(
                        feedUrl = "https://feeds.simplecast.com/dHoohVNH",
                        onFeedUrlChange = {},
                        onSubscribe = {},
                        isSubscribing = true,
                    )
                }
            }
        }
    }
}
