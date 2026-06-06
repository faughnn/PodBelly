package com.podbelly.core.database.entity

/**
 * Merge freshly-parsed channel metadata from a feed into an existing podcast row.
 *
 * Mirrors AntennaPod's `Feed.updateFromOther`: on every refresh we take each feed
 * value **only when the feed actually provides one** (non-blank), otherwise we keep
 * what we already have. This lets publisher corrections — a new cover image, a
 * retitled show, an updated description — propagate after the initial subscribe,
 * without a blank/missing field ever wiping good local data.
 *
 * Deliberately untouched here: identity and user-owned state (`feedUrl`,
 * `subscribed`, `subscribedAt`, `playbackSpeed`, notification prefs) and the
 * refresh bookkeeping (`lastRefreshedAt`, `episodeCount`) — callers set those
 * themselves, since the right value is caller-specific.
 */
fun PodcastEntity.withRefreshedMetadata(
    title: String,
    author: String,
    description: String,
    artworkUrl: String,
    link: String,
): PodcastEntity = copy(
    title = title.ifBlank { this.title },
    author = author.ifBlank { this.author },
    description = description.ifBlank { this.description },
    artworkUrl = artworkUrl.ifBlank { this.artworkUrl },
    link = link.ifBlank { this.link },
)
