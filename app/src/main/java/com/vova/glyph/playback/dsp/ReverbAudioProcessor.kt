package com.vova.glyph.playback.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class ReverbAudioProcessor : BaseAudioProcessor() {
    val settings = ReverbSettings()

    private var freeverb: Freeverb? = null
    private var scratch = ShortArray(0)
    private var channels = 1

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        freeverb = if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            Freeverb(inputAudioFormat.sampleRate, channels, settings)
        } else {
            null
        }
        scratch = ShortArray(0)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size <= 0) return
        val output = replaceOutputBuffer(size)
        if (size % 2 != 0) {
            output.put(inputBuffer)
            output.position(0).limit(size)
            return
        }
        val sampleCount = size / 2
        val samples = scratch(sampleCount)
        inputBuffer.asShortBuffer().get(samples, 0, sampleCount)
        inputBuffer.position(inputBuffer.limit())
        freeverb?.process(samples, sampleCount / channels)
        output.position(0).limit(size)
        output.asShortBuffer().put(samples, 0, sampleCount)
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        freeverb?.reset()
    }

    override fun onReset() {
        freeverb = null
        scratch = ShortArray(0)
        channels = 1
    }

    private fun scratch(size: Int): ShortArray {
        if (scratch.size < size) scratch = ShortArray(size)
        return scratch
    }
}
