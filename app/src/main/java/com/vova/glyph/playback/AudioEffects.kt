package com.vova.glyph.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.vova.glyph.data.Prefs
import com.vova.glyph.playback.dsp.EqCurve
import com.vova.glyph.playback.dsp.EqPreset
import com.vova.glyph.playback.dsp.ReverbAudioProcessor

class AudioEffects(
    private val prefs: Prefs,
    private val reverb: ReverbAudioProcessor,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var sessionId: Int = SESSION_UNSET

    fun attach(id: Int) {
        if (id <= 0 || id == sessionId) return
        release()
        sessionId = id
        equalizer = create { Equalizer(0, id) }
        bassBoost = create { BassBoost(0, id) }
        loudness = create { LoudnessEnhancer(id) }
    }

    fun applyAll(speed: Float) {
        applyEqualizer(speed)
        applyBass()
        applyLoudness()
        applyReverb()
    }

    fun applyKey(key: String, speed: Float) {
        when {
            key == "fx_speed" ||
                key == "fx_eq_enabled" ||
                key == "fx_eq_preset" ||
                key == "fx_eq_bands" -> applyEqualizer(speed)

            key == "fx_bass_enabled" || key == "fx_bass_strength" -> applyBass()

            key == "fx_loudness_enabled" || key == "fx_loudness_gain" -> applyLoudness()

            key.startsWith("fx_reverb") -> applyReverb()
        }
    }

    fun snapshot(speed: Float): DspUiState {
        val safeSpeed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        val preset = EqPreset.fromName(prefs.fxEqPreset)
        val centers = equalizer?.let { effect ->
            runCatching {
                val count = effect.numberOfBands.toInt()
                IntArray(count) { index -> effect.getCenterFreq(index.toShort()) }
            }.getOrDefault(IntArray(0))
        } ?: IntArray(0)
        val range = equalizer?.let { effect ->
            runCatching { effect.bandLevelRange }.getOrNull()
        }
        val minMb = range?.getOrNull(0)?.toInt() ?: DEFAULT_MIN_MB
        val maxMb = range?.getOrNull(1)?.toInt() ?: DEFAULT_MAX_MB
        val levels = when {
            centers.isEmpty() -> IntArray(0)
            preset == EqPreset.CUSTOM -> {
                val stored = decodeBands(prefs.fxEqBands)
                IntArray(centers.size) { index -> stored.getOrElse(index) { 0 } }
            }
            else -> EqCurve.gains(preset, centers, safeSpeed, minMb, maxMb)
        }
        return DspUiState(
            queried = true,
            attached = sessionId > 0,
            bands = centers.indices.map { index ->
                DspBand(centers[index], levels[index].coerceIn(minMb, maxMb))
            },
            levelMinMb = minMb,
            levelMaxMb = maxMb,
            eqEnabled = prefs.fxEqEnabled,
            eqPreset = preset,
            bassSupported = bassBoost != null,
            bassEnabled = prefs.fxBassEnabled,
            bassStrength = prefs.fxBassStrength,
            loudnessSupported = loudness != null,
            loudnessEnabled = prefs.fxLoudnessEnabled,
            loudnessGainMb = prefs.fxLoudnessGain,
            speed = safeSpeed,
            reverbEnabled = prefs.fxReverbEnabled,
            reverbRoom = prefs.fxReverbRoom,
            reverbDamp = prefs.fxReverbDamp,
            reverbWet = prefs.fxReverbWet,
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        loudness = null
        sessionId = SESSION_UNSET
    }

    private fun applyEqualizer(speed: Float) {
        val effect = equalizer ?: return
        runCatching {
            val enabled = prefs.fxEqEnabled
            if (!enabled) {
                effect.setEnabled(false)
                return
            }
            val count = effect.numberOfBands.toInt()
            if (count <= 0) return
            val range = effect.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: DEFAULT_MIN_MB
            val maxMb = range.getOrNull(1)?.toInt() ?: DEFAULT_MAX_MB
            val preset = EqPreset.fromName(prefs.fxEqPreset)
            val gains = if (preset == EqPreset.CUSTOM) {
                val stored = decodeBands(prefs.fxEqBands)
                IntArray(count) { index -> stored.getOrElse(index) { 0 } }
            } else {
                val centers = IntArray(count) { index -> effect.getCenterFreq(index.toShort()) }
                EqCurve.gains(preset, centers, speed, minMb, maxMb)
            }
            gains.forEachIndexed { index, level ->
                effect.setBandLevel(index.toShort(), level.coerceIn(minMb, maxMb).toShort())
            }
            effect.setEnabled(true)
        }
    }

    private fun applyBass() {
        val effect = bassBoost ?: return
        runCatching {
            effect.setStrength(prefs.fxBassStrength.toShort())
            effect.setEnabled(prefs.fxBassEnabled)
        }
    }

    private fun applyLoudness() {
        val effect = loudness ?: return
        runCatching {
            effect.setTargetGain(prefs.fxLoudnessGain)
            effect.setEnabled(prefs.fxLoudnessEnabled)
        }
    }

    private fun applyReverb() {
        reverb.settings.enabled = prefs.fxReverbEnabled
        reverb.settings.roomSize = prefs.fxReverbRoom / 100f
        reverb.settings.damp = prefs.fxReverbDamp / 100f
        reverb.settings.wet = prefs.fxReverbWet / 100f
    }

    private fun <T : AudioEffect> create(factory: () -> T): T? {
        val effect = runCatching { factory() }.getOrNull() ?: return null
        val controlled = runCatching { effect.hasControl() }.getOrDefault(false)
        if (!controlled) {
            runCatching { effect.release() }
            return null
        }
        return effect
    }

    private companion object {
        const val SESSION_UNSET = -1
        const val DEFAULT_MIN_MB = -1500
        const val DEFAULT_MAX_MB = 1500
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1.5f
    }
}
