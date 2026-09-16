package com.podbelly.core.playback.visualizer

/**
 * Delay line between analysis and display.
 *
 * Decoded PCM reaches the audio sink well before it is heard: ExoPlayer keeps a
 * quarter to three-quarters of a second queued in the `AudioTrack`, and Bluetooth
 * output adds more on top. Frames are therefore analysed early, tagged with the
 * presentation time of the audio they describe, and parked here until the sink
 * reports that position as actually playing.
 *
 * Not thread-safe; everything runs on the player's playback thread.
 */
internal class AudibleFrameQueue(private val capacity: Int = DEFAULT_CAPACITY) {

    private class Entry(val timeUs: Long, val frame: VisualizerFrame)

    private val entries = ArrayDeque<Entry>()

    val size: Int get() = entries.size

    /** Queues [frame], describing audio that ends at [timeUs]. Oldest entries go first if full. */
    fun add(timeUs: Long, frame: VisualizerFrame) {
        if (entries.size >= capacity) entries.removeFirst()
        entries.addLast(Entry(timeUs, frame))
    }

    /**
     * Returns the newest frame whose audio has finished playing by [positionUs],
     * discarding it and everything older, or null if nothing is audible yet.
     */
    fun pollAudible(positionUs: Long): VisualizerFrame? {
        var newest: Entry? = null
        while (entries.isNotEmpty() && entries.first().timeUs <= positionUs) {
            newest = entries.removeFirst()
        }
        return newest?.frame
    }

    fun clear() = entries.clear()

    private companion object {
        /** ~10 s of frames at 40/s; far more than the sink ever buffers. */
        const val DEFAULT_CAPACITY = 400
    }
}
