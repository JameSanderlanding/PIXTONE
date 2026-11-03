package com.vova.glyph

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vova.glyph.data.LyricLine
import com.vova.glyph.data.Playlist
import com.vova.glyph.data.Track
import com.vova.glyph.playback.PlayerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class Tab(val title: String) {
    LIBRARY("БИБЛИОТЕКА"),
    FAVORITES("ИЗБРАННОЕ"),
}

class MainViewModel(
    private val app: Application,
    private val container: AppContainer,
) : ViewModel() {
    private val repo = container.repository
    private val prefs = container.prefs
    private val player = container.player

    val tracks = repo.tracks
    val favorites = repo.favorites
    val playlists = repo.playlists
    val playerState: StateFlow<PlayerUiState> = player.state

    private val _tab = MutableStateFlow(Tab.LIBRARY)
    val tab = _tab.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer = _showPlayer.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings = _showSettings.asStateFlow()

    private val _onboardingVisible = MutableStateFlow(!prefs.onboardingDone)
    val onboardingVisible = _onboardingVisible.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private val _scanStatus = MutableStateFlow("")
    val scanStatus = _scanStatus.asStateFlow()

    private val _scanFound = MutableStateFlow(0)
    val scanFound = _scanFound.asStateFlow()

    private val _onboardingError = MutableStateFlow<String?>(null)
    val onboardingError = _onboardingError.asStateFlow()

    private val _folderName = MutableStateFlow(prefs.folderName)
    val folderName = _folderName.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()

    private val _syncedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val syncedLyrics = _syncedLyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading = _lyricsLoading.asStateFlow()

    private val _lyricsTrackId = MutableStateFlow<Long?>(null)
    val lyricsTrackId = _lyricsTrackId.asStateFlow()

    private val _artworkStatus = MutableStateFlow<String?>(null)
    val artworkStatus = _artworkStatus.asStateFlow()

    private var artworkJob: Job? = null

    private val _coverPaused = MutableStateFlow(false)
    val coverPaused = _coverPaused.asStateFlow()

    private var activeCoverTracks: List<Track> = emptyList()
    private var coverRemainingIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _message.asSharedFlow()

    private val _openPlaylistId = MutableStateFlow<Long?>(null)
    val openPlaylistId = _openPlaylistId.asStateFlow()

    private val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks = _playlistTracks.asStateFlow()

    val openPlaylist: StateFlow<Playlist?> = combine(_openPlaylistId, repo.playlists) { id, list ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentTrack: StateFlow<Track?> = combine(player.state, repo.tracks) { state, list ->
        list.firstOrNull { it.id == state.trackId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favoriteCount: StateFlow<Int> = repo.favorites.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            runCatching { repo.refresh() }
        }
        viewModelScope.launch {
            player.state.map { it.error }.collect { error ->
                if (!error.isNullOrBlank()) {
                    notify(error)
                    player.clearError()
                }
            }
        }
    }

    fun skipOnboarding() {
        prefs.onboardingDone = true
        _onboardingVisible.value = false
        _onboardingError.value = null
    }

    fun onFolderPicked(uri: Uri, fromOnboarding: Boolean) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _onboardingError.value = null
            _scanFound.value = 0
            _scanStatus.value = "ОТКРЫВАЮ ПАПКУ…"
            try {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                prefs.folderUri = uri.toString()
                val existing = repo.tracks.value
                    .filter { it.source == Track.SOURCE_LOCAL }
                    .associateBy { it.uri }
                val result = container.scanner.scan(uri, existing) { scanned, found, current ->
                    _scanFound.value = found
                    _scanStatus.value = buildString {
                        append("ПРОСМОТРЕНО: ").append(scanned)
                        if (current.isNotBlank()) append("  ·  ").append(current.take(34))
                    }
                }
                prefs.folderName = result.folderName
                _folderName.value = result.folderName
                if (result.scannedFiles == 0) {
                    val error = "В этой папке не нашлось поддерживаемых аудиофайлов."
                    if (fromOnboarding) _onboardingError.value = error else notify(error)
                    return@launch
                }
                val saved = repo.addLocalTracks(result.added)
                _scanStatus.value = "ГОТОВО"
                prefs.onboardingDone = true
                _onboardingVisible.value = false
                _tab.value = Tab.LIBRARY
                notify(
                    when {
                        result.newCount > 0 -> "Добавлено треков: ${result.newCount}"
                        saved > 0 -> "Библиотека обновлена"
                        else -> "Новых треков нет — всё уже в библиотеке"
                    }
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось прочитать папку"
                if (fromOnboarding) _onboardingError.value = error else notify(error)
            } finally {
                _scanning.value = false
            }
        }
    }

    fun rescanFolder() {
        val uri = prefs.folderUriOrNull() ?: run {
            notify("Сначала выберите папку с музыкой")
            return
        }
        onFolderPicked(uri, fromOnboarding = false)
    }

    fun findMissingCovers() {
        startCoverRefresh(allTracks = false)
    }

    fun refreshAllCovers() {
        startCoverRefresh(allTracks = true)
    }

    private fun startCoverRefresh(allTracks: Boolean) {
        if (_scanning.value) {
            notify("Дождитесь окончания сканирования")
            return
        }
        if (repo.tracks.value.isEmpty()) {
            notify("В библиотеке пока нет треков")
            return
        }
        if (!hasInternet()) {
            notify("Нет интернета — обложки обновить не получится")
            return
        }
        if (artworkJob?.isActive == true) return
        val pending = repo.tracks.value.filter { track ->
            track.source == Track.SOURCE_LOCAL &&
                (allTracks || !container.artworkStore.hasArtwork(track.artwork))
        }
        if (pending.isEmpty()) {
            if (!allTracks) notify("Все обложки уже на месте")
            return
        }
        activeCoverTracks = pending
        val remaining = ConcurrentHashMap.newKeySet<Long>()
        remaining.addAll(pending.map { it.id })
        coverRemainingIds = remaining
        _coverPaused.value = false
        artworkJob = viewModelScope.launch { runArtworkPass(pending, remaining) }
    }

    fun pauseCoverRefresh() {
        if (artworkJob?.isActive == true && !_coverPaused.value) {
            _coverPaused.value = true
            _artworkStatus.value = "ПРИОСТАНОВЛЕНО"
            val remaining = coverRemainingIds.toSet()
            activeCoverTracks = activeCoverTracks.filter { it.id in remaining }
            artworkJob?.cancel()
        }
    }

    fun resumeCoverRefresh() {
        if (!_coverPaused.value) return
        val tracks = activeCoverTracks
        _coverPaused.value = false
        if (tracks.isEmpty()) {
            _artworkStatus.value = null
            return
        }
        coverRemainingIds = ConcurrentHashMap.newKeySet()
        coverRemainingIds.addAll(tracks.map { it.id })
        val remaining = coverRemainingIds
        artworkJob = viewModelScope.launch { runArtworkPass(tracks, remaining) }
    }

    fun resetCovers() {
        if (_scanning.value) {
            notify("Дождитесь окончания сканирования")
            return
        }
        if (artworkJob?.isActive == true) return
        viewModelScope.launch {
            val tracks = repo.tracks.value.filter { it.source == Track.SOURCE_LOCAL }
            tracks.forEach { track ->
                val original = container.scanner.embeddedArtworkFor(track.uri)
                if (original != track.artwork) {
                    container.artworkStore.deleteReplacedArt(track.artwork)
                    runCatching { repo.setArtwork(track.id, original) }
                }
            }
            runCatching { repo.refresh() }
            notify("Исходные обложки восстановлены")
        }
    }

    private suspend fun runArtworkPass(tracks: List<Track>, remaining: MutableSet<Long>) {
        if (tracks.isEmpty() || !hasInternet()) return

        val total = tracks.size
        _artworkStatus.value = "ОБНОВЛЯЮ ОБЛОЖКИ: 0/$total"

        val processed = AtomicInteger(0)
        val updated = AtomicInteger(0)
        val networkErrors = AtomicInteger(0)
        val semaphore = Semaphore(COVER_WORKERS)

        coroutineScope {
            for (track in tracks) {
                launch {
                    semaphore.withPermit {
                        if (networkErrors.get() >= MAX_NETWORK_ERRORS && !hasInternet()) {
                            remaining.remove(track.id)
                            val done = processed.incrementAndGet()
                            _artworkStatus.value = "ОБНОВЛЯЮ ОБЛОЖКИ: $done/$total"
                            return@withPermit
                        }
                        try {
                            val path = container.coverRepository.downloadCover(
                                track.artist,
                                track.title,
                                container.artworkStore.keyFor(track.uri),
                            )
                            if (path != null) {
                                networkErrors.set(0)
                                repo.setArtwork(track.id, path)
                                if (track.artwork != path) {
                                    container.artworkStore.deleteReplacedArt(track.artwork)
                                }

                                if (updated.incrementAndGet() % 4 == 0) {
                                    runCatching { repo.refresh() }
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: IOException) {
                            networkErrors.incrementAndGet()
                        } catch (_: Exception) {
                        }
                        remaining.remove(track.id)
                        val done = processed.incrementAndGet()
                        _artworkStatus.value = "ОБНОВЛЯЮ ОБЛОЖКИ: $done/$total"
                    }
                }
            }
        }

        runCatching { repo.refresh() }
        activeCoverTracks = emptyList()
        coverRemainingIds.clear()
        _coverPaused.value = false
        _artworkStatus.value = null
        val count = updated.get()
        if (count > 0) {
            notify("Обложки обновлены: $count")
        } else {
            notify("Не удалось найти обложки — проверьте интернет или попробуйте позже")
        }
    }

    private fun hasInternet(): Boolean {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun selectTab(tab: Tab) {
        _tab.value = tab
        if (tab != Tab.LIBRARY || _openPlaylistId.value != null) {
            closePlaylist()
        }
    }

    fun openPlayer() {
        _showPlayer.value = true
    }

    fun closePlayer() {
        _showPlayer.value = false
    }

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun handleBack(): Boolean {
        return when {
            _showPlayer.value -> {
                _showPlayer.value = false
                true
            }

            _showSettings.value -> {
                _showSettings.value = false
                true
            }

            _openPlaylistId.value != null -> {
                closePlaylist()
                true
            }

            _tab.value != Tab.LIBRARY -> {
                _tab.value = Tab.LIBRARY
                true
            }

            else -> false
        }
    }

    fun playTracks(tracksToPlay: List<Track>, startIndex: Int, openPlayer: Boolean = false) {
        if (tracksToPlay.isEmpty()) return
        player.playQueue(tracksToPlay, startIndex)
        if (openPlayer) _showPlayer.value = true
    }

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun seekToLine(positionMs: Long) {
        player.seekTo(positionMs)
        if (!playerState.value.isPlaying) player.togglePlayPause()
    }

    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()

    fun fetchLyrics(track: Track) {
        if (_lyricsTrackId.value == track.id && (_lyrics.value != null || _lyricsLoading.value)) return
        viewModelScope.launch { loadLyrics(track) }
    }

    private suspend fun loadLyrics(track: Track) {
        _lyricsTrackId.value = track.id
        _lyricsLoading.value = true
        _lyrics.value = null
        _syncedLyrics.value = emptyList()

        val cached = runCatching { repo.cachedLyrics(track.id) }.getOrNull()
        val cachedHasText = cached != null &&
            (cached.plain != null || cached.synced.isNotEmpty())
        if (cachedHasText) {
            if (_lyricsTrackId.value == track.id) {
                _lyrics.value = cached?.plain
                _syncedLyrics.value = cached?.synced.orEmpty()
                _lyricsLoading.value = false
            }
            return
        }

        var waited = 0L
        while (true) {
            val result = runCatching {
                container.lyricsRepository.fetchLyrics(track.artist, track.title)
            }.getOrNull()

            if (result != null) {
                runCatching { repo.saveLyrics(track.id, result) }

                if (_lyricsTrackId.value == track.id) {
                    _lyrics.value = result.plain
                    _syncedLyrics.value = result.synced
                    _lyricsLoading.value = false
                }
                return
            }

            val blockLeft = container.geniusGate.blockedForMs
            if (blockLeft <= 0L || blockLeft > MAX_LYRICS_RETRY_MS) break
            if (waited >= MAX_LYRICS_RETRY_MS) break
            if (_lyricsTrackId.value != track.id) return

            val pause = minOf(blockLeft + 500L, MAX_LYRICS_RETRY_MS - waited)
            delay(pause)
            waited += pause
            if (_lyricsTrackId.value != track.id) return
        }

        if (_lyricsTrackId.value == track.id) {
            _lyrics.value = null
            _syncedLyrics.value = emptyList()
            _lyricsLoading.value = false
        }
    }

    fun clearLyrics() {
        _lyrics.value = null
        _syncedLyrics.value = emptyList()
        _lyricsTrackId.value = null
        _lyricsLoading.value = false
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = runCatching { repo.toggleFavorite(track.id) }.getOrNull() ?: return@launch
            notify(if (favorite) "«${track.title}» в избранном" else "Убрано из избранного")
        }
    }

    fun openPlaylist(id: Long) {
        _openPlaylistId.value = id
        viewModelScope.launch {
            _playlistTracks.value = repo.playlistTracks(id)
        }
    }

    fun closePlaylist() {
        _openPlaylistId.value = null
        _playlistTracks.value = emptyList()
    }

    fun createPlaylist(name: String, description: String, coverUri: Uri?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            notify("У плейлиста должно быть название")
            return
        }
        viewModelScope.launch {
            val coverPath = coverUri?.let { uri ->
                runCatching { container.artworkStore.copyPlaylistCover(uri) }.getOrNull()
            }
            runCatching {
                repo.createPlaylist(trimmed, description.trim(), coverPath)
            }.onSuccess {
                notify("Плейлист «$trimmed» создан")
            }.onFailure {
                notify(it.message ?: "Не удалось создать плейлист")
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            runCatching { repo.deletePlaylist(playlist.id) }
                .onSuccess {
                    if (_openPlaylistId.value == playlist.id) closePlaylist()
                    container.artworkStore.deleteCover(playlist.cover)
                    notify("Плейлист «${playlist.name}» удалён")
                }
                .onFailure { notify(it.message ?: "Не удалось удалить плейлист") }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            runCatching { repo.addToPlaylist(playlistId, track.id) }
                .onSuccess { added ->
                    if (!added) notify("Трек уже в этом плейлисте")
                    if (_openPlaylistId.value == playlistId) {
                        _playlistTracks.value = repo.playlistTracks(playlistId)
                    }
                }
                .onFailure { notify(it.message ?: "Не удалось добавить трек") }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            runCatching { repo.removeFromPlaylist(playlistId, track.id) }
                .onSuccess {
                    if (_openPlaylistId.value == playlistId) {
                        _playlistTracks.value = repo.playlistTracks(playlistId)
                    }
                    notify("Трек убран из плейлиста")
                }
                .onFailure { notify(it.message ?: "Не удалось убрать трек") }
        }
    }

    private fun notify(text: String) {
        _message.tryEmit(text)
    }

    private companion object {
        const val COVER_WORKERS = 4

        const val MAX_NETWORK_ERRORS = 4

        const val MAX_LYRICS_RETRY_MS = 4 * 60_000L
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val glyphApp = app as GlyphApp
            return MainViewModel(app, glyphApp.container) as T
        }
    }
}
