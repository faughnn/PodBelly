package com.podbelly.core.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.podbelly.core.database.entity.EpisodeEntity
import com.podbelly.core.database.entity.PodcastEntity

/**
 * Pure mapping layer between Podbelly's database entities and the Media3 browse tree
 * served to Android Auto (and any other MediaBrowser client) by [PlaybackService].
 *
 * Structure (media3 session demo's MediaItemTree pattern, content modelled on
 * Pocket Casts' AutoConverter):
 *
 * ```
 * root
 * ├── queue        (only when the queue feature is enabled)
 * │   └── queued episodes that are downloaded          [playable]
 * └── podcasts
 *     └── podcast_{id}  (subscribed shows with >= 1 downloaded episode)
 *         └── downloaded episodes, newest first        [playable]
 * ```
 *
 * Download-first: only downloaded episodes are ever listed as playable, matching the
 * app's core design — Android Auto must not stream episodes the user hasn't downloaded.
 *
 * Media ID conventions:
 * - Browse items use stable prefixed ids: `podcast_{id}` / `episode_{id}`.
 * - *Playable* (resolved) items use the plain numeric episode id, which is the
 *   convention [PlaybackController.play] has always used, so the controller's
 *   state sync keeps working regardless of who started playback.
 *
 * Everything in here is a pure function of its inputs so it can be unit tested
 * without a service or player.
 */
@OptIn(UnstableApi::class) // MediaMetadata.Builder.setDurationMs
object BrowseTree {

    const val ROOT_ID = "root"
    const val QUEUE_ID = "queue"
    const val PODCASTS_ID = "podcasts"
    const val PODCAST_PREFIX = "podcast_"
    const val EPISODE_PREFIX = "episode_"

    fun podcastMediaId(podcastId: Long): String = PODCAST_PREFIX + podcastId

    fun episodeMediaId(episodeId: Long): String = EPISODE_PREFIX + episodeId

    /** Parses a `podcast_{id}` media id; null for anything else. */
    fun parsePodcastId(mediaId: String): Long? =
        if (mediaId.startsWith(PODCAST_PREFIX)) {
            mediaId.removePrefix(PODCAST_PREFIX).toLongOrNull()
        } else {
            null
        }

    /**
     * Parses an episode id from a media id. Accepts both the browse-tree form
     * ("episode_42") and the app's historical plain numeric form ("42") so that
     * every layer (controller sync, item resolution) understands items from
     * either origin.
     */
    fun parseEpisodeId(mediaId: String): Long? = when {
        mediaId.startsWith(EPISODE_PREFIX) -> mediaId.removePrefix(EPISODE_PREFIX).toLongOrNull()
        else -> mediaId.toLongOrNull()
    }

    /** True when [mediaId] is a browse-tree episode id (i.e. picked from Android Auto). */
    fun isBrowseEpisodeId(mediaId: String): Boolean = mediaId.startsWith(EPISODE_PREFIX)

