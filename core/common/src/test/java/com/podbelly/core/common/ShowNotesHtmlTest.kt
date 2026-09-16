package com.podbelly.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ShowNotesHtmlTest {

    @Test
    fun `plain text newlines become html line breaks`() {
        val raw = "Intro paragraph.\n\nIN THIS EPISODE:\n00:00 Introduction\n03:24 Writing Her First Memoir"

        val html = ShowNotesHtml.normalize(raw)

        assertEquals(
            "Intro paragraph.<br /><br />IN THIS EPISODE:<br />00:00 Introduction<br />03:24 Writing Her First Memoir",
            html,
        )
    }

    @Test
    fun `windows and old mac line endings are treated as newlines`() {
        assertEquals("a<br />b<br />c", ShowNotesHtml.normalize("a\r\nb\rc"))
    }

    @Test
    fun `notes that already use paragraph tags are left untouched`() {
        val raw = "<p>First.</p>\n<p>Second.</p>"
        assertSame(raw, ShowNotesHtml.normalize(raw))
    }

    @Test
    fun `notes that already use br tags are left untouched`() {
        val raw = "First.<br>\nSecond.<BR/>\nThird.<br />"
        assertSame(raw, ShowNotesHtml.normalize(raw))
    }

    @Test
    fun `notes with lists or headings are left untouched`() {
        val list = "<ul>\n<li>One</li>\n<li>Two</li>\n</ul>"
        assertSame(list, ShowNotesHtml.normalize(list))
        val heading = "<h2>Links</h2>\nSee below"
        assertSame(heading, ShowNotesHtml.normalize(heading))
    }

    @Test
    fun `inline-only html still gets line breaks from newlines`() {
        val raw = "Visit <a href=\"https://example.com\">our site</a>\nand <b>subscribe</b>"
        assertEquals(
            "Visit <a href=\"https://example.com\">our site</a><br />and <b>subscribe</b>",
            ShowNotesHtml.normalize(raw),
        )
    }

    @Test
    fun `text mentioning a p-word is not mistaken for a paragraph tag`() {
        // "<prose" is not a <p> or <pre> tag; only exact tag names count.
        val raw = "Guests <prose>\nnext"
        assertEquals("Guests <prose><br />next", ShowNotesHtml.normalize(raw))
    }

    @Test
    fun `empty and newline-free text pass through unchanged`() {
        assertEquals("", ShowNotesHtml.normalize(""))
        assertEquals("Just one line.", ShowNotesHtml.normalize("Just one line."))
    }

    @Test
    fun `trailing whitespace is trimmed but leading and inner whitespace kept`() {
        assertEquals(" a\n\nb", ShowNotesHtml.trimTrailingWhitespace(" a\n\nb\n\n ").toString())
        val untouched = "no trailing"
        assertSame(untouched, ShowNotesHtml.trimTrailingWhitespace(untouched))
        assertEquals("", ShowNotesHtml.trimTrailingWhitespace("\n\n").toString())
    }
}
