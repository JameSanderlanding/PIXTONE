package com.vova.glyph

import android.content.Context
import com.vova.glyph.data.ArtworkStore
import com.vova.glyph.data.CoverRepository
import com.vova.glyph.data.GeniusGate
import com.vova.glyph.data.GlyphDb
import com.vova.glyph.data.LyricsRepository
import com.vova.glyph.data.MusicRepository
import com.vova.glyph.data.MusicScanner
import com.vova.glyph.data.Prefs
import com.vova.glyph.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val prefs = Prefs(appContext)
    val db = GlyphDb(appContext)
    val repository = MusicRepository(db)
    val artworkStore = ArtworkStore(appContext)
    val scanner = MusicScanner(appContext, artworkStore)
    val player = PlayerConnection(appContext)
    val geniusGate = GeniusGate(appContext)
    val lyricsRepository = LyricsRepository(geniusGate)
    val coverRepository = CoverRepository(artworkStore, geniusGate)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        appScope.launch { runCatching { repository.refresh() } }
        player.connect()
    }
}
