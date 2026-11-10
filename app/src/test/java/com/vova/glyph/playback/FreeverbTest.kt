package com.vova.glyph.playback

import com.vova.glyph.playback.dsp.Freeverb
import com.vova.glyph.playback.dsp.ReverbSettings
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FreeverbTest {
    private fun reverbSettings(
        enabled: Boolean = true,
        roomSize: Float = 0.8f,
        damp: Float = 0.3f,
        wet: Float = 0.5f,
    ) = ReverbSettings().apply {
        this.enabled = enabled
        this.roomSize = roomSize
        this.damp = damp
        this.wet = wet
    }

    @Test
    fun zeroWetIsBitExact() {
        val verb = Freeverb(48000, 2, reverbSettings(wet = 0f))
        val samples = ShortArray(4096) { index -> (index * 37 % 30000 - 15000).toShort() }
        val expected = samples.copyOf()

        verb.process(samples, samples.size / 2)

        assertArrayEquals(expected, samples)
    }

    @Test
    fun disabledReverbIsBitExact() {
        val verb = Freeverb(44100, 1, reverbSettings(enabled = false, wet = 1f))
        val samples = ShortArray(1024) { index -> (index * 13).toShort() }
        val expected = samples.copyOf()

        verb.process(samples, samples.size)

        assertArrayEquals(expected, samples)
    }

    @Test
    fun impulseProducesDecayingTail() {
        val frames = 48000 * 2
        val verb = Freeverb(48000, 1, reverbSettings(roomSize = 0.9f, damp = 0.2f, wet = 0.6f))
        val samples = ShortArray(frames)
        samples[0] = Short.MAX_VALUE

        verb.process(samples, frames)

        val early = samples.copyOfRange(64, 4_800).sumOf { abs(it.toInt()) }
        val late = samples.copyOfRange(frames - 4_800, frames).sumOf { abs(it.toInt()) }
        assertTrue("no reverb tail", early > 0)
        assertTrue("tail does not decay: early=$early late=$late", late < early)
        assertTrue("tail is silent", late > 0)
    }

    @Test
    fun resetClearsTail() {
        val verb = Freeverb(44100, 1, reverbSettings(wet = 0.8f, roomSize = 0.95f))
        val impulse = ShortArray(44_100)
        impulse[0] = 12_000
        verb.process(impulse, impulse.size)

        verb.reset()
        val silence = ShortArray(44_100)
        verb.process(silence, silence.size)

        assertTrue(silence.all { it.toInt() == 0 })
    }

    @Test
    fun processesThirtySecondsQuickly() {
        val verb = Freeverb(48000, 2, reverbSettings(wet = 0.4f))
        val chunk = ShortArray(4_800) { index -> (index * 971 % 20_000 - 10_000).toShort() }
        val framesPerChunk = chunk.size / 2
        repeat(200) { verb.process(chunk, framesPerChunk) }

        val targetFrames = 48_000 * 30
        var processed = 0
        val startedAt = System.nanoTime()
        while (processed < targetFrames) {
            verb.process(chunk, framesPerChunk)
            processed += framesPerChunk
        }
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("30 s of audio took $elapsedMs ms", elapsedMs < 5_000)
    }
}
