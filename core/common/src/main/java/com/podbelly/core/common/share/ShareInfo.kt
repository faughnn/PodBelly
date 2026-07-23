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
    }
}
