package com.podbelly.core.playback

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.Id3Frame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdChaptersTest {

    // -------------------------------------------------------------------------
    // isAdChapterTitle
    // -------------------------------------------------------------------------

    @Test
    fun `exact ad titles match regardless of case and whitespace`() {
        listOf(
            "Ad",
            "ads",
            "  Advert  ",
            "ADVERTISEMENT",
            "Ad Break",
            "Commercial",
            "Commercial Break",
            "Promo",
            "Paid Promotion",
            "Sponsor",
            "Sponsors",
            "Sponsorship",
            "Sponsored",
            "Werbung",
        ).forEach { title ->
            assertTrue("expected \"$title\" to match", isAdChapterTitle(title))
        }
    }

    @Test
    fun `ad prefixes with trailing text match`() {
        listOf(
            "Sponsor: Squarespace",
            "Sponsored by Athletic Greens",
            "Ad: MeUndies",
            "Ad break 2",
            "Adverts",
            "Advertisement from our partners",
            "Promo: our other show",
            "Commercial break (2 of 3)",
        ).forEach { title ->
            assertTrue("expected \"$title\" to match", isAdChapterTitle(title))
        }
    }

    @Test
    fun `ordinary chapter titles do not match`() {
        listOf(
            "Ad Astra review",
            "Adventures in Coding",
            "Introduction",
            "The Sponsor Problem in Politics",
            "Additional listener questions",
            "Interview: Jane Doe",
            "",
        ).forEach { title ->
            assertFalse("expected \"$title\" not to match", isAdChapterTitle(title))
        }
    }

    // -------------------------------------------------------------------------
    // chaptersFromMetadata
    // -------------------------------------------------------------------------

    private fun chapterFrame(
        id: String,
        startMs: Int,
        endMs: Int,
        title: String? = null,
    ): ChapterFrame {
        val subFrames = if (title != null) {
            arrayOf<Id3Frame>(TextInformationFrame("TIT2", null, listOf(title)))
        } else {
            emptyArray()
        }
        return ChapterFrame(id, startMs, endMs, -1L, -1L, subFrames)
    }

    @Test
    fun `chapters are extracted sorted with titles`() {
        val metadata = Metadata(
            chapterFrame("ch1", 60_000, 120_000, "Sponsor"),
            chapterFrame("ch0", 0, 60_000, "Intro"),
        )

        val chapters = chaptersFromMetadata(listOf(metadata))

        assertEquals(2, chapters.size)
        assertEquals("Intro", chapters[0].title)
        assertEquals(0L, chapters[0].startTimeMs)
        assertEquals(60_000L, chapters[0].endTimeMs)
        assertEquals("Sponsor", chapters[1].title)
        assertEquals(60_000L, chapters[1].startTimeMs)
        assertEquals(120_000L, chapters[1].endTimeMs)
    }

    @Test
    fun `missing end times are repaired from the next chapter's start`() {
        // 0xFFFFFFFF ("not set") comes through media3 as int -1.
        val metadata = Metadata(
            chapterFrame("ch0", 0, -1, "Intro"),
            chapterFrame("ch1", 30_000, -1, "Main"),
        )

        val chapters = chaptersFromMetadata(listOf(metadata), durationMs = 90_000L)

        assertEquals(30_000L, chapters[0].endTimeMs)
        // The last chapter falls back to the episode duration.
        assertEquals(90_000L, chapters[1].endTimeMs)
    }

    @Test
    fun `last chapter with unknown end and unknown duration stays zero-length`() {
        val metadata = Metadata(chapterFrame("ch0", 30_000, -1, "Outro"))

        val chapters = chaptersFromMetadata(listOf(metadata), durationMs = 0L)

        assertEquals(30_000L, chapters[0].startTimeMs)
        assertEquals(30_000L, chapters[0].endTimeMs)
    }

    @Test
    fun `non-chapter metadata yields no chapters`() {
        val metadata = Metadata(TextInformationFrame("TIT2", null, listOf("Episode title")))

        assertTrue(chaptersFromMetadata(listOf(metadata)).isEmpty())
        assertTrue(chaptersFromMetadata(emptyList()).isEmpty())
    }

    @Test
    fun `chapter without a TIT2 sub-frame gets an empty title`() {
        val metadata = Metadata(chapterFrame("ch0", 0, 10_000))

        val chapters = chaptersFromMetadata(listOf(metadata))

        assertEquals("", chapters[0].title)
        assertFalse(isAdChapterTitle(chapters[0].title))
    }
}
