package com.vova.glyph.playback.dsp

class ReverbSettings {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var roomSize: Float = DEFAULT_ROOM_SIZE

    @Volatile
    var damp: Float = DEFAULT_DAMP

    @Volatile
    var wet: Float = DEFAULT_WET

    @Volatile
    var width: Float = DEFAULT_WIDTH
}

private const val DEFAULT_ROOM_SIZE = 0.7f

private const val DEFAULT_DAMP = 0.4f

private const val DEFAULT_WET = 0.3f

private const val DEFAULT_WIDTH = 1f

private const val COMB_COUNT = 8

private const val ALLPASS_COUNT = 4

private const val STEREO_SPREAD = 23

private const val BASE_SAMPLE_RATE = 44100f

private const val INPUT_GAIN = 0.015f

private const val ALLPASS_FEEDBACK = 0.5f

private const val PCM_SCALE = 32768f

private val COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)

private val ALLPASS_TUNING = intArrayOf(556, 441, 341, 225)

class Freeverb(
    sampleRate: Int,
    channelCount: Int,
    private val settings: ReverbSettings,
) {
    private val channels = channelCount.coerceAtLeast(1)
    private val scale = sampleRate.coerceIn(8000, 192000) / BASE_SAMPLE_RATE
    private val combs = Array(channels) { channel ->
        Array(COMB_COUNT) { index -> Comb(delayOf(COMB_TUNING[index] + spread(channel))) }
    }
    private val allpasses = Array(channels) { channel ->
        Array(ALLPASS_COUNT) { index -> Allpass(delayOf(ALLPASS_TUNING[index] + spread(channel))) }
    }
    private var active = false

    fun process(samples: ShortArray, frameCount: Int) {
        val wet = settings.wet.coerceIn(0f, 1f)
        if (!settings.enabled || wet <= 0f) {
            if (active) reset()
            return
        }
        active = true

        val dry = 1f - wet
        val width = settings.width.coerceIn(0f, 1f)
        val wet1 = wet * (width / 2f + 0.5f)
        val wet2 = wet * ((1f - width) / 2f)
        val feedback = (settings.roomSize.coerceIn(0f, 1f) * 0.28f + 0.7f).coerceIn(0.7f, 0.98f)
        val damp = settings.damp.coerceIn(0f, 1f)
        val frames = frameCount.coerceIn(0, samples.size / channels)

        for (frame in 0 until frames) {
            val base = frame * channels
            if (channels == 1) {
                val input = samples[base] / PCM_SCALE
                var reverb = 0f
                val mono = input * INPUT_GAIN
                for (comb in combs[0]) reverb += comb.process(mono, feedback, damp)
                for (allpass in allpasses[0]) reverb = allpass.process(reverb)
                samples[base] = toPcm(dry * input + wet * reverb)
            } else {
                val left = samples[base] / PCM_SCALE
                val right = samples[base + 1] / PCM_SCALE
                val mono = (left + right) * INPUT_GAIN
                var reverbLeft = 0f
                var reverbRight = 0f
                for (comb in combs[0]) reverbLeft += comb.process(mono, feedback, damp)
                for (comb in combs[1]) reverbRight += comb.process(mono, feedback, damp)
                for (allpass in allpasses[0]) reverbLeft = allpass.process(reverbLeft)
                for (allpass in allpasses[1]) reverbRight = allpass.process(reverbRight)
                samples[base] = toPcm(dry * left + wet1 * reverbLeft + wet2 * reverbRight)
                samples[base + 1] = toPcm(dry * right + wet1 * reverbRight + wet2 * reverbLeft)
            }
        }
    }

    fun reset() {
        for (bank in combs) for (comb in bank) comb.reset()
        for (bank in allpasses) for (allpass in bank) allpass.reset()
        active = false
    }

    private fun delayOf(tuning: Int): Int = (tuning * scale).toInt().coerceAtLeast(1)

    private fun spread(channel: Int): Int = if (channel % 2 == 1) STEREO_SPREAD else 0

    private fun toPcm(value: Float): Short =
        (value.coerceIn(-1f, 1f) * PCM_SCALE).toInt().coerceIn(-32768, 32767).toShort()

    private class Comb(size: Int) {
        private val buffer = FloatArray(size)
        private var index = 0
        private var store = 0f

        fun process(input: Float, feedback: Float, damp: Float): Float {
            val output = buffer[index]
            store = output * (1f - damp) + store * damp
            buffer[index] = input + store * feedback
            index = (index + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            store = 0f
            index = 0
        }
    }

    private class Allpass(size: Int) {
        private val buffer = FloatArray(size)
        private var index = 0

        fun process(input: Float): Float {
            val buffered = buffer[index]
            val output = -input + buffered
            buffer[index] = input + buffered * ALLPASS_FEEDBACK
            index = (index + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            index = 0
        }
    }
}
