package com.vova.glyph.data

object TrackSorter {
    fun apply(
        tracks: List<Track>,
        sorting: TrackSorting,
    ): List<Track> {
        if (sorting.by == TrackSort.PLAYLIST) {
            return if (sorting.reverse) tracks.reversed() else tracks
        }
        val comparator = when (sorting.by) {
            TrackSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.effectiveTitle }
            TrackSort.ADDED -> compareBy<Track> { it.addedAt }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.effectiveTitle }
            TrackSort.MODIFIED -> compareBy<Track> { if (it.fileModifiedAt > 0) 0 else 1 }
                .thenBy { it.fileModifiedAt }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.effectiveTitle }
            TrackSort.TRACK_NO -> compareBy<Track> { if (it.trackNumber > 0) 0 else 1 }
                .thenBy { it.trackNumber }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.effectiveTitle }
            TrackSort.PLAYLIST -> compareBy { 0 }
        }
        val knownFirst = when (sorting.by) {
            TrackSort.MODIFIED -> compareBy<Track> { if (it.fileModifiedAt > 0) 0 else 1 }
            TrackSort.TRACK_NO -> compareBy<Track> { if (it.trackNumber > 0) 0 else 1 }
            else -> null
        }
        if (!sorting.reverse) return tracks.sortedWith(comparator)
        val reversed = comparator.reversed()
        return if (knownFirst == null) {
            tracks.sortedWith(reversed)
        } else {
            tracks.sortedWith(knownFirst.then(reversed))
        }
    }
}
