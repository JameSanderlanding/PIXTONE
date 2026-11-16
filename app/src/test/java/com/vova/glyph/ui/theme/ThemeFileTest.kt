package com.vova.glyph.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFileTest {
    private val palette = ThemePalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceHigh = Color(0xFF141414),
        text = Color(0xFFF2F2F2),
        gray = Color(0xFF8B8B8B),
        dim = Color(0xFF5C5C5C),
        line = Color(0xFF272727),
        accent = Color(0xFFD71921),
    )

    @Test
    fun `exported file can be imported back`() {
        val preset = ThemePreset("custom_1", "MY THEME", palette)

        val parsed = ThemeFile.parse(ThemeFile.export(listOf(preset)))

        assertEquals(1, parsed.size)
        assertEquals("MY THEME", parsed.single().name)
        assertEquals(palette, parsed.single().palette)
    }

    @Test
    fun `six digit hex colors become opaque`() {
        val text = """
            {
              "themes": [
                {
                  "name": "HEX",
                  "colors": {
                    "background": "#000000",
                    "surface": "#0a0a0a",
                    "surfaceHigh": "#141414",
                    "text": "#f2f2f2",
                    "gray": "#8b8b8b",
                    "dim": "#5c5c5c",
                    "line": "#272727",
                    "accent": "#d71921"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = ThemeFile.parse(text)

        assertEquals(1, parsed.size)
        assertEquals(palette, parsed.single().palette)
        assertEquals(Color(0xFFD71921), parsed.single().palette.accent)
    }

    @Test
    fun `legacy prefs array with int colors is accepted`() {
        val colors = JSONArray()
        listOf(
            palette.background,
            palette.surface,
            palette.surfaceHigh,
            palette.text,
            palette.gray,
            palette.dim,
            palette.line,
            palette.accent,
        ).forEach { colors.put(it.toArgb()) }
        val text = JSONArray()
            .put(JSONObject().put("id", "custom_1").put("name", "LEGACY").put("colors", colors))
            .toString()

        val parsed = ThemeFile.parse(text)

        assertEquals("LEGACY", parsed.single().name)
        assertEquals(palette, parsed.single().palette)
    }

    @Test
    fun `broken colors are skipped`() {
        val text = """
            {
              "themes": [
                { "name": "BROKEN", "colors": { "background": "#zzzzzz" } },
                { "name": "EMPTY" }
              ]
            }
        """.trimIndent()

        assertTrue(ThemeFile.parse(text).isEmpty())
    }

    @Test
    fun `file without themes gives an empty list`() {
        assertTrue(ThemeFile.parse("not a theme file").isEmpty())
        assertTrue(ThemeFile.parse("{}").isEmpty())
        assertTrue(ThemeFile.parse("""{"themes":[]}""").isEmpty())
    }

    @Test
    fun `export without presets still has a valid structure`() {
        val parsed = ThemeFile.parse(ThemeFile.export(emptyList()))

        assertTrue(parsed.isEmpty())
    }
}
