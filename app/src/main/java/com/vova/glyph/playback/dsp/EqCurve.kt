package com.vova.glyph.playback.dsp

import kotlin.math.log10
import kotlin.math.roundToInt

enum class EqPreset {
    FLAT,
    BASS,
    VOCAL,
    ROCK,
    CUSTOM;

    companion object {
        fun fromName(name: String?): EqPreset = entries.firstOrNull { it.name == name } ?: FLAT
    }
}

object EqCurve {
    private const val MIN_SPEED = 0.5f

    private const val MAX_SPEED = 1.5f

    private const val MIN_HZ = 20f

    private const val MAX_HZ = 20000f

    private data class Curve(val freqs: FloatArray, val weights: FloatArray)

    private val BASS = Curve(
        freqs = floatArrayOf(20f, 70f, 180f, 500f, 2000f, 8000f, 20000f),
        weights = floatArrayOf(0.9f, 0.85f, 0.55f, 0.15f, 0f, -0.1f, -0.15f),
    )

    private val VOCAL = Curve(
        freqs = floatArrayOf(20f, 120f, 300f, 1200f, 3200f, 8000f, 20000f),
        weights = floatArrayOf(-0.5f, -0.4f, 0.2f, 0.5f, 0.45f, -0.2f, -0.3f),
    )

    private val ROCK = Curve(
        freqs = floatArrayOf(20f, 80f, 300f, 800f, 2000f, 6000f, 20000f),
        weights = floatArrayOf(0.65f, 0.55f, -0.1f, -0.2f, 0.05f, 0.5f, 0.4f),
    )

    fun gains(
        preset: EqPreset,
        centersHz: IntArray,
        speed: Float,
        minMb: Int,
        maxMb: Int,
    ): IntArray {
        val safeSpeed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        return IntArray(centersHz.size) { index ->
            levelAt(preset, centersHz[index] / safeSpeed, minMb, maxMb)
        }
    }

    fun levelAt(preset: EqPreset, contentHz: Float, minMb: Int, maxMb: Int): Int {
        if (preset == EqPreset.FLAT || preset == EqPreset.CUSTOM) return 0
        val weight = weightAt(preset, contentHz)
        val bound = if (weight >= 0f) maxMb else -minMb
        return (weight * bound).roundToInt().coerceIn(minMb, maxMb)
    }

    private fun weightAt(preset: EqPreset, contentHz: Float): Float {
        val curve = when (preset) {
            EqPreset.BASS -> BASS
            EqPreset.VOCAL -> VOCAL
            EqPreset.ROCK -> ROCK
            else -> return 0f
        }
        val freq = contentHz.coerceIn(MIN_HZ, MAX_HZ)
        if (freq <= curve.freqs.first()) return curve.weights.first()
        for (index in 1 until curve.freqs.size) {
            val high = curve.freqs[index]
            if (freq <= high) {
                val low = curve.freqs[index - 1]
                val span = log10(high) - log10(low)
                val position = if (span <= 0f) 0f else (log10(freq) - log10(low)) / span
                return curve.weights[index - 1] +
                    (curve.weights[index] - curve.weights[index - 1]) * position
            }
        }
        return curve.weights.last()
    }
}
