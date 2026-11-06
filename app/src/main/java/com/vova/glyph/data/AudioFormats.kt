package com.vova.glyph.data

internal object AudioFormats {
    private val EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "ogg", "opus", "aac")

    fun isAudio(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in EXTENSIONS
}
