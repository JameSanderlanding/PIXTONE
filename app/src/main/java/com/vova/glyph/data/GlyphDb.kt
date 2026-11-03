package com.vova.glyph.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext




class GlyphDb(context: Context) : SQLiteOpenHelper(context, "glyph.db", null, 10) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tracks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                artist TEXT NOT NULL DEFAULT '',
                album TEXT NOT NULL DEFAULT '',
                genre TEXT,
                genre_checked INTEGER NOT NULL DEFAULT 0,
                duration INTEGER NOT NULL DEFAULT 0,
                uri TEXT NOT NULL UNIQUE,
                artwork TEXT,
                favorite INTEGER NOT NULL DEFAULT 0,
                source TEXT NOT NULL DEFAULT 'LOCAL',
                source_id TEXT,
                added_at INTEGER NOT NULL DEFAULT 0,
                file_size INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tracks_favorite ON tracks(favorite)")
        db.execSQL("CREATE UNIQUE INDEX idx_tracks_source ON tracks(source, source_id)")

        db.execSQL(
            """
            CREATE TABLE playlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                cover TEXT,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlist_tracks (
                playlist_id INTEGER NOT NULL,
                track_id INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (playlist_id, track_id)
            )
            """.trimIndent()
        )

        createLyricsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createLyricsTable(db)
        if (oldVersion < 3) {
            
            
            db.execSQL("UPDATE tracks SET artwork = NULL WHERE artwork LIKE '%net_%'")
        }
        if (oldVersion in 2..3) {
            
            
            db.execSQL("ALTER TABLE lyrics ADD COLUMN found INTEGER NOT NULL DEFAULT 1")
        }
        if (oldVersion < 5) {
            
            
            db.execSQL("ALTER TABLE tracks ADD COLUMN genre TEXT")
        }
        if (oldVersion < 6) {
            
            db.execSQL("ALTER TABLE tracks ADD COLUMN genre_checked INTEGER NOT NULL DEFAULT 0")
            
            db.execSQL(
                "UPDATE tracks SET genre_checked = 1 WHERE genre IS NOT NULL AND TRIM(genre) != ''"
            )
        }
        if (oldVersion < 7) {
            
            
            db.execSQL(
                "UPDATE tracks SET genre = NULL, genre_checked = 0 WHERE source = 'LOCAL'"
            )
        }
        if (oldVersion < 8) {
            
            
            
            db.execSQL(
                "UPDATE tracks SET genre_checked = 0 " +
                    "WHERE source = 'LOCAL' AND (genre IS NULL OR TRIM(genre) = '')"
            )
        }
        if (oldVersion < 9) {
            
            
            db.execSQL(
                "UPDATE tracks SET genre_checked = 0 " +
                    "WHERE source = 'LOCAL' AND (genre IS NULL OR TRIM(genre) = '')"
            )
        }
    }

    



    private fun createLyricsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lyrics (
                track_id INTEGER PRIMARY KEY,
                plain TEXT,
                synced TEXT,
                found INTEGER NOT NULL DEFAULT 1,
                fetched_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    suspend fun allTracks(): List<Track> = withContext(Dispatchers.IO) {
        readableDatabase.query(
            "tracks", null, null, null, null, null, "title COLLATE NOCASE ASC"
        ).use { it.toTracks() }
    }

    suspend fun favoriteTracks(): List<Track> = withContext(Dispatchers.IO) {
        readableDatabase.query(
            "tracks", null, "favorite = 1", null, null, null, "title COLLATE NOCASE ASC"
        ).use { it.toTracks() }
    }

    suspend fun trackById(id: Long): Track? = withContext(Dispatchers.IO) {
        readableDatabase.query(
            "tracks", null, "id = ?", arrayOf(id.toString()), null, null, null
        ).use { if (it.moveToFirst()) it.toTrack() else null }
    }

    suspend fun upsertLocal(track: Track): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.query(
            "tracks", arrayOf("id"), "uri = ?", arrayOf(track.uri), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val values = ContentValues().apply {
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("duration", track.durationMs)
                    put("file_size", track.fileSize)
                    track.artwork?.let { put("artwork", it) }
                }
                db.update("tracks", values, "id = ?", arrayOf(id.toString()))
                return@withContext id
            }
        }
        val values = ContentValues().apply {
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("duration", track.durationMs)
            put("uri", track.uri)
            track.artwork?.let { put("artwork", it) }
            put("favorite", 0)
            put("source", Track.SOURCE_LOCAL)
            put("added_at", System.currentTimeMillis())
            put("file_size", track.fileSize)
        }
        db.insertOrThrow("tracks", null, values)
    }

    suspend fun toggleFavorite(trackId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        var newValue = true
        db.query(
            "tracks", arrayOf("favorite"), "id = ?", arrayOf(trackId.toString()), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                newValue = cursor.getInt(0) == 0
                val values = ContentValues().apply { put("favorite", if (newValue) 1 else 0) }
                db.update("tracks", values, "id = ?", arrayOf(trackId.toString()))
            }
        }
        newValue
    }

    suspend fun setFavorite(trackId: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("favorite", if (favorite) 1 else 0) }
        writableDatabase.update("tracks", values, "id = ?", arrayOf(trackId.toString()))
    }

     
    suspend fun updateArtwork(trackId: Long, artwork: String?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            if (artwork == null) putNull("artwork") else put("artwork", artwork)
        }
        writableDatabase.update("tracks", values, "id = ?", arrayOf(trackId.toString()))
    }

     
    suspend fun lyricsFor(trackId: Long): CachedLyrics? = withContext(Dispatchers.IO) {
        readableDatabase.query(
            "lyrics", arrayOf("plain", "synced", "found", "fetched_at"),
            "track_id = ?", arrayOf(trackId.toString()), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val plain = if (cursor.isNull(0)) null else cursor.getString(0)
            val syncedRaw = if (cursor.isNull(1)) "" else cursor.getString(1)
            CachedLyrics(
                plain = plain,
                synced = parseLrc(syncedRaw),
                found = cursor.getInt(2) != 0,
                fetchedAt = cursor.getLong(3),
            )
        }
    }

     
    suspend fun saveLyrics(trackId: Long, result: LyricsResult) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("track_id", trackId)
            if (result.plain == null) putNull("plain") else put("plain", result.plain)
            put("synced", result.synced.toLrc())
            put("found", 1)
            put("fetched_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "lyrics", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    



    suspend fun saveLyricsMissing(trackId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("track_id", trackId)
            putNull("plain")
            put("synced", "")
            put("found", 0)
            put("fetched_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "lyrics", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun playlists(): List<Playlist> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            """
            SELECT p.*, (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlist_id = p.id) AS track_count
            FROM playlists p
            ORDER BY p.created_at DESC
            """.trimIndent(), null
        ).use { cursor ->
            val out = mutableListOf<Playlist>()
            while (cursor.moveToNext()) {
                out += Playlist(
                    id = cursor.long("id"),
                    name = cursor.string("name"),
                    description = cursor.string("description"),
                    cover = cursor.nullableString("cover"),
                    createdAt = cursor.long("created_at"),
                    trackCount = cursor.int("track_count"),
                )
            }
            out
        }
    }

    suspend fun createPlaylist(name: String, description: String, cover: String?): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("name", name)
                put("description", description)
                put("cover", cover)
                put("created_at", System.currentTimeMillis())
            }
            writableDatabase.insertOrThrow("playlists", null, values)
        }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("playlist_tracks", "playlist_id = ?", arrayOf(id.toString()))
        db.delete("playlists", "id = ?", arrayOf(id.toString()))
    }

    suspend fun playlistTracks(playlistId: Long): List<Track> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            """
            SELECT t.* FROM tracks t
            INNER JOIN playlist_tracks pt ON pt.track_id = t.id
            WHERE pt.playlist_id = ?
            ORDER BY pt.position ASC, pt.rowid ASC
            """.trimIndent(), arrayOf(playlistId.toString())
        ).use { it.toTracks() }
    }

    suspend fun addToPlaylist(playlistId: Long, trackId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.query(
            "playlist_tracks", arrayOf("track_id"),
            "playlist_id = ? AND track_id = ?", arrayOf(playlistId.toString(), trackId.toString()),
            null, null, null
        ).use { if (it.moveToFirst()) return@withContext false }
        val position = db.rawQuery(
            "SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlist_id = ?",
            arrayOf(playlistId.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val values = ContentValues().apply {
            put("playlist_id", playlistId)
            put("track_id", trackId)
            put("position", position)
        }
        db.insertOrThrow("playlist_tracks", null, values)
        true
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            "playlist_tracks", "playlist_id = ? AND track_id = ?",
            arrayOf(playlistId.toString(), trackId.toString())
        )
    }

    private fun Cursor.toTracks(): List<Track> {
        val out = mutableListOf<Track>()
        while (moveToNext()) out += toTrack()
        return out
    }

    private fun Cursor.toTrack(): Track = Track(
        id = long("id"),
        title = string("title"),
        artist = string("artist"),
        album = string("album"),
        durationMs = long("duration"),
        uri = string("uri"),
        artwork = nullableString("artwork"),
        favorite = int("favorite") == 1,
        source = string("source"),
        sourceId = nullableString("source_id"),
        addedAt = long("added_at"),
        fileSize = long("file_size"),
    )

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.string(column: String): String =
        getString(getColumnIndexOrThrow(column)).orEmpty()

    private fun Cursor.nullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }
}
