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

    private val ARTIST_TAIL = Regex(
        "(?i)\\s*[,/].*$|[\\s(\\[{]*\\b(?:feat\\.?|ft\\.?|featuring|with|vs\\.?)\\b.*$",
    )

    private val TAIL_CHARS = charArrayOf(
        ' ', '(', '[', '{', '-', '\u2013', '\u2014', ',', ';', '&', '+', '/', '|', ':', '.', '\'', '"',
    )

    fun groupingArtist(artist: String): String {
        val trimmed = artist.trim()
        if (trimmed.isEmpty()) return ""
        val cut = ARTIST_TAIL.find(trimmed)?.range?.first ?: trimmed.length
        val head = trimmed.substring(0, cut).trimEnd(*TAIL_CHARS)
        return head.ifBlank { trimmed }.lowercase()
    }

    fun keyOf(track: Track): String {
        val album = track.album.trim()
        if (album.isBlank()) return UNKNOWN_KEY
        return album.lowercase() + SEPARATOR + groupingArtist(track.effectiveArtist)
    }

    fun normalizeKey(key: String): String {
        val index = key.indexOf(SEPARATOR)
        if (index < 0) return key
        return key.substring(0, index + 1) + groupingArtist(key.substring(index + 1))
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
                artist = if (unknown) "" else dominantArtist(items).ifBlank { UNKNOWN_ARTIST },
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

    private fun dominantArtist(items: List<Track>): String {
        val keys = items.map { groupingArtist(it.effectiveArtist) }
        val winner = keys.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value })
            .firstOrNull()
            ?.key
            ?: return ""
        val variants = items
            .filter { groupingArtist(it.effectiveArtist) == winner }
            .map { it.effectiveArtist.trim() }
        return variants.groupingBy { it }.eachCount().entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key.length },
            )
            .firstOrNull()
            ?.key
            .orEmpty()
    }

    fun tracksOf(tracks: List<Track>, key: String): List<Track> = tracks.filter { keyOf(it) == key }
}
