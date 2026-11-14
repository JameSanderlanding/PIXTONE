package com.vova.glyph.data

import org.json.JSONArray
import org.json.JSONObject

data class SoundPreset(
    val id: String,
    val name: String,
    val speed: Float,
    val eqEnabled: Boolean,
    val eqPreset: String,
    val bands: List<Int>,
    val bassEnabled: Boolean,
    val bassStrength: Int,
    val loudnessEnabled: Boolean,
    val loudnessGainMb: Int,
    val reverbEnabled: Boolean,
    val reverbRoom: Int,
    val reverbDamp: Int,
    val reverbWet: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("speed", speed.toDouble())
        .put("eq_enabled", eqEnabled)
        .put("eq_preset", eqPreset)
        .put("bands", JSONArray(bands))
        .put("bass_enabled", bassEnabled)
        .put("bass_strength", bassStrength)
        .put("loudness_enabled", loudnessEnabled)
        .put("loudness_gain", loudnessGainMb)
        .put("reverb_enabled", reverbEnabled)
        .put("reverb_room", reverbRoom)
        .put("reverb_damp", reverbDamp)
        .put("reverb_wet", reverbWet)

    companion object {
        fun fromJson(item: JSONObject): SoundPreset? {
            val id = item.optString("id")
            val name = item.optString("name")
            if (id.isBlank() || name.isBlank()) return null
            val bands = item.optJSONArray("bands")?.let { array ->
                List(array.length()) { index -> array.optInt(index) }
            }.orEmpty()
            return SoundPreset(
                id = id,
                name = name,
                speed = item.optDouble("speed", 1.0).toFloat(),
                eqEnabled = item.optBoolean("eq_enabled", false),
                eqPreset = item.optString("eq_preset", "FLAT"),
                bands = bands,
                bassEnabled = item.optBoolean("bass_enabled", false),
                bassStrength = item.optInt("bass_strength", 0),
                loudnessEnabled = item.optBoolean("loudness_enabled", false),
                loudnessGainMb = item.optInt("loudness_gain", 0),
                reverbEnabled = item.optBoolean("reverb_enabled", false),
                reverbRoom = item.optInt("reverb_room", 70),
                reverbDamp = item.optInt("reverb_damp", 40),
                reverbWet = item.optInt("reverb_wet", 30),
            )
        }
    }
}
