package com.podbelly.core.network.model

data class RssFeed(
    val title: String,
    val description: String,
    val author: String,
    val artworkUrl: String,
    val link: String,
    val episodes: List<RssEpisode>,
)

data class RssEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val publishedAt: Long,
    val duration: Long,
    val artworkUrl: String?,
    val fileSize: Long,
)
