package com.podbelly.core.common.share

import java.net.URLEncoder

/**
 * Hosted redirect that turns a tappable https link into a `podcast://` open —
 * a static page in this repo, served by GitHub Pages. Chat apps only linkify
 * well-known schemes (http/https), so a raw `podcast://` link isn't tappable in
 * most of them; this https wrapper is, and the page bounces the phone into the
 * podcast app (PodBelly included).
 */
private const val SUBSCRIBE_REDIRECT_BASE = "https://faughnn.github.io/PodBelly/subscribe/"

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

    // Each link on its own line with a blank line between, so the block stays
    // readable even when a URL wraps across several lines in the chat.
    val links = buildList {
        if (episodeUrl.isNotBlank()) add("Listen: $episodeUrl")
        if (showWebsite.isNotBlank()) add("Show: $showWebsite")
        if (feedUrl.isNotBlank()) {
            // An https link (tappable everywhere) that redirects into the
            // podcast:// scheme, so friends can subscribe with one tap.
            add("Subscribe: ${feedUrl.toSubscribeLink()}")
        }
    }
    if (links.isNotEmpty()) {
        append("\n\n")
        append(links.joinToString("\n\n"))
    }
}

/**
 * Wrap an http(s) feed URL in the hosted subscribe-redirect link. The feed is
 * carried as a query param the page reads and turns into a `podcast://` open.
 */
fun String.toSubscribeLink(): String =
    SUBSCRIBE_REDIRECT_BASE + "?feed=" + URLEncoder.encode(this, "UTF-8")
