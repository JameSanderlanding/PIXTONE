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

    @Test
    fun `custom artist is used for album grouping and title`() {
        val tagTrack = track(1, "One", "Tag Artist", "Album")
        val customTrack = tagTrack.copy(customArtist = "Custom Artist")

        val albums = Albums.group(
            listOf(
                customTrack,
                track(2, "Two", "Tag Artist", "Album"),
            )
        )

        assertEquals(2, albums.size)
        assertEquals(1, albums.count { it.artist == "Custom Artist" })
        assertEquals(1, albums.count { it.artist == "Tag Artist" })
        assertTrue(Albums.keyOf(customTrack) != Albums.keyOf(track(3, "One", "Tag Artist", "Album")))
    }

    @Test
    fun `featured artists do not split an album`() {
        val tracks = listOf(
            track(1, "One", "Drake feat. Rihanna", "Testament"),
            track(2, "Two", "Drake", "Testament"),
            track(3, "Three", "Drake ft. Future", "Testament"),
            track(4, "Four", "Drake featuring Beyonce", "Testament"),
            track(5, "Five", "Drake with Rihanna", "Testament"),
            track(6, "Six", "Drake (feat. Rihanna)", "Testament"),
        )

        val albums = Albums.group(tracks)

        assertEquals(1, albums.size)
        assertEquals(6, albums.single().trackCount)
    }

    @Test
    fun `comma separated artists do not split an album`() {
        val tracks = listOf(
            track(1, "One", "Tyler, The Creator", "Chromakopia"),
            track(2, "Two", "Tyler, The Creator, Kali Uchis", "Chromakopia"),
        )

        val albums = Albums.group(tracks)

        assertEquals(1, albums.size)
        assertEquals(2, albums.single().trackCount)
        assertEquals("Tyler, The Creator", albums.single().artist)
    }

    @Test
    fun `slash separated artists do not split an album`() {
        val tracks = listOf(
            track(1, "One", "Tyler, The Creator", "Chromakopia"),
            track(2, "Two", "Tyler, The Creator, Kali Uchis", "Chromakopia"),
            track(3, "Three", "Tyler, The Creator / Kali Uchis", "Chromakopia"),
            track(4, "Four", "Tyler, The Creator/Kali Uchis", "Chromakopia"),
        )

        val albums = Albums.group(tracks)

        assertEquals(1, albums.size)
        assertEquals(4, albums.single().trackCount)
        assertEquals("Tyler, The Creator", albums.single().artist)
        assertEquals("drake", Albums.groupingArtist("Drake / Rihanna"))
        assertEquals("drake", Albums.groupingArtist("Drake/Rihanna"))
    }

    @Test
    fun `artist names without features are kept whole`() {
        assertEquals("within temptation", Albums.groupingArtist("Within Temptation"))
        assertEquals("the beatles", Albums.groupingArtist("The Beatles"))
        assertEquals("acdc", Albums.groupingArtist("ACDC"))
        assertEquals("", Albums.groupingArtist("   "))
    }

    @Test
    fun `different artists still form different albums`() {
        val tracks = listOf(
            track(1, "One", "Drake feat. Rihanna", "Greatest Hits"),
            track(2, "Two", "Drake", "Greatest Hits"),
            track(3, "Three", "Cover Band", "Greatest Hits"),
        )

        val albums = Albums.group(tracks)

        assertEquals(2, albums.size)
        assertEquals(2, albums.first { it.artist == "Drake" }.trackCount)
        assertEquals(1, albums.first { it.artist == "Cover Band" }.trackCount)
    }

    @Test
    fun `album artist is the most frequent artist in the album`() {
        val tracks = listOf(
            track(1, "Panic Attack", "saluki, темный принц", "Album"),
            track(2, "Euphoria", "saluki", "Album"),
            track(3, "Au Revoir", "saluki", "Album"),
        )

        val album = Albums.group(tracks).single()

        assertEquals("saluki", album.artist)
        assertEquals(3, album.trackCount)
    }

    @Test
    fun `album artist tie prefers the shortest variant`() {
        val tracks = listOf(
            track(1, "One", "Drake feat. Rihanna", "Album"),
            track(2, "Two", "Drake", "Album"),
        )

        assertEquals("Drake", Albums.group(tracks).single().artist)
    }

    @Test
    fun `album artist keeps the tag case of the most frequent variant`() {
        val tracks = listOf(
            track(1, "One", "Saluki", "Album"),
            track(2, "Two", "SALUKI", "Album"),
            track(3, "Three", "Saluki", "Album"),
        )

        assertEquals("Saluki", Albums.group(tracks).single().artist)
    }

    @Test
    fun `legacy hidden album keys are normalized`() {
        val legacy = "testament\u0000drake feat. rihanna"

        assertEquals("testament\u0000drake", Albums.normalizeKey(legacy))
        assertEquals(Albums.UNKNOWN_KEY, Albums.normalizeKey(Albums.UNKNOWN_KEY))
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
