package com.vova.glyph.data

data class Track(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0,
    val uri: String,
    val artwork: String? = null,
    val favorite: Boolean = false,
    val source: String = SOURCE_LOCAL,
    val sourceId: String? = null,
    val addedAt: Long = 0,
    val fileSize: Long = 0,
) {
    val displayArtist: String get() = artist.trim().ifBlank { "НЕИЗВЕСТНЫЙ ИСПОЛНИТЕЛЬ" }

    companion object {
        const val SOURCE_LOCAL = "LOCAL"
    }
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val cover: String? = null,
    val createdAt: Long = 0,
    val trackCount: Int = 0,
)
