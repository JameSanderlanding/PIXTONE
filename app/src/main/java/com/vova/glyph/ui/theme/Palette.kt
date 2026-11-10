package com.vova.glyph.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject

data class ThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val gray: Color,
    val dim: Color,
    val line: Color,
    val accent: Color,
) {
    fun colorOf(slot: ColorSlot): Color = when (slot) {
        ColorSlot.BACKGROUND -> background
        ColorSlot.SURFACE -> surface
        ColorSlot.SURFACE_HIGH -> surfaceHigh
        ColorSlot.TEXT -> text
        ColorSlot.GRAY -> gray
        ColorSlot.DIM -> dim
        ColorSlot.LINE -> line
        ColorSlot.ACCENT -> accent
    }

    fun with(slot: ColorSlot, color: Color): ThemePalette = when (slot) {
        ColorSlot.BACKGROUND -> copy(background = color)
        ColorSlot.SURFACE -> copy(surface = color)
        ColorSlot.SURFACE_HIGH -> copy(surfaceHigh = color)
        ColorSlot.TEXT -> copy(text = color)
        ColorSlot.GRAY -> copy(gray = color)
        ColorSlot.DIM -> copy(dim = color)
        ColorSlot.LINE -> copy(line = color)
        ColorSlot.ACCENT -> copy(accent = color)
    }
}

enum class ColorSlot(val label: String) {
    BACKGROUND("ФОН"),
    SURFACE("ПОВЕРХНОСТЬ"),
    SURFACE_HIGH("ПОВЕРХНОСТЬ 2"),
    TEXT("ОСНОВНОЙ ТЕКСТ"),
    GRAY("СЕРЫЙ"),
    DIM("ТУСКЛЫЙ"),
    LINE("ЛИНИИ"),
    ACCENT("АКЦЕНТ"),
}

data class ThemePreset(
    val id: String,
    val name: String,
    val palette: ThemePalette,
)

val DefaultPalette = ThemePalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceHigh = Color(0xFF141414),
    text = Color(0xFFF2F2F2),
    gray = Color(0xFF8B8B8B),
    dim = Color(0xFF5C5C5C),
    line = Color(0xFF272727),
    accent = Color(0xFFD71921),
)

val ThemePresets: List<ThemePreset> = listOf(
    ThemePreset("glyph", "PIXTONE", DefaultPalette),
    ThemePreset(
        "amber",
        "AMBER",
        ThemePalette(
            background = Color(0xFF050403),
            surface = Color(0xFF0D0B07),
            surfaceHigh = Color(0xFF17130C),
            text = Color(0xFFF7F1E3),
            gray = Color(0xFFA3967A),
            dim = Color(0xFF6B6049),
            line = Color(0xFF2B2419),
            accent = Color(0xFFFFB000),
        ),
    ),
    ThemePreset(
        "acid",
        "ACID",
        ThemePalette(
            background = Color(0xFF000000),
            surface = Color(0xFF070A04),
            surfaceHigh = Color(0xFF0F1508),
            text = Color(0xFFF0FFE8),
            gray = Color(0xFF93A382),
            dim = Color(0xFF58634D),
            line = Color(0xFF1E2917),
            accent = Color(0xFFA6FF00),
        ),
    ),
    ThemePreset(
        "ice",
        "ICE",
        ThemePalette(
            background = Color(0xFF03060B),
            surface = Color(0xFF070C14),
            surfaceHigh = Color(0xFF0D1520),
            text = Color(0xFFEAF3FF),
            gray = Color(0xFF8CA3BD),
            dim = Color(0xFF566B85),
            line = Color(0xFF16222F),
            accent = Color(0xFF2FA8FF),
        ),
    ),
    ThemePreset(
        "violet",
        "VIOLET",
        ThemePalette(
            background = Color(0xFF060310),
            surface = Color(0xFF0B0718),
            surfaceHigh = Color(0xFF140D25),
            text = Color(0xFFF1ECFF),
            gray = Color(0xFFA092C4),
            dim = Color(0xFF625580),
            line = Color(0xFF251C3A),
            accent = Color(0xFFB14BFF),
        ),
    ),
    ThemePreset(
        "mono",
        "MONO",
        ThemePalette(
            background = Color(0xFF000000),
            surface = Color(0xFF0A0A0A),
            surfaceHigh = Color(0xFF161616),
            text = Color(0xFFFFFFFF),
            gray = Color(0xFF9A9A9A),
            dim = Color(0xFF5E5E5E),
            line = Color(0xFF2A2A2A),
            accent = Color(0xFFFFFFFF),
        ),
    ),
)

val AdaptivePreset = ThemePreset("adaptive", "АДАПТИВ", DefaultPalette)

object ThemeController {
    private var prefs: SharedPreferences? = null

    var basePalette by mutableStateOf(DefaultPalette)
        private set

    var coverPalette by mutableStateOf<ThemePalette?>(null)
        private set

    var presetId by mutableStateOf(ThemePresets.first().id)
        private set

    var customPresets by mutableStateOf<List<ThemePreset>>(emptyList())
        private set

    val adaptive: Boolean get() = presetId == AdaptivePreset.id

    private var draftPalette: ThemePalette? = null
    private var draftPresetId: String? = null
    private var draftEditingId: String? = null
    private var draftActive by mutableStateOf(false)

    val palette: ThemePalette
        get() = when {
            draftActive -> basePalette
            !adaptive -> basePalette
            else -> coverPalette ?: DefaultPalette
        }

    val isEditingDraft: Boolean get() = draftEditingId != null

    val presetName: String
        get() = allPresets().firstOrNull { it.id == presetId }?.name ?: ThemePresets.first().name

