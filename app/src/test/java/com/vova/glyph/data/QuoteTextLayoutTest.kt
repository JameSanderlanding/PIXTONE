package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteTextLayoutTest {
    private val measureByLength: (String) -> Float = { it.length * 10f }

    private val measureByLengthAndSize: (String, Float) -> Float =
        { text, size -> text.length * size * 0.5f }

    @Test
    fun `keeps explicit line breaks`() {
        val lines = wrapQuoteText("one\ntwo\n\nthree", 10_000f, measureByLength)
        assertEquals(listOf("one", "two", "", "three"), lines)
    }

    @Test
    fun `wraps long lines at word boundaries`() {
        val lines = wrapQuoteText("alpha beta gamma delta", 100f, measureByLength)
        assertEquals(listOf("alpha beta", "gamma", "delta"), lines)
    }

    @Test
    fun `breaks a word that is wider than the line`() {
        val lines = wrapQuoteText("abcdefghij", 30f, measureByLength)
        assertEquals(listOf("abc", "def", "ghi", "j"), lines)
    }

    @Test
    fun `keeps the requested size when everything fits`() {
        val layout = fitQuoteText(
            text = "short line",
            maxWidth = 1_000f,
            maxHeight = 400f,
            maxFontSize = 64f,
            minFontSize = 24f,
            step = 4f,
            lineHeightFactor = 1.3f,
            measure = measureByLengthAndSize,
        )
        assertEquals(64f, layout.fontSize, 0.001f)
        assertEquals(listOf("short line"), layout.lines)
        assertEquals(64f * 1.3f, layout.lineHeight, 0.001f)
    }

    @Test
    fun `shrinks the font until the block fits`() {
        val layout = fitQuoteText(
            text = "one two three four five six seven eight",
            maxWidth = 100f,
            maxHeight = 100f,
            maxFontSize = 32f,
            minFontSize = 12f,
            step = 2f,
            lineHeightFactor = 1.3f,
            measure = measureByLengthAndSize,
        )
        assertTrue(layout.fontSize < 32f)
        assertTrue(layout.fontSize >= 12f)
        assertTrue(layout.height <= 100f)
    }

    @Test
    fun `never goes below the minimum size`() {
        val layout = fitQuoteText(
            text = "a very long quote that cannot possibly fit into this tiny box",
            maxWidth = 20f,
            maxHeight = 10f,
            maxFontSize = 40f,
            minFontSize = 16f,
            step = 4f,
            lineHeightFactor = 1.3f,
            measure = measureByLengthAndSize,
        )
        assertEquals(16f, layout.fontSize, 0.001f)
        assertTrue(layout.lines.isNotEmpty())
    }

    @Test
    fun `keeps an empty quote empty`() {
        val layout = fitQuoteText(
            text = "",
            maxWidth = 100f,
            maxHeight = 100f,
            maxFontSize = 32f,
            minFontSize = 12f,
            step = 2f,
            lineHeightFactor = 1.3f,
            measure = measureByLengthAndSize,
        )
        assertTrue(layout.lines.isEmpty())
        assertEquals(0f, layout.height, 0.001f)
    }
}
