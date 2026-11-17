package com.vova.glyph.data

object AlbumSearch {
    fun filter(albums: List<Album>, query: String): List<Album> {
        val q = query.trim()
        if (q.isBlank()) return albums
        return albums.filter { matches(it, q) }
    }

    fun matches(album: Album, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return album.name.contains(q, ignoreCase = true) ||
            album.artist.contains(q, ignoreCase = true)
    }
}
