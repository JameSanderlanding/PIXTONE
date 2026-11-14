package com.vova.glyph.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundPresetTest {
    @Test
    fun `preset survives a json round trip`() {
        val preset = SoundPreset(
            id = "preset-1",
            name = "Night drive",
            speed = 1.15f,
            eqEnabled = true,
            eqPreset = "CUSTOM",
            bands = listOf(-300, 0, 250, 600),
            bassEnabled = true,
            bassStrength = 420,
            loudnessEnabled = true,
            loudnessGainMb = 350,
            reverbEnabled = true,
            reverbRoom = 80,
            reverbDamp = 35,
            reverbWet = 45,
        )

        val restored = SoundPreset.fromJson(preset.toJson())

        assertEquals(preset, restored)
    }

    @Test
    fun `preset without id or name is ignored`() {
        assertNull(SoundPreset.fromJson(SoundPresetJson.blank))
        assertNull(SoundPreset.fromJson(SoundPresetJson.noName))
    }
}

private object SoundPresetJson {
    val blank = JSONObject().put("id", "").put("name", "Broken")

    val noName = JSONObject().put("id", "preset-2").put("name", "")
}
