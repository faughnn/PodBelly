package com.podbelly.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastFeedMergeTest {

    private fun podcast() = PodcastEntity(
        id = 1,
        feedUrl = "https://example.com/feed.xml",
        title = "Old Title",
        author = "Old Author",
        description = "Old description",
        artworkUrl = "",
        link = "https://old.link",
        language = "en",
        lastBuildDate = 0L,
        subscribed = true,
        subscribedAt = 123L,
        playbackSpeed = 1.5f,
    )

    @Test
    fun `present feed values overwrite existing metadata`() {
        val merged = podcast().withRefreshedMetadata(
            title = "New Title",
            author = "New Author",
            description = "New description",
            artworkUrl = "https://cdn/cover.png",
            link = "https://new.link",
        )

        assertEquals("New Title", merged.title)
        assertEquals("New Author", merged.author)
        assertEquals("New description", merged.description)
        assertEquals("https://cdn/cover.png", merged.artworkUrl)
        assertEquals("https://new.link", merged.link)
    }

    @Test
    fun `blank feed values keep existing metadata`() {
        val original = podcast().copy(artworkUrl = "https://cdn/existing.png")
        val merged = original.withRefreshedMetadata(
            title = "",
            author = "",
            description = "",
            artworkUrl = "",
            link = "",
        )

        assertEquals("Old Title", merged.title)
        assertEquals("Old Author", merged.author)
        assertEquals("Old description", merged.description)
        assertEquals("https://cdn/existing.png", merged.artworkUrl)
        assertEquals("https://old.link", merged.link)
    }

    @Test
    fun `the empty-artwork-then-populated case (the cover-art bug) heals`() {
        // Subscribed before the feed had cover art (artworkUrl = ""); a later refresh
        // supplies it. The merge must adopt it rather than stay blank.
        val merged = podcast().withRefreshedMetadata(
            title = "Old Title",
            author = "Old Author",
            description = "Old description",
            artworkUrl = "https://cdn/new-cover.png",
            link = "https://old.link",
        )

        assertEquals("https://cdn/new-cover.png", merged.artworkUrl)
    }

    @Test
    fun `user-owned and identity fields are never touched`() {
        val merged = podcast().withRefreshedMetadata(
            title = "New Title",
            author = "New Author",
            description = "New description",
            artworkUrl = "https://cdn/cover.png",
            link = "https://new.link",
        )

        assertEquals("https://example.com/feed.xml", merged.feedUrl)
        assertEquals(true, merged.subscribed)
        assertEquals(123L, merged.subscribedAt)
        assertEquals(1.5f, merged.playbackSpeed)
    }
}
