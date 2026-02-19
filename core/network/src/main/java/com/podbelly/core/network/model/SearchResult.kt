package com.podbelly.core.network.model

data class SearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val artworkUrl: String,
    val description: String = ""
)
