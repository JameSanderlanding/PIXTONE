package com.vova.glyph.data

object TrackSearch {
    fun filter(tracks: List<Track>, query: String): List<Track> {
        val q = query.trim()
        if (q.isBlank()) return tracks
        return tracks.filter { matches(it, q) }
    }

    fun matches(track: Track, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return track.title.contains(q, ignoreCase = true) ||
            track.effectiveTitle.contains(q, ignoreCase = true) ||
            track.artist.contains(q, ignoreCase = true) ||
            track.effectiveArtist.contains(q, ignoreCase = true) ||
            track.album.contains(q, ignoreCase = true)
    }
}
