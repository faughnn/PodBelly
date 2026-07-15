package com.podbelly.core.playback

import androidx.media3.common.MediaMetadata
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [BrowseTree], the pure entity-to-MediaItem mapping behind the
 * Android Auto browse tree, and [resolveExternalStartPosition], the intro-skip /
 * resume rule for playback started outside the app UI.
 *
 * Robolectric is required because MediaItem/MediaMetadata use android.net.Uri.
 */
@RunWith(RobolectricTestRunner::class)
class BrowseTreeTest {

    private fun podcast(
        id: Long = 7L,
        title: String = "Test Show",
        author: String = "Test Author",
        artworkUrl: String = "https://example.com/show.jpg",
    ) = PodcastEntity(
        id = id,
        feedUrl = "https://example.com/feed.xml",
        title = title,
        author = author,
        description = "",
        artworkUrl = artworkUrl,
        link = "",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
    )

    private fun episode(
        id: Long = 42L,
        podcastId: Long = 7L,
        title: String = "Episode 42",
        audioUrl: String = "https://example.com/ep42.mp3",
        downloadPath: String = "/data/files/ep42.mp3",
        durationSeconds: Int = 1800,
        artworkUrl: String = "https://example.com/ep42.jpg",
        playbackPosition: Long = 0L,
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = "guid-$id",
        title = title,
        description = "",
        audioUrl = audioUrl,
        publicationDate = 0L,
        durationSeconds = durationSeconds,
        artworkUrl = artworkUrl,
        playbackPosition = playbackPosition,
        downloadPath = downloadPath,
    )

    // -------------------------------------------------------------------------
    // Media id round-trips
    // -------------------------------------------------------------------------

    @Test
    fun `podcast media id round-trips`() {
        assertEquals("podcast_7", BrowseTree.podcastMediaId(7L))
        assertEquals(7L, BrowseTree.parsePodcastId("podcast_7"))
    }

    @Test
    fun `episode media id round-trips`() {
        assertEquals("episode_42", BrowseTree.episodeMediaId(42L))
        assertEquals(42L, BrowseTree.parseEpisodeId("episode_42"))
    }

    @Test
    fun `parseEpisodeId accepts the app's plain numeric ids`() {
        assertEquals(42L, BrowseTree.parseEpisodeId("42"))
    }

    @Test
    fun `parseEpisodeId rejects garbage`() {
        assertNull(BrowseTree.parseEpisodeId("episode_abc"))
        assertNull(BrowseTree.parseEpisodeId("podcasts"))
        assertNull(BrowseTree.parseEpisodeId("root"))
    }

    @Test
    fun `parsePodcastId rejects non-podcast ids`() {
        assertNull(BrowseTree.parsePodcastId("episode_42"))
        assertNull(BrowseTree.parsePodcastId("42"))
        assertNull(BrowseTree.parsePodcastId("podcast_x"))
    }

    @Test
    fun `isBrowseEpisodeId distinguishes browse picks from app ids`() {
        assertTrue(BrowseTree.isBrowseEpisodeId("episode_42"))
        assertFalse(BrowseTree.isBrowseEpisodeId("42"))
    }

    // -------------------------------------------------------------------------
    // Root folders
    // -------------------------------------------------------------------------

    @Test
    fun `root item is browsable and not playable`() {
        val root = BrowseTree.rootItem()
        assertEquals(BrowseTree.ROOT_ID, root.mediaId)
        assertEquals(true, root.mediaMetadata.isBrowsable)
        assertEquals(false, root.mediaMetadata.isPlayable)
    }

    @Test
    fun `root children include queue when the queue feature is enabled`() {
        val children = BrowseTree.rootChildren(queueEnabled = true)
        assertEquals(listOf(BrowseTree.QUEUE_ID, BrowseTree.PODCASTS_ID), children.map { it.mediaId })
    }

    @Test
    fun `root children omit queue when the queue feature is disabled`() {
        val children = BrowseTree.rootChildren(queueEnabled = false)
        assertEquals(listOf(BrowseTree.PODCASTS_ID), children.map { it.mediaId })
    }

    // -------------------------------------------------------------------------
    // Podcast folders
    // -------------------------------------------------------------------------

