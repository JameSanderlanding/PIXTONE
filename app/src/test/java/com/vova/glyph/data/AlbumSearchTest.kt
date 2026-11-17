package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumSearchTest {
    private val albums = listOf(
        Album(
            key = "ok computer\u0000radiohead",
            name = "OK Computer",
            artist = "Radiohead",
            trackCount = 12,
            durationMs = 0,
            artwork = null,
        ),
        Album(
            key = "kid a\u0000radiohead",
            name = "Kid A",
            artist = "Radiohead",
            trackCount = 10,
            durationMs = 0,
            artwork = null,
        ),
        Album(
            key = "the bends\u0000radiohead feat. someone",
            name = "The Bends",
            artist = "Radiohead feat. Someone",
            trackCount = 12,
            durationMs = 0,
            artwork = null,
        ),
        Album(
            key = Albums.UNKNOWN_KEY,
            name = Albums.UNKNOWN_NAME,
            artist = "",
            trackCount = 3,
            durationMs = 0,
            artwork = null,
            unknown = true,
        ),
    )

    @Test
    fun `blank query keeps every album`() {
        assertEquals(albums, AlbumSearch.filter(albums, ""))
        assertEquals(albums, AlbumSearch.filter(albums, "   "))
    }

    @Test
    fun `finds album by name ignoring case`() {
        assertEquals(listOf("Kid A"), AlbumSearch.filter(albums, "kid").map { it.name })
    }

    @Test
    fun `finds albums by artist`() {
        assertEquals(1, AlbumSearch.filter(albums, "someone").size)
        assertEquals(3, AlbumSearch.filter(albums, "radiohead").size)
    }

    @Test
    fun `query is trimmed`() {
        assertEquals(listOf("The Bends"), AlbumSearch.filter(albums, "  bends  ").map { it.name })
    }

    @Test
    fun `unknown album is searchable by its name`() {
        val found = AlbumSearch.filter(albums, "без альбома")

        assertTrue(found.single().unknown)
    }

    @Test
    fun `missing text gives nothing`() {
        assertTrue(AlbumSearch.filter(albums, "nirvana").isEmpty())
    }

    @Test
    fun `matches ignores blank query`() {
        assertTrue(AlbumSearch.matches(albums.first(), ""))
        assertFalse(AlbumSearch.matches(albums.first(), "nirvana"))
    }
}
