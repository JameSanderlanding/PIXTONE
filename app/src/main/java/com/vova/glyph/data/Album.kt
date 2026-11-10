package com.vova.glyph.data

data class Album(
    val key: String,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val durationMs: Long,
    val artwork: String?,
    val unknown: Boolean = false,
)

object Albums {
    const val UNKNOWN_KEY = "\u0000no-album"

    const val UNKNOWN_NAME = "Без альбома"

    const val UNKNOWN_ARTIST = "Неизвестный исполнитель"

    private const val SEPARATOR = "\u0000"

    fun keyOf(track: Track): String {
        val album = track.album.trim()
        if (album.isBlank()) return UNKNOWN_KEY
        return album.lowercase() + SEPARATOR + track.effectiveArtist.lowercase()
    }

    fun group(tracks: List<Track>): List<Album> {
        val groups = LinkedHashMap<String, MutableList<Track>>()
        for (track in tracks) {
            groups.getOrPut(keyOf(track)) { mutableListOf() } += track
        }
        return groups.map { (key, items) ->
            val first = items.first()
            val unknown = key == UNKNOWN_KEY
            Album(
                key = key,
                name = if (unknown) UNKNOWN_NAME else first.album.trim(),
                artist = if (unknown) "" else first.effectiveArtist.ifBlank { UNKNOWN_ARTIST },
                trackCount = items.size,
                durationMs = items.sumOf { it.durationMs },
                artwork = items.firstNotNullOfOrNull { it.artwork?.takeIf(String::isNotBlank) },
                unknown = unknown,
            )
        }.sortedWith(
            compareBy<Album> { it.unknown }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.artist },
        )
    }

    fun tracksOf(tracks: List<Track>, key: String): List<Track> = tracks.filter { keyOf(it) == key }
}
