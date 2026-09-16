package com.podbelly.core.common

import android.text.Html
import android.text.Spanned

/**
 * Turns raw show notes from a feed into something [Html.fromHtml] renders well.
 *
 * Feeds are inconsistent: some ship real HTML (`<p>`, `<br>`, `<ul>`), others
 * ship plain text whose paragraphs and chapter lists are separated only by
 * newline characters. HTML rendering collapses whitespace, so plain-text notes
 * come out as one solid block. This mirrors AntennaPod's `ShownotesCleaner`:
 * when the notes contain no HTML line breaks or paragraphs, every newline is
 * promoted to a `<br>` so the author's layout survives.
 */
object ShowNotesHtml {

    /** Any tag that already produces a line break when rendered as HTML. */
    private val BLOCK_TAG_REGEX = Regex(
        "<(br|p|div|li|ul|ol|h[1-6]|blockquote|pre|table|tr)\\b",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Returns [raw] as HTML that preserves its visible line structure.
     *
     * - Notes that already use HTML line breaks or block tags are returned as-is.
     * - Plain-text notes have `\r\n` / `\r` normalised to `\n` and every `\n`
     *   replaced with `<br />`, so paragraph gaps and one-per-line chapter
     *   timestamps render on their own lines.
     */
    fun normalize(raw: String): String {
        if (raw.isEmpty()) return raw
        if (BLOCK_TAG_REGEX.containsMatchIn(raw)) return raw
        return raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\n", "<br />")
    }

    /**
     * Renders [raw] show notes for a `TextView`, with plain-text line breaks
     * preserved and the trailing blank lines `Html.fromHtml` leaves after a
     * closing `</p>` trimmed away.
     */
    fun toSpanned(raw: String): CharSequence {
        val spanned: Spanned = Html.fromHtml(normalize(raw), Html.FROM_HTML_MODE_COMPACT)
        return trimTrailingWhitespace(spanned)
    }

    /** Drops trailing whitespace (including newlines) while keeping spans intact. */
    internal fun trimTrailingWhitespace(text: CharSequence): CharSequence {
        var end = text.length
        while (end > 0 && text[end - 1].isWhitespace()) end--
        return if (end == text.length) text else text.subSequence(0, end)
    }
}