    fun rootItem(): MediaItem =
        folderItem(ROOT_ID, "Podbelly", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    fun queueFolderItem(): MediaItem =
        folderItem(QUEUE_ID, "Queue", MediaMetadata.MEDIA_TYPE_PLAYLIST)

    fun podcastsFolderItem(): MediaItem =
        folderItem(PODCASTS_ID, "Podcasts", MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)

    /** Top-level folders. The Queue folder only appears when the queue feature is on. */
    fun rootChildren(queueEnabled: Boolean): List<MediaItem> = buildList {
        if (queueEnabled) add(queueFolderItem())
        add(podcastsFolderItem())
    }

    /** A browsable (not playable) folder for one subscribed podcast. */
    fun podcastItem(podcast: PodcastEntity): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(podcast.title)
            .setArtist(podcast.author)
            .setArtworkUri(uriOrNull(podcast.artworkUrl))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
            .build()
        return MediaItem.Builder()
            .setMediaId(podcastMediaId(podcast.id))
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * A playable episode entry as shown in browse lists ("episode_{id}" media id,
     * no URI — the URI is attached during resolution in onAddMediaItems/onSetMediaItems,
     * which is also where download-first is enforced).
     */
    fun episodeBrowseItem(
        episode: EpisodeEntity,
        podcastTitle: String,
        fallbackArtworkUrl: String = "",
    ): MediaItem = MediaItem.Builder()
        .setMediaId(episodeMediaId(episode.id))
        .setMediaMetadata(episodeMetadata(episode, podcastTitle, fallbackArtworkUrl))
        .build()

    /**
     * A fully populated, immediately playable MediaItem for [episode].
     *
     * Download-first: prefers the downloaded file and falls back to the remote
     * stream URL only when there is no download (mirrors every play() call site).
     * Returns null when there is nothing playable at all.
     *
     * The media id is the plain numeric episode id — the app-wide convention — so
     * [PlaybackController] recognises the item no matter who initiated playback.
     */
    fun playableEpisodeItem(
        episode: EpisodeEntity,
        podcastTitle: String,
        fallbackArtworkUrl: String = "",
    ): MediaItem? {
        val url = episode.downloadPath.ifBlank { episode.audioUrl }
        if (url.isBlank()) return null
        return MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(url)
            // The mime type is inferred from the *remote* URL: downloaded files keep
            // the feed's extension-less or hashed names, while enclosure URLs almost
            // always carry a real audio extension.
            .setMimeType(audioMimeTypeFor(episode.audioUrl.ifBlank { url }))
            .setMediaMetadata(episodeMetadata(episode, podcastTitle, fallbackArtworkUrl))
            .build()
    }

    /**
     * A playable MediaItem for a Chromecast receiver.
     *
     * Always uses the REMOTE stream URL — the receiver cannot read files on the
     * phone, so the download-first rule necessarily bends while casting — and
     * carries an explicit MIME type because CastPlayer's DefaultMediaItemConverter
     * requires one. Returns null when the feed provides no remote URL (the caller
     * must surface that instead of silently failing on the receiver).
     */
    fun castEpisodeItem(
        episode: EpisodeEntity,
        podcastTitle: String,
        fallbackArtworkUrl: String = "",
    ): MediaItem? {
        if (episode.audioUrl.isBlank()) return null
        return MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(episode.audioUrl)
            .setMimeType(audioMimeTypeFor(episode.audioUrl))
            .setMediaMetadata(episodeMetadata(episode, podcastTitle, fallbackArtworkUrl))
            .build()
    }

    /**
     * Best-effort audio MIME type for a podcast enclosure URL. Podcast enclosures are
     * overwhelmingly MP3, so that is the default when the extension is unrecognised.
     * Needed because CastPlayer's DefaultMediaItemConverter requires a non-null MIME
     * type, and it also helps ExoPlayer skip container sniffing.
     */
    fun audioMimeTypeFor(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".m4a") || path.endsWith(".mp4") -> "audio/mp4"
            path.endsWith(".aac") -> "audio/aac"
            path.endsWith(".ogg") || path.endsWith(".oga") || path.endsWith(".opus") -> "audio/ogg"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".flac") -> "audio/flac"
            else -> "audio/mpeg"
        }
    }

    private fun episodeMetadata(
        episode: EpisodeEntity,
        podcastTitle: String,
        fallbackArtworkUrl: String,
    ): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(podcastTitle)
            .setArtworkUri(uriOrNull(episode.artworkUrl.ifBlank { fallbackArtworkUrl }))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
        if (episode.durationSeconds > 0) {
            builder.setDurationMs(episode.durationSeconds * 1000L)
        }
        return builder.build()
    }

    private fun folderItem(mediaId: String, title: String, mediaType: Int): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(mediaType)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun uriOrNull(url: String): Uri? = if (url.isNotBlank()) Uri.parse(url) else null
}
