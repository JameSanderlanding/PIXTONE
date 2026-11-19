package com.vova.glyph.data

data class Track(
    val id: Long = 0,
    val title: String,
    val customTitle: String? = null,
    val artist: String,
    val customArtist: String? = null,
    val album: String = "",
    val fileModifiedAt: Long = 0,
    val trackNumber: Int = 0,
    val durationMs: Long = 0,
    val uri: String,
    val artwork: String? = null,
    val favorite: Boolean = false,
    val source: String = SOURCE_LOCAL,
    val sourceId: String? = null,
    val addedAt: Long = 0,
    val fileSize: Long = 0,
) {
    val effectiveTitle: String
        get() = customTitle?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim()

    val hasCustomTitle: Boolean get() = !customTitle.isNullOrBlank()

    val effectiveArtist: String
        get() = customArtist?.trim().takeUnless { it.isNullOrBlank() } ?: artist.trim()

    val hasCustomArtist: Boolean get() = !customArtist.isNullOrBlank()

    val displayArtist: String get() = effectiveArtist.ifBlank { "НЕИЗВЕСТНЫЙ ИСПОЛНИТЕЛЬ" }

    val isOnline: Boolean get() = source != SOURCE_LOCAL

    companion object {
        const val SOURCE_LOCAL = "LOCAL"

        const val SOURCE_JIOSAAVN = "JIOSAAVN"
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

enum class TrackSort { TITLE, ADDED, MODIFIED, TRACK_NO, PLAYLIST }

data class TrackSorting(
    val by: TrackSort = TrackSort.TITLE,
    val reverse: Boolean = false,
) {
    fun encode(): String = "${by.name}$SEPARATOR${if (reverse) 1 else 0}"

    companion object {
        private const val SEPARATOR = ":"

        fun decode(raw: String?, fallback: TrackSorting): TrackSorting {
            if (raw.isNullOrBlank()) return fallback
            val parts = raw.split(SEPARATOR)
            val by = runCatching { TrackSort.valueOf(parts.first()) }.getOrNull() ?: return fallback
            return TrackSorting(by = by, reverse = parts.getOrNull(1) == "1")
        }
    }
}
