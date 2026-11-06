package com.vova.glyph.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatsTest {
    @Test
    fun `accepts supported audio extensions in any case`() {
        assertTrue(AudioFormats.isAudio("track.mp3"))
        assertTrue(AudioFormats.isAudio("TRACK.MP3"))
        assertTrue(AudioFormats.isAudio("01 - Song.FlAc"))
        assertTrue(AudioFormats.isAudio("voice.m4a"))
        assertTrue(AudioFormats.isAudio("ambient.opus"))
    }

    @Test
    fun `rejects files without supported extension`() {
        assertFalse(AudioFormats.isAudio("cover.jpg"))
        assertFalse(AudioFormats.isAudio("playlist.m3u"))
        assertFalse(AudioFormats.isAudio("no-extension"))
        assertFalse(AudioFormats.isAudio(""))
        assertFalse(AudioFormats.isAudio("mp3"))
    }
}
