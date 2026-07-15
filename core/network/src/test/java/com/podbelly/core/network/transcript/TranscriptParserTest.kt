package com.podbelly.core.network.transcript

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TranscriptParserTest {

    private lateinit var parser: TranscriptParser

    @Before
    fun setUp() {
        parser = TranscriptParser(OkHttpClient())
    }

    // -------------------------------------------------------------------------
    // podcastindex JSON
    // -------------------------------------------------------------------------

    @Test
    fun `parses podcastindex JSON segments`() {
        val json = """
            {
              "version": "1.0.0",
              "segments": [
                { "speaker": "Alice", "startTime": 0.0, "endTime": 4.5, "body": "Welcome to the show." },
                { "speaker": "Bob", "startTime": 4.5, "endTime": 9.2, "body": "Thanks for having me." }
              ]
            }
        """.trimIndent()

        val cues = parser.parse(json, "application/json")

        assertEquals(2, cues.size)
        assertEquals(0L, cues[0].startMs)
        assertEquals("Welcome to the show.", cues[0].text)
        assertEquals(4500L, cues[1].startMs)
        assertEquals("Thanks for having me.", cues[1].text)
    }

    @Test
    fun `JSON fractional startTime converts to millis`() {
        val json = """{"segments":[{"startTime":1.25,"body":"Hi"}]}"""

        val cues = parser.parse(json, "application/json")

        assertEquals(1, cues.size)
        assertEquals(1250L, cues[0].startMs)
    }

    @Test
    fun `JSON tolerates start key instead of startTime`() {
        val json = """{"segments":[{"start":2.0,"body":"Alt key"}]}"""

        val cues = parser.parse(json, "application/json")

        assertEquals(1, cues.size)
        assertEquals(2000L, cues[0].startMs)
        assertEquals("Alt key", cues[0].text)
    }

    @Test
    fun `JSON skips segments without body or start time`() {
        val json = """
            {"segments":[
              {"startTime":1.0,"body":"Good"},
              {"startTime":2.0},
              {"body":"No time"},
              {"startTime":3.0,"body":""}
            ]}
        """.trimIndent()

        val cues = parser.parse(json, "application/json")

        assertEquals(1, cues.size)
        assertEquals("Good", cues[0].text)
    }

    // -------------------------------------------------------------------------
    // WebVTT
    // -------------------------------------------------------------------------

    @Test
    fun `parses VTT cue blocks`() {
        val vtt = """
            WEBVTT

            00:00:01.000 --> 00:00:04.000
            Hello and welcome.

            00:00:04.000 --> 00:00:08.500
            Today we talk about parsers.
        """.trimIndent()

        val cues = parser.parse(vtt, "text/vtt")

        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals("Hello and welcome.", cues[0].text)
        assertEquals(4000L, cues[1].startMs)
        assertEquals("Today we talk about parsers.", cues[1].text)
    }

    @Test
    fun `VTT joins multi-line cue text and strips voice tags`() {
        val vtt = """
            WEBVTT

            00:01.000 --> 00:04.000
            <v Alice>First line
            second line
        """.trimIndent()

        val cues = parser.parse(vtt, "text/vtt")

        assertEquals(1, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals("First line second line", cues[0].text)
    }

    @Test
    fun `VTT without hours parses MM SS timestamps`() {
        val vtt = """
            WEBVTT

            01:30.250 --> 01:35.000
            Ninety seconds in.
        """.trimIndent()

        val cues = parser.parse(vtt, "text/vtt")

        assertEquals(1, cues.size)
        assertEquals(90_250L, cues[0].startMs)
    }

    // -------------------------------------------------------------------------
    // SRT
    // -------------------------------------------------------------------------

    @Test
    fun `parses SRT blocks with index lines`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            First subtitle.

            2
            00:00:05,500 --> 00:00:09,000
            Second subtitle.
        """.trimIndent()

        val cues = parser.parse(srt, "application/x-subrip")

        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals("First subtitle.", cues[0].text)
        assertEquals(5500L, cues[1].startMs)
        assertEquals("Second subtitle.", cues[1].text)
    }

    @Test
    fun `application slash srt type is accepted`() {
        val srt = """
            1
            00:00:02,000 --> 00:00:03,000
            Hi there.
        """.trimIndent()

        val cues = parser.parse(srt, "application/srt")

        assertEquals(1, cues.size)
        assertEquals(2000L, cues[0].startMs)
    }

    // -------------------------------------------------------------------------
    // Format sniffing and merging
    // -------------------------------------------------------------------------

    @Test
    fun `unknown type sniffs JSON content`() {
        val json = """{"segments":[{"startTime":1.0,"body":"Sniffed"}]}"""

        val cues = parser.parse(json, null)

        assertEquals(1, cues.size)
        assertEquals("Sniffed", cues[0].text)
    }

    @Test
    fun `unknown type sniffs VTT content`() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nSniffed VTT"

        val cues = parser.parse(vtt, "text/plain")

        assertEquals(1, cues.size)
        assertEquals("Sniffed VTT", cues[0].text)
    }

    @Test
    fun `type with charset parameter is normalized`() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nWith charset"

        val cues = parser.parse(vtt, "text/vtt; charset=utf-8")

        assertEquals(1, cues.size)
    }

    @Test
    fun `consecutive cues with identical text are merged`() {
        val vtt = """
            WEBVTT

            00:00:01.000 --> 00:00:02.000
            Repeated caption

            00:00:02.000 --> 00:00:03.000
            Repeated caption

            00:00:03.000 --> 00:00:04.000
            New caption
        """.trimIndent()

        val cues = parser.parse(vtt, "text/vtt")

        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals("Repeated caption", cues[0].text)
        assertEquals("New caption", cues[1].text)
    }

    // -------------------------------------------------------------------------
    // Malformed input never crashes
    // -------------------------------------------------------------------------

    @Test
    fun `empty content returns empty list`() {
        assertTrue(parser.parse("", "text/vtt").isEmpty())
        assertTrue(parser.parse("   \n  ", "application/json").isEmpty())
    }

    @Test
    fun `malformed JSON returns empty list`() {
        assertTrue(parser.parse("{not json at all", "application/json").isEmpty())
        assertTrue(parser.parse("""{"segments":"nope"}""", "application/json").isEmpty())
        assertTrue(parser.parse("[1,2,3]", "application/json").isEmpty())
    }

    @Test
    fun `malformed VTT returns empty list`() {
        assertTrue(parser.parse("WEBVTT\n\nnot a timestamp\ntext", "text/vtt").isEmpty())
        assertTrue(parser.parse("random prose with no cues", "text/vtt").isEmpty())
    }

    @Test
    fun `garbage with unknown type returns empty list`() {
        assertTrue(parser.parse("<<<garbage>>>", null).isEmpty())
        assertTrue(parser.parse("<html><body>Not a transcript</body></html>", "text/html").isEmpty())
    }

    @Test
    fun `timestamp block missing text yields no cue`() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\n\n"
        assertTrue(parser.parse(vtt, "text/vtt").isEmpty())
    }

    // -------------------------------------------------------------------------
    // Timestamp parsing
    // -------------------------------------------------------------------------

    @Test
    fun `parseTimestampMs handles VTT and SRT separators`() {
        assertEquals(3_661_000L, parser.parseTimestampMs("01:01:01.000"))
        assertEquals(3_661_500L, parser.parseTimestampMs("01:01:01,500"))
        assertEquals(61_000L, parser.parseTimestampMs("01:01"))
        assertNull(parser.parseTimestampMs("not a time"))
        assertNull(parser.parseTimestampMs(""))
    }
}
