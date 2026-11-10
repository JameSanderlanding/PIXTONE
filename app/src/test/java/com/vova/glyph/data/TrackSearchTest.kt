package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSearchTest {
    private val tracks = listOf(
        Track(id = 1, title = "Come Together", artist = "The Beatles", album = "Abbey Road", uri = "content://1"),
        Track(id = 2, title = "Something", artist = "The Beatles", album = "Abbey Road", uri = "content://2"),
        Track(
            id = 3,
            title = "Heroes",
            artist = "",
            customArtist = "David Bowie",
            album = "Heroes",
            uri = "content://3",
        ),
        Track(
            id = 4,
            title = "Track without tags",
            artist = "",
            customArtist = "  ",
            album = "",
            uri = "content://4",
        ),
    )

    @Test
    fun `blank query keeps the whole library`() {
        assertEquals(tracks, TrackSearch.filter(tracks, "   "))
    }

    @Test
    fun `search matches track title`() {
        assertEquals(listOf(1L), TrackSearch.filter(tracks, "come").map { it.id })
    }

    @Test
    fun `search matches the artist tag`() {
        assertEquals(listOf(1L, 2L), TrackSearch.filter(tracks, "beatles").map { it.id })
    }

    @Test
    fun `search matches a manually assigned artist`() {
        assertEquals(listOf(3L), TrackSearch.filter(tracks, "bowie").map { it.id })
    }

    @Test
    fun `search matches the album`() {
        assertEquals(listOf(1L, 2L), TrackSearch.filter(tracks, "abbey").map { it.id })
    }

    @Test
    fun `custom artist overrides the tag artist`() {
        val track = Track(
            id = 5,
            title = "Heroes",
            artist = "Wrong Artist",
            customArtist = "David Bowie",
            uri = "content://5",
        )

        assertEquals("David Bowie", track.effectiveArtist)
        assertEquals("David Bowie", track.displayArtist)
        assertTrue(track.hasCustomArtist)
    }

    @Test
    fun `blank custom artist falls back to the tag artist`() {
        val track = Track(
            id = 6,
            title = "Heroes",
            artist = "David Bowie",
            customArtist = "   ",
            uri = "content://6",
        )

        assertEquals("David Bowie", track.effectiveArtist)
        assertFalse(track.hasCustomArtist)
    }

    @Test
    fun `missing artist is shown as unknown`() {
        val track = Track(id = 7, title = "Untitled", artist = "", uri = "content://7")

        assertEquals("НЕИЗВЕСТНЫЙ ИСПОЛНИТЕЛЬ", track.displayArtist)
    }
}
