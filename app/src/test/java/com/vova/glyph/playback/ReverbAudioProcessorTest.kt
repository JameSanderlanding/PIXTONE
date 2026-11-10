package com.vova.glyph.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.vova.glyph.playback.dsp.ReverbAudioProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReverbAudioProcessorTest {
    @Test
    fun disabledReverbCopiesTheBuffer() {
        val processor = ReverbAudioProcessor()
        val format = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        assertEquals(format, processor.configure(format))

        val samples = ShortArray(1_024) { index -> (index * 29 % 24_000 - 12_000).toShort() }
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        input.asShortBuffer().put(samples)
        input.position(0)

        processor.queueInput(input)
        val output = processor.getOutput()
        val result = ShortArray(samples.size)
        output.asShortBuffer().get(result)

        assertArrayEquals(samples, result)
    }
}
