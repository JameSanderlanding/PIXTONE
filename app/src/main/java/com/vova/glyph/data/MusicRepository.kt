package com.vova.glyph.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicRepository(private val db: GlyphDb) {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _onlineHistory = MutableStateFlow<List<Track>>(emptyList())
    val onlineHistory: StateFlow<List<Track>> = _onlineHistory.asStateFlow()

    suspend fun refresh() {
        _tracks.value = db.allTracks()
        _favorites.value = db.favoriteTracks()
        _playlists.value = db.playlists()
        _onlineHistory.value = db.onlineHistory(ONLINE_HISTORY_LIMIT)
    }

    suspend fun addLocalTracks(tracks: List<Track>): Int {
        var added = 0
        for (track in tracks) {
            runCatching {
                if (db.upsertLocal(track)) added++
            }
        }
        refresh()
        return added
    }

    suspend fun saveOnlineTracks(tracks: List<Track>): List<Track> {
        if (tracks.isEmpty()) return emptyList()
        val ids = tracks.mapNotNull { track ->
            runCatching { db.upsertOnline(track) }.getOrNull()
        }
        refresh()
        return ids.mapNotNull { db.trackById(it) }
    }

    suspend fun pruneOnlineTracks(keepId: Long?): Int {
        val removed = runCatching { db.pruneOnlineTracks(keepId) }.getOrDefault(emptyList())
        if (removed.isNotEmpty()) refresh()
        return removed.size
    }

    suspend fun markOnlinePlayed(trackId: Long) {
        if (!runCatching { db.markOnlinePlayed(trackId) }.getOrDefault(false)) return
        runCatching { db.trimOnlineHistory(ONLINE_HISTORY_LIMIT) }
        _onlineHistory.value = db.onlineHistory(ONLINE_HISTORY_LIMIT)
    }

    suspend fun removeOnlineFromHistory(trackId: Long) {
        db.removeOnlineHistory(trackId)
        refresh()
    }

    suspend fun setArtwork(trackId: Long, artwork: String?) {
        db.updateArtwork(trackId, artwork)
    }

    suspend fun setTrackInfo(trackId: Long, title: String?, artist: String?) {
        db.setCustomTrackInfo(trackId, title, artist)
        refresh()
    }

    suspend fun cachedLyrics(trackId: Long): CachedLyrics? = db.lyricsFor(trackId)

    suspend fun saveLyrics(trackId: Long, result: LyricsResult) = db.saveLyrics(trackId, result)

    suspend fun markLyricsMissing(trackId: Long) = db.saveLyricsMissing(trackId)

    suspend fun lyricQuotes(trackId: Long): List<LyricQuote> = db.lyricQuotes(trackId)

    suspend fun addLyricQuote(trackId: Long, lines: List<LyricQuoteLine>): Boolean =
        db.addLyricQuote(trackId, lines)

    suspend fun deleteLyricQuote(trackId: Long, quoteId: Long) =
        db.deleteLyricQuote(trackId, quoteId)

    suspend fun deleteLyricHighlights(trackId: Long, lineIndices: List<Int>) =
        db.deleteLyricHighlights(trackId, lineIndices)

    suspend fun toggleFavorite(trackId: Long): Boolean {
        val nowFavorite = db.toggleFavorite(trackId)
        refresh()
        return nowFavorite
    }

    suspend fun deleteTracks(ids: Collection<Long>) {
        db.deleteTracks(ids.toList())
        refresh()
    }

    suspend fun createPlaylist(name: String, description: String, cover: String?): Long {
        val id = db.createPlaylist(name, description, cover)
        refresh()
        return id
    }

    suspend fun deletePlaylist(id: Long) {
        db.deletePlaylist(id)
        refresh()
    }

    suspend fun addToPlaylist(playlistId: Long, trackId: Long): Boolean {
        val added = db.addToPlaylist(playlistId, trackId)
        refresh()
        return added
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        db.removeFromPlaylist(playlistId, trackId)
        refresh()
    }

    suspend fun playlistTracks(playlistId: Long): List<Track> = db.playlistTracks(playlistId)

    suspend fun trackById(id: Long): Track? = db.trackById(id)

    private companion object {
        const val ONLINE_HISTORY_LIMIT = 100
    }
}
