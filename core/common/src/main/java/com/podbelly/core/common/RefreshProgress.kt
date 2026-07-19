package com.podbelly.core.common

/**
 * Progress of an in-flight feed refresh: [completed] of [total] feeds finished
 * (succeeded or failed). Feeds are fetched in parallel (capped at 5 at a time),
 * so [completed] counts completions, not a position in a sequence.
 */
data class RefreshProgress(
    val completed: Int,
    val total: Int,
)
