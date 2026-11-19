package com.vova.glyph.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.vova.glyph.data.Track
import com.vova.glyph.data.TrackSearch
import com.vova.glyph.data.TrackSorter
import com.vova.glyph.data.TrackSorting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberSortedTracks(
    tracks: List<Track>,
    query: String,
    sorting: TrackSorting,
): List<Track> {
    val initial = remember { TrackSorter.apply(TrackSearch.filter(tracks, query), sorting) }
    val sorted by produceState(initial, tracks, query, sorting) {
        value = withContext(Dispatchers.Default) {
            TrackSorter.apply(TrackSearch.filter(tracks, query), sorting)
        }
    }
    return sorted
}

@Composable
fun rememberFilteredTracks(
    tracks: List<Track>,
    query: String,
): List<Track> {
    val initial = remember { TrackSearch.filter(tracks, query) }
    val filtered by produceState(initial, tracks, query) {
        value = withContext(Dispatchers.Default) { TrackSearch.filter(tracks, query) }
    }
    return filtered
}
