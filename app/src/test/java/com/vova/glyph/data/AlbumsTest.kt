package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumsTest {
    @Test
    fun `groups tracks by album and artist`() {
        val tracks = listOf(
            track(1, "Come Together", "The Beatles", "Abbey Road"),
            track(2, "Something", "The Beatles", "Abbey Road"),
            track(3, "Come Together", "Cover Band", "Abbey Road"),
        )

        val albums = Albums.group(tracks)

        assertEquals(2, albums.size)
        assertEquals(2, albums.first { it.artist == "The Beatles" }.trackCount)
        assertEquals(1, albums.first { it.artist == "Cover Band" }.trackCount)
    }

    @Test
    fun `same album name with different case is merged`() {
        val tracks = listOf(
            track(1, "One", "Artist", "Abbey Road"),
            track(2, "Two", "Artist", "abbey road"),
        )

        val albums = Albums.group(tracks)

        assertEquals(1, albums.size)
        assertEquals(2, albums.single().trackCount)
        assertEquals("Abbey Road", albums.single().name)
    }

    @Test
    fun `tracks without album metadata form a single unknown album`() {
        val tracks = listOf(
            track(1, "One", "Artist A", ""),
            track(2, "Two", "Artist B", "   "),
        )

        val albums = Albums.group(tracks)

        val unknown = albums.single()
        assertTrue(unknown.unknown)
        assertEquals(Albums.UNKNOWN_KEY, unknown.key)
        assertEquals(Albums.UNKNOWN_NAME, unknown.name)
        assertEquals(2, unknown.trackCount)
    }

    @Test
    fun `unknown album goes last and the rest are sorted by name`() {
        val tracks = listOf(
            track(1, "One", "Artist", ""),
            track(2, "Two", "Artist", "Zebra"),
            track(3, "Three", "Artist", "Apple"),
        )

        val albums = Albums.group(tracks)

        assertEquals(listOf("Apple", "Zebra", Albums.UNKNOWN_NAME), albums.map { it.name })
    }

    @Test
    fun `album summary sums duration and takes the first artwork`() {
        val tracks = listOf(
            track(1, "One", "Artist", "Album", durationMs = 1_000, artwork = null),
            track(2, "Two", "Artist", "Album", durationMs = 2_500, artwork = "/art/net_genius_a.jpg"),
        )

        val album = Albums.group(tracks).single()

        assertEquals(3_500L, album.durationMs)
        assertEquals("/art/net_genius_a.jpg", album.artwork)
    }

    @Test
    fun `album without artwork keeps null cover`() {
        val album = Albums.group(listOf(track(1, "One", "Artist", "Album"))).single()

        assertNull(album.artwork)
        assertFalse(album.unknown)
    }

    @Test
    fun `tracksOf selects only tracks of the requested album`() {
        val tracks = listOf(
            track(1, "One", "Artist", "Abbey Road"),
            track(2, "Two", "Artist", "Let It Be"),
            track(3, "Three", "Artist", "Abbey Road"),
        )

        val key = Albums.keyOf(tracks.first())
        val selected = Albums.tracksOf(tracks, key)

        assertEquals(listOf(1L, 3L), selected.map { it.id })
    }

    @Test
    fun `unknown artist is shown for album with blank artist`() {
        val album = Albums.group(listOf(track(1, "One", "   ", "Album"))).single()

        assertEquals(Albums.UNKNOWN_ARTIST, album.artist)
    }

    private fun track(
        id: Long,
        title: String,
        artist: String,
        album: String,
        durationMs: Long = 0,
        artwork: String? = null,
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = "content://track/$id",
        artwork = artwork,
    )
}
