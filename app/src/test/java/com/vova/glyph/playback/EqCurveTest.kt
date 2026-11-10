package com.vova.glyph.playback

import com.vova.glyph.playback.dsp.EqCurve
import com.vova.glyph.playback.dsp.EqPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqCurveTest {
    private val centers = intArrayOf(60, 230, 910, 3600, 14000)

    @Test
    fun flatKeepsBandsNeutral() {
        val gains = EqCurve.gains(EqPreset.FLAT, centers, 1f, -1500, 1500)

        assertTrue(gains.all { it == 0 })
    }

    @Test
    fun bassPresetBoostsLowBands() {
        val gains = EqCurve.gains(EqPreset.BASS, centers, 1f, -1500, 1500)

        assertTrue(gains[0] > 0)
        assertTrue(gains[0] > gains[2])
        assertTrue(gains.last() <= 0)
    }

    @Test
    fun speedShiftsThePresetCurve() {
        val band = intArrayOf(120)
        val normal = EqCurve.gains(EqPreset.BASS, band, 1f, -1500, 1500)[0]
        val slow = EqCurve.gains(EqPreset.BASS, band, 0.5f, -1500, 1500)[0]
        val fast = EqCurve.gains(EqPreset.BASS, band, 1.5f, -1500, 1500)[0]

        assertTrue(slow < normal)
        assertTrue(fast > normal)
    }

    @Test
    fun levelsStayInsideDeviceRange() {
        EqPreset.entries.forEach { preset ->
            val gains = EqCurve.gains(preset, centers, 1f, -900, 600)
            assertTrue(gains.all { it in -900..600 })
        }
    }

    @Test
    fun customPresetIsNeutral() {
        val gains = EqCurve.gains(EqPreset.CUSTOM, centers, 1f, -1500, 1500)

        assertEquals(0, gains.sum())
    }
}
