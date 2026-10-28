package com.podbelly.core.network.model

data class SearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val artworkUrl: String,
    val description: String = ""
)

/** Which field a podcast search matches the query against. */
enum class SearchField {
    /** Title, author, description and keywords. */
    ANYTHING,
    TITLE,
    /** The podcast's creator / host ("artist" in iTunes terms). */
    AUTHOR,
}

/**
 * An episode found by episode search or the trending-episodes chart. [episodeGuid],
 * [publishedAt] and [durationSeconds] are unknown (null / 0) for trending entries,
 * which only give a title and show.
 */
data class EpisodeSearchResult(
    val title: String,
    val podcastTitle: String,
    val feedUrl: String,
    val artworkUrl: String,
    val episodeGuid: String? = null,
    val audioUrl: String? = null,
    val publishedAt: Long = 0L,
    val durationSeconds: Int = 0,
)
