package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSorterTest {
    private val old = track(1, "Old Song", addedAt = 300, fileModifiedAt = 900)
    private val fresh = track(2, "fresh song", addedAt = 900, fileModifiedAt = 300)
    private val middle = track(3, "Middle Song", addedAt = 600, fileModifiedAt = 600)

    private val tracks = listOf(fresh, middle, old)

    @Test
    fun `sorts by title ignoring case`() {
        val sorted = TrackSorter.apply(tracks, TrackSorting())

        assertEquals(listOf("fresh song", "Middle Song", "Old Song"), sorted.map { it.title })
    }

    @Test
    fun `sorts by the manually assigned title`() {
        val custom = listOf(
            track(1, "Zebra").copy(customTitle = "Aardvark"),
            track(2, "Apple"),
        )

        val sorted = TrackSorter.apply(custom, TrackSorting())

        assertEquals(listOf(1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `sorts by added date`() {
        val sorted = TrackSorter.apply(tracks, TrackSorting(by = TrackSort.ADDED))

        assertEquals(listOf(1L, 3L, 2L), sorted.map { it.id })
    }

    @Test
    fun `sorts by file modification date`() {
        val sorted = TrackSorter.apply(tracks, TrackSorting(by = TrackSort.MODIFIED))

        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `reverses any order`() {
        val byTitle = TrackSorter.apply(tracks, TrackSorting(reverse = true))
        val byAddDate = TrackSorter.apply(
            tracks,
            TrackSorting(by = TrackSort.ADDED, reverse = true),
        )

        assertEquals(listOf("Old Song", "Middle Song", "fresh song"), byTitle.map { it.title })
        assertEquals(listOf(2L, 3L, 1L), byAddDate.map { it.id })
    }

    @Test
    fun `sorts by track number and keeps unknown numbers last`() {
        val numbered = listOf(
            track(1, "Third", addedAt = 0, fileModifiedAt = 0, trackNumber = 3),
            track(2, "Unknown", addedAt = 0, fileModifiedAt = 0, trackNumber = 0),
            track(3, "First", addedAt = 0, fileModifiedAt = 0, trackNumber = 1),
        )

        val sorted = TrackSorter.apply(numbered, TrackSorting(by = TrackSort.TRACK_NO))

        assertEquals(listOf("First", "Third", "Unknown"), sorted.map { it.title })
    }

    @Test
    fun `keeps playlist order unless reversed`() {
        val playlistOrder = listOf(middle, old, fresh)

        val asIs = TrackSorter.apply(playlistOrder, TrackSorting(by = TrackSort.PLAYLIST))
        val reversed = TrackSorter.apply(
            playlistOrder,
            TrackSorting(by = TrackSort.PLAYLIST, reverse = true),
        )

        assertEquals(listOf(3L, 1L, 2L), asIs.map { it.id })
        assertEquals(listOf(2L, 1L, 3L), reversed.map { it.id })
    }

    @Test
    fun `unknown file dates go last`() {
        val tracks = listOf(
            track(1, "No date", addedAt = 0, fileModifiedAt = 0),
            track(2, "New", addedAt = 0, fileModifiedAt = 900),
            track(3, "Old", addedAt = 0, fileModifiedAt = 300),
        )

        val sorted = TrackSorter.apply(tracks, TrackSorting(by = TrackSort.MODIFIED))

        assertEquals(listOf("Old", "New", "No date"), sorted.map { it.title })
    }

    @Test
    fun `encoding round trips through prefs value`() {
        val sorting = TrackSorting(by = TrackSort.MODIFIED, reverse = true)

        val decoded = TrackSorting.decode(sorting.encode(), TrackSorting())

        assertEquals(sorting, decoded)
    }

    @Test
    fun `decoding falls back on blank or unknown values`() {
        val fallback = TrackSorting(by = TrackSort.PLAYLIST)

        assertEquals(fallback, TrackSorting.decode(null, fallback))
        assertEquals(fallback, TrackSorting.decode("", fallback))
        assertEquals(fallback, TrackSorting.decode("BROKEN", fallback))
        assertEquals(
            TrackSorting(by = TrackSort.ADDED),
            TrackSorting.decode("ADDED", fallback),
        )
    }

    private fun track(
        id: Long,
        title: String,
        addedAt: Long = 0,
        fileModifiedAt: Long = 0,
        trackNumber: Int = 0,
    ) = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        uri = "content://track/$id",
        addedAt = addedAt,
        fileModifiedAt = fileModifiedAt,
        trackNumber = trackNumber,
    )
}
