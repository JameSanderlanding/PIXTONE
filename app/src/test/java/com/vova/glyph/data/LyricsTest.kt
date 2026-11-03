package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test
    fun `parses lrc lines and fractions`() {
        val lrc = """
            [00:12.34]Second line
            [00:05]First line
            [01:02.5]Third line
        """.trimIndent()

        val lines = parseLrc(lrc)

        assertEquals(3, lines.size)
        assertEquals(5_000L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(12_340L, lines[1].timeMs)
        assertEquals("Second line", lines[1].text)
        assertEquals(62_500L, lines[2].timeMs)
        assertEquals("Third line", lines[2].text)
    }

    @Test
    fun `ignores empty and invalid lines`() {
        val lrc = "no timestamp here\n[00:01]\n[00:02]Real line\n[bad]Ignored"

        val lines = parseLrc(lrc)

        assertEquals(1, lines.size)
        assertEquals("Real line", lines[0].text)
    }

    @Test
    fun `finds active line by position`() {
        val lines = listOf(
            LyricLine(0L, "one"),
            LyricLine(10_000L, "two"),
            LyricLine(20_000L, "three"),
        )

        assertEquals(1, activeLineIndex(lines, 15_000L))
        assertEquals(2, activeLineIndex(lines, 20_000L))
        assertEquals(-1, activeLineIndex(lines, -1L))
        assertEquals(2, activeLineIndex(lines, 99_000L))
    }

    @Test
    fun `formats lyric line as lrc`() {
        val lrc = listOf(LyricLine(65_430L, "Hello")).toLrc()

        assertEquals("[01:05.43]Hello", lrc)
    }

    @Test
    fun `adds intro and gap markers`() {
        val lines = listOf(
            LyricLine(10_000L, "first"),
            LyricLine(30_000L, "second"),
        )

        val result = withGapMarkers(lines, durationMs = 50_000L, minGapMs = 8_000L)

        assertEquals(5, result.size)
        assertEquals(0L, result[0].timeMs)
        assertTrue(result[0].instrumental)
        assertEquals(INTRO_MARKER, result[0].text)
        assertEquals("first", result[1].text)
        assertEquals("second", result[3].text)
        assertTrue(result[4].instrumental)
        assertEquals(GAP_MARKER, result[4].text)
    }
}
