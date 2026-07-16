package com.podbelly.core.network.model

/**
 * One line of an episode transcript: the text spoken from [startMs] (relative to
 * the start of the episode audio) until the next cue.
 */
data class TranscriptCue(
    val startMs: Long,
    val text: String,
)