    @Test
    fun `podcast item is a browsable folder with title, author and artwork`() {
        val item = BrowseTree.podcastItem(podcast())
        assertEquals("podcast_7", item.mediaId)
        assertEquals(true, item.mediaMetadata.isBrowsable)
        assertEquals(false, item.mediaMetadata.isPlayable)
        assertEquals("Test Show", item.mediaMetadata.title.toString())
        assertEquals("Test Author", item.mediaMetadata.artist.toString())
        assertEquals("https://example.com/show.jpg", item.mediaMetadata.artworkUri.toString())
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST, item.mediaMetadata.mediaType)
    }

    // -------------------------------------------------------------------------
    // Episode browse items
    // -------------------------------------------------------------------------

    @Test
    fun `episode browse item is playable with duration and artwork`() {
        val item = BrowseTree.episodeBrowseItem(episode(), "Test Show")
        assertEquals("episode_42", item.mediaId)
        assertEquals(false, item.mediaMetadata.isBrowsable)
        assertEquals(true, item.mediaMetadata.isPlayable)
        assertEquals("Episode 42", item.mediaMetadata.title.toString())
        assertEquals("Test Show", item.mediaMetadata.artist.toString())
        assertEquals(1_800_000L, item.mediaMetadata.durationMs)
        assertEquals("https://example.com/ep42.jpg", item.mediaMetadata.artworkUri.toString())
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE, item.mediaMetadata.mediaType)
    }

    @Test
    fun `episode browse item falls back to podcast artwork`() {
        val item = BrowseTree.episodeBrowseItem(
            episode(artworkUrl = ""),
            "Test Show",
            fallbackArtworkUrl = "https://example.com/show.jpg",
        )
        assertEquals("https://example.com/show.jpg", item.mediaMetadata.artworkUri.toString())
    }

    @Test
    fun `episode browse item omits unknown duration`() {
        val item = BrowseTree.episodeBrowseItem(episode(durationSeconds = 0), "Test Show")
        assertNull(item.mediaMetadata.durationMs)
    }

    // -------------------------------------------------------------------------
    // Playable item resolution (download-first)
    // -------------------------------------------------------------------------

    @Test
    fun `playable item prefers the downloaded file`() {
        val item = BrowseTree.playableEpisodeItem(episode(), "Test Show")!!
        assertEquals("/data/files/ep42.mp3", item.localConfiguration?.uri.toString())
    }

    @Test
    fun `playable item falls back to the remote url when not downloaded`() {
        val item = BrowseTree.playableEpisodeItem(episode(downloadPath = ""), "Test Show")!!
        assertEquals("https://example.com/ep42.mp3", item.localConfiguration?.uri.toString())
    }

    @Test
    fun `playable item is null when there is nothing to play`() {
        assertNull(BrowseTree.playableEpisodeItem(episode(audioUrl = "", downloadPath = ""), "Test Show"))
    }

    @Test
    fun `playable item uses the app's plain numeric media id convention`() {
        val item = BrowseTree.playableEpisodeItem(episode(), "Test Show")!!
        assertEquals("42", item.mediaId)
    }

    @Test
    fun `playable item carries a mime type for the player`() {
        val item = BrowseTree.playableEpisodeItem(episode(), "Test Show")!!
        assertEquals("audio/mpeg", item.localConfiguration?.mimeType)
    }

    // -------------------------------------------------------------------------
    // Mime type inference
    // -------------------------------------------------------------------------

    @Test
    fun `mime type inference covers common podcast formats`() {
        assertEquals("audio/mpeg", BrowseTree.audioMimeTypeFor("https://x.com/e.mp3"))
        assertEquals("audio/mp4", BrowseTree.audioMimeTypeFor("https://x.com/e.m4a"))
        assertEquals("audio/mp4", BrowseTree.audioMimeTypeFor("https://x.com/e.mp4"))
        assertEquals("audio/aac", BrowseTree.audioMimeTypeFor("https://x.com/e.aac"))
        assertEquals("audio/ogg", BrowseTree.audioMimeTypeFor("https://x.com/e.opus"))
        assertEquals("audio/ogg", BrowseTree.audioMimeTypeFor("https://x.com/e.ogg"))
        assertEquals("audio/wav", BrowseTree.audioMimeTypeFor("https://x.com/e.wav"))
        assertEquals("audio/flac", BrowseTree.audioMimeTypeFor("https://x.com/e.flac"))
    }

    @Test
    fun `mime type inference ignores query strings and defaults to mp3`() {
        assertEquals("audio/mpeg", BrowseTree.audioMimeTypeFor("https://x.com/e.MP3?token=abc#t=10"))
        assertEquals("audio/mpeg", BrowseTree.audioMimeTypeFor("https://x.com/episode"))
    }

    // -------------------------------------------------------------------------
    // External (Android Auto) start position: resume + intro skip
    // -------------------------------------------------------------------------

    @Test
    fun `external start resumes from the saved position by default`() {
        assertEquals(90_000L, resolveExternalStartPosition(90_000L, 600_000L, 0))
    }

    @Test
    fun `external start applies the intro skip on a fresh episode`() {
        assertEquals(30_000L, resolveExternalStartPosition(0L, 600_000L, 30))
    }

    @Test
    fun `external start never moves a position already past the intro`() {
        assertEquals(45_000L, resolveExternalStartPosition(45_000L, 600_000L, 30))
    }

    @Test
    fun `external start ignores an intro covering the whole episode`() {
        assertEquals(0L, resolveExternalStartPosition(0L, 20_000L, 30))
    }

    @Test
    fun `external start allows the intro skip when the duration is unknown`() {
        assertEquals(30_000L, resolveExternalStartPosition(0L, 0L, 30))
    }
}
