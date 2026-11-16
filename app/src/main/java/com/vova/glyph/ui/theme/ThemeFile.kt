package com.vova.glyph.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object ThemeFile {
    data class Entry(val name: String, val palette: ThemePalette)

    private val KEYS = listOf(
        "background",
        "surface",
        "surfaceHigh",
        "text",
        "gray",
        "dim",
        "line",
        "accent",
    )

    fun export(presets: List<ThemePreset>): String {
        val themes = JSONArray()
        presets.forEach { preset ->
            themes.put(
                JSONObject()
                    .put("name", preset.name)
                    .put(
                        "colors",
                        JSONObject()
                            .put("background", hex(preset.palette.background))
                            .put("surface", hex(preset.palette.surface))
                            .put("surfaceHigh", hex(preset.palette.surfaceHigh))
                            .put("text", hex(preset.palette.text))
                            .put("gray", hex(preset.palette.gray))
                            .put("dim", hex(preset.palette.dim))
                            .put("line", hex(preset.palette.line))
                            .put("accent", hex(preset.palette.accent)),
                    ),
            )
        }
        return JSONObject()
            .put("app", "PIXTONE")
            .put("kind", "theme-presets")
            .put("version", 1)
            .put("themes", themes)
            .toString(2)
    }

    fun parse(text: String): List<Entry> {
        val root = runCatching { JSONObject(text) }.getOrNull()
        val themes = root?.optJSONArray("themes")
            ?: runCatching { JSONArray(text) }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (index in 0 until themes.length()) {
                val item = themes.optJSONObject(index) ?: continue
                val palette = paletteOf(item.opt("colors")) ?: continue
                val name = item.optString("name").trim().ifBlank { "PRESET" }
                add(Entry(name, palette))
            }
        }
    }

    private fun paletteOf(value: Any?): ThemePalette? {
        val colors = when (value) {
            is JSONObject -> KEYS.map { key -> colorOf(value.opt(key) ?: value.opt(altKey(key))) }
            is JSONArray -> (0 until 8).map { index -> colorOf(value.opt(index)) }
            else -> return null
        }
        if (colors.size < 8 || colors.any { it == null }) return null
        return ThemePalette(
            background = Color(colors[0]!!),
            surface = Color(colors[1]!!),
            surfaceHigh = Color(colors[2]!!),
            text = Color(colors[3]!!),
            gray = Color(colors[4]!!),
            dim = Color(colors[5]!!),
            line = Color(colors[6]!!),
            accent = Color(colors[7]!!),
        )
    }

    private fun altKey(key: String): String =
        if (key == "surfaceHigh") "surface_high" else key

    private fun colorOf(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        is String -> parseHex(value)
        else -> null
    }

    private fun hex(color: Color): String = String.format(Locale.US, "#%08X", color.toArgb())

    private fun parseHex(raw: String): Int? {
        val hex = raw.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        val value = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) (0xFF000000L or value).toInt() else value.toInt()
    }
}
