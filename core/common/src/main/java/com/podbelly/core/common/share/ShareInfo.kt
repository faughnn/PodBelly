package com.podbelly.core.common.share

/**
 * Everything needed to share an episode: what the card shows plus the links
 * that ride along as the message text.
 *
 * [episodeUrl] is the best available link to the episode itself (its audio
 * enclosure today). [showWebsite] is the series' web page (may be blank).
 * [feedUrl] is the RSS feed — the universal "subscribe to this show" link that
 * any podcast app (PodBelly included) can add.
 */
data class ShareInfo(
    val episodeTitle: String,
    val podcastTitle: String,
    val artworkUrl: String,
    val episodeUrl: String,
    val showWebsite: String,
    val feedUrl: String,
)

/** The message text sent alongside the share-card image. */
fun ShareInfo.toShareText(): String = buildString {
    append(episodeTitle)
    if (podcastTitle.isNotBlank()) append(" · ").append(podcastTitle)
    append("\n")
    if (episodeUrl.isNotBlank()) {
        append("\nListen: ").append(episodeUrl)
    }
    if (showWebsite.isNotBlank()) {
        append("\nShow: ").append(showWebsite)
    }
    if (feedUrl.isNotBlank()) {
        append("\nSubscribe (RSS): ").append(feedUrl)
        // A podcast:// version of the feed: tapping it opens a native podcast app
        // (Apple Podcasts on iOS, the user's default app on Android, or PodBelly
        // itself) straight into subscribing to the show.
        append("\nSubscribe (open app): ").append(feedUrl.toPodcastScheme())
    }
}

/**
 * Turn an http(s) feed URL into a `podcast://` subscribe link. This is the
 * de-facto scheme podcast apps register for; PodBelly registers for it too, so
 * the link round-trips back into this app's add-by-RSS flow.
 */
fun String.toPodcastScheme(): String = when {
    startsWith("https://") -> "podcast://" + substring("https://".length)
    startsWith("http://") -> "podcast://" + substring("http://".length)
    else -> "podcast://" + trimStart('/')
}