    fun allPresets(): List<ThemePreset> = ThemePresets + AdaptivePreset + customPresets

    fun attach(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext
            .getSharedPreferences("glyph_theme", Context.MODE_PRIVATE)
        prefs = sp
        presetId = sp.getString(KEY_PRESET, ThemePresets.first().id) ?: ThemePresets.first().id
        basePalette = ThemePalette(
            background = Color(sp.getInt("bg", DefaultPalette.background.toArgb())),
            surface = Color(sp.getInt("surface", DefaultPalette.surface.toArgb())),
            surfaceHigh = Color(sp.getInt("surface_high", DefaultPalette.surfaceHigh.toArgb())),
            text = Color(sp.getInt("text", DefaultPalette.text.toArgb())),
            gray = Color(sp.getInt("gray", DefaultPalette.gray.toArgb())),
            dim = Color(sp.getInt("dim", DefaultPalette.dim.toArgb())),
            line = Color(sp.getInt("line", DefaultPalette.line.toArgb())),
            accent = Color(sp.getInt("accent", DefaultPalette.accent.toArgb())),
        )
        customPresets = loadCustomPresets(sp)
    }

    fun applyPreset(preset: ThemePreset) {
        presetId = preset.id
        if (preset.id != AdaptivePreset.id) {
            basePalette = preset.palette
        }
        save()
    }

    fun updateCoverPalette(value: ThemePalette?) {
        coverPalette = value
    }

    fun reset() {
        applyPreset(ThemePresets.first())
    }

    fun startDraft() {
        if (draftActive) return
        draftPalette = basePalette
        draftPresetId = presetId
        draftEditingId = null
        draftActive = true
    }

    fun startEdit(preset: ThemePreset) {
        if (!draftActive) {
            draftPalette = basePalette
            draftPresetId = presetId
            draftActive = true
        }
        draftEditingId = preset.id
        basePalette = preset.palette
        presetId = preset.id
    }

    fun setDraftColor(slot: ColorSlot, color: Color) {
        basePalette = basePalette.with(slot, color)
    }

    fun cancelDraft() {
        draftPalette?.let { basePalette = it }
        draftPresetId?.let { presetId = it }
        draftPalette = null
        draftPresetId = null
        draftEditingId = null
        draftActive = false
    }

    fun deletePreset(preset: ThemePreset) {
        if (customPresets.none { it.id == preset.id }) return
        customPresets = customPresets.filterNot { it.id == preset.id }
        if (draftEditingId == preset.id) {
            draftEditingId = null
        }
        if (presetId == preset.id) {
            applyPreset(ThemePresets.first())
        }
        saveCustomPresets()
    }

    fun saveDraft(name: String) {
        val trimmed = name.trim().ifBlank { "PRESET" }
        val editingId = draftEditingId
        if (editingId != null) {
            customPresets = customPresets.map { preset ->
                if (preset.id == editingId) {
                    preset.copy(name = trimmed, palette = basePalette)
                } else {
                    preset
                }
            }
            presetId = editingId
        } else {
            val created = ThemePreset(
                id = "custom_${System.currentTimeMillis()}",
                name = trimmed,
                palette = basePalette,
            )
            customPresets = customPresets + created
            presetId = created.id
        }
        draftPalette = null
        draftPresetId = null
        draftEditingId = null
        draftActive = false
        save()
        saveCustomPresets()
    }

    private fun save() {
        val sp = prefs ?: return
        sp.edit()
            .putString(KEY_PRESET, presetId)
            .putInt("bg", basePalette.background.toArgb())
            .putInt("surface", basePalette.surface.toArgb())
            .putInt("surface_high", basePalette.surfaceHigh.toArgb())
            .putInt("text", basePalette.text.toArgb())
            .putInt("gray", basePalette.gray.toArgb())
            .putInt("dim", basePalette.dim.toArgb())
            .putInt("line", basePalette.line.toArgb())
            .putInt("accent", basePalette.accent.toArgb())
            .apply()
    }

    private fun saveCustomPresets() {
        val sp = prefs ?: return
        val array = JSONArray()
        customPresets.forEach { preset ->
            array.put(
                JSONObject()
                    .put("id", preset.id)
                    .put("name", preset.name)
                    .put(
                        "colors",
                        JSONArray().apply {
                            put(preset.palette.background.toArgb())
                            put(preset.palette.surface.toArgb())
                            put(preset.palette.surfaceHigh.toArgb())
                            put(preset.palette.text.toArgb())
                            put(preset.palette.gray.toArgb())
                            put(preset.palette.dim.toArgb())
                            put(preset.palette.line.toArgb())
                            put(preset.palette.accent.toArgb())
                        },
                    )
            )
        }
        sp.edit().putString(KEY_CUSTOM, array.toString()).apply()
    }

    private fun loadCustomPresets(sp: SharedPreferences): List<ThemePreset> = runCatching {
        val raw = sp.getString(KEY_CUSTOM, null) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val colors = item.optJSONArray("colors") ?: continue
                if (colors.length() < 8) continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(
                    ThemePreset(
                        id = id,
                        name = item.optString("name").ifBlank { "PRESET" },
                        palette = ThemePalette(
                            background = Color(colors.getInt(0)),
                            surface = Color(colors.getInt(1)),
                            surfaceHigh = Color(colors.getInt(2)),
                            text = Color(colors.getInt(3)),
                            gray = Color(colors.getInt(4)),
                            dim = Color(colors.getInt(5)),
                            line = Color(colors.getInt(6)),
                            accent = Color(colors.getInt(7)),
                        ),
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private const val KEY_PRESET = "preset"
    private const val KEY_CUSTOM = "custom_presets"
}
