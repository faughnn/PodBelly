package com.podbelly.core.playback

/**
 * Computes a safe target position for a relative skip (rewind / fast-forward).
 *
 * Shared by [PlaybackService] (notification / lock-screen skip buttons) and
 * [PlaybackController] (in-app skip buttons) so both behave identically.
 *
 * Behaviour:
 * - Never returns a negative position (rewinding past the start parks at 0).
 * - When [durationMs] is known (> 0), the target is capped one second short of the
 *   end. A forward skip that landed *exactly* on the duration drove ExoPlayer to
 *   [androidx.media3.common.Player.STATE_ENDED], which marked the episode as played
 *   (so it reopened looking "completed") and cleared the media item — a subsequent
 *   rapid skip then seeked into an empty timeline and crashed the process. Parking a
 *   second short keeps the player out of STATE_ENDED, so fast-forwarding near the end
 *   can no longer spuriously complete the episode.
 * - When the duration is unknown (streaming / still buffering, [durationMs] <= 0) the
 *   raw target is returned and the player clamps it once the real duration is known.
 *   (Clamping to an unknown duration would force the target back to 0 — see the
 *   `C.TIME_UNSET` note in the seek call sites.)
 */
internal fun computeSkipTarget(currentPositionMs: Long, offsetMs: Long, durationMs: Long): Long {
    val target = currentPositionMs + offsetMs
    return when {
        target <= 0L -> 0L
        durationMs > 0L -> target.coerceAtMost((durationMs - 1_000L).coerceAtLeast(0L))
        else -> target
    }
}

/**
 * Decides whether playback has entered the configured outro-skip window and the
 * episode should be treated as finished (per-podcast "skip ending", the AntennaPod
 * pattern: mark played + advance the queue, exactly like a natural STATE_ENDED).
 *
 * Called from [PlaybackController]'s periodic position loop rather than by seeking
 * to the end: [computeSkipTarget] deliberately parks user seeks one second short of
 * the duration to keep the player out of STATE_ENDED, so ending the episode via a
 * seek would be fragile. Instead the controller ends it explicitly when this
 * returns true.
 *
 * Guards:
 * - [alreadyFired]: fires at most once per episode playback; the caller resets its
 *   flag when a new episode starts.
 * - [skipOutroSeconds] <= 0 means the feature is off for this podcast.
 * - [durationMs] <= 0 means the duration is unknown (still buffering / endless
 *   stream) — never fire, we can't know where the outro starts.
 * - An outro at least as long as the whole episode never fires (misconfiguration
 *   would otherwise finish the episode the instant it starts).
 */
internal fun shouldEndForOutro(
    positionMs: Long,
    durationMs: Long,
    skipOutroSeconds: Int,
    alreadyFired: Boolean,
): Boolean {
    if (alreadyFired) return false
    if (skipOutroSeconds <= 0) return false
    if (durationMs <= 0L) return false
    val skipOutroMs = skipOutroSeconds * 1000L
    if (skipOutroMs >= durationMs) return false
    return positionMs >= durationMs - skipOutroMs
}
