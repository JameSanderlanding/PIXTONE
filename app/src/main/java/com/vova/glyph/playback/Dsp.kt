package com.vova.glyph.playback

import android.os.Bundle
import com.vova.glyph.playback.dsp.EqPreset

internal const val CMD_QUERY_DSP = "com.vova.glyph.QUERY_DSP"

internal const val CMD_DSP_CHANGED = "com.vova.glyph.DSP_CHANGED"

data class DspBand(
    val centerHz: Int,
    val levelMb: Int,
)

data class DspUiState(
    val queried: Boolean = false,
    val attached: Boolean = false,
    val bands: List<DspBand> = emptyList(),
    val levelMinMb: Int = -1500,
    val levelMaxMb: Int = 1500,
    val eqEnabled: Boolean = false,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val bassSupported: Boolean = false,
    val bassEnabled: Boolean = false,
    val bassStrength: Int = 0,
    val loudnessSupported: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val loudnessGainMb: Int = 0,
    val speed: Float = 1f,
    val reverbEnabled: Boolean = false,
    val reverbRoom: Int = 70,
    val reverbDamp: Int = 40,
    val reverbWet: Int = 30,
) {
    val eqSupported: Boolean get() = bands.isNotEmpty()

    val anyEffectEnabled: Boolean
        get() = eqEnabled || bassEnabled || loudnessEnabled || reverbEnabled || speed != 1f
}

internal fun encodeBands(levels: List<Int>): String = levels.joinToString(",")

internal fun decodeBands(encoded: String): List<Int> =
    encoded.split(',').mapNotNull { it.trim().toIntOrNull() }

internal fun DspUiState.toBundle(): Bundle = Bundle().apply {
    putBoolean(KEY_QUERIED, queried)
    putBoolean(KEY_ATTACHED, attached)
    putIntArray(KEY_CENTERS, bands.map { it.centerHz }.toIntArray())
    putIntArray(KEY_LEVELS, bands.map { it.levelMb }.toIntArray())
    putInt(KEY_LEVEL_MIN, levelMinMb)
    putInt(KEY_LEVEL_MAX, levelMaxMb)
    putBoolean(KEY_EQ_ENABLED, eqEnabled)
    putString(KEY_EQ_PRESET, eqPreset.name)
    putBoolean(KEY_BASS_SUPPORTED, bassSupported)
    putBoolean(KEY_BASS_ENABLED, bassEnabled)
    putInt(KEY_BASS_STRENGTH, bassStrength)
    putBoolean(KEY_LOUDNESS_SUPPORTED, loudnessSupported)
    putBoolean(KEY_LOUDNESS_ENABLED, loudnessEnabled)
    putInt(KEY_LOUDNESS_GAIN, loudnessGainMb)
    putFloat(KEY_SPEED, speed)
    putBoolean(KEY_REVERB_ENABLED, reverbEnabled)
    putInt(KEY_REVERB_ROOM, reverbRoom)
    putInt(KEY_REVERB_DAMP, reverbDamp)
    putInt(KEY_REVERB_WET, reverbWet)
}

internal fun dspFromBundle(bundle: Bundle?): DspUiState {
    if (bundle == null) return DspUiState()
    val centers = bundle.getIntArray(KEY_CENTERS) ?: IntArray(0)
    val levels = bundle.getIntArray(KEY_LEVELS) ?: IntArray(0)
    val bands = centers.indices.map { index ->
        DspBand(centers[index], levels.getOrElse(index) { 0 })
    }
    return DspUiState(
        queried = bundle.getBoolean(KEY_QUERIED, false),
        attached = bundle.getBoolean(KEY_ATTACHED, false),
        bands = bands,
        levelMinMb = bundle.getInt(KEY_LEVEL_MIN, -1500),
        levelMaxMb = bundle.getInt(KEY_LEVEL_MAX, 1500),
        eqEnabled = bundle.getBoolean(KEY_EQ_ENABLED, false),
        eqPreset = EqPreset.fromName(bundle.getString(KEY_EQ_PRESET)),
        bassSupported = bundle.getBoolean(KEY_BASS_SUPPORTED, false),
        bassEnabled = bundle.getBoolean(KEY_BASS_ENABLED, false),
        bassStrength = bundle.getInt(KEY_BASS_STRENGTH, 0),
        loudnessSupported = bundle.getBoolean(KEY_LOUDNESS_SUPPORTED, false),
        loudnessEnabled = bundle.getBoolean(KEY_LOUDNESS_ENABLED, false),
        loudnessGainMb = bundle.getInt(KEY_LOUDNESS_GAIN, 0),
        speed = bundle.getFloat(KEY_SPEED, 1f),
        reverbEnabled = bundle.getBoolean(KEY_REVERB_ENABLED, false),
        reverbRoom = bundle.getInt(KEY_REVERB_ROOM, 70),
        reverbDamp = bundle.getInt(KEY_REVERB_DAMP, 40),
        reverbWet = bundle.getInt(KEY_REVERB_WET, 30),
    )
}

private const val KEY_QUERIED = "queried"

private const val KEY_ATTACHED = "attached"

private const val KEY_CENTERS = "centers"

private const val KEY_LEVELS = "levels"

private const val KEY_LEVEL_MIN = "level_min"

private const val KEY_LEVEL_MAX = "level_max"

private const val KEY_EQ_ENABLED = "eq_enabled"

private const val KEY_EQ_PRESET = "eq_preset"

private const val KEY_BASS_SUPPORTED = "bass_supported"

private const val KEY_BASS_ENABLED = "bass_enabled"

private const val KEY_BASS_STRENGTH = "bass_strength"

private const val KEY_LOUDNESS_SUPPORTED = "loudness_supported"

private const val KEY_LOUDNESS_ENABLED = "loudness_enabled"

private const val KEY_LOUDNESS_GAIN = "loudness_gain"

private const val KEY_SPEED = "speed"

private const val KEY_REVERB_ENABLED = "reverb_enabled"

private const val KEY_REVERB_ROOM = "reverb_room"

private const val KEY_REVERB_DAMP = "reverb_damp"

private const val KEY_REVERB_WET = "reverb_wet"
