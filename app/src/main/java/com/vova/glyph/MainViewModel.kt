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
import com.vova.glyph.data.Album
import com.vova.glyph.data.Albums
import com.vova.glyph.data.LyricLine
import com.vova.glyph.data.LyricQuote
import com.vova.glyph.data.LyricQuoteLine
import com.vova.glyph.data.OnlineTrack
import com.vova.glyph.data.Playlist
import com.vova.glyph.data.SoundPreset
import com.vova.glyph.data.Track
import com.vova.glyph.data.TrackSort
import com.vova.glyph.data.TrackSorting
import com.vova.glyph.playback.DspUiState
import com.vova.glyph.playback.PlayerProgress
import com.vova.glyph.playback.PlayerUiState
import com.vova.glyph.playback.dsp.EqPreset
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class SortTarget { LIBRARY, ALBUM, PLAYLIST }

enum class Tab(val title: String) {
    ONLINE("ПОИСК"),
    LIBRARY("БИБЛИОТЕКА"),
    ALBUMS("АЛЬБОМЫ"),
    FAVORITES("ИЗБРАННОЕ"),
}

enum class OnlineStatus { IDLE, LOADING, READY, EMPTY, FAILED }

data class OnlineUiState(
    val query: String = "",
    val status: OnlineStatus = OnlineStatus.IDLE,
    val results: List<OnlineTrack> = emptyList(),
    val message: String? = null,
)

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
    val onlineHistory = repo.onlineHistory
    val playerState: StateFlow<PlayerUiState> = player.state
    val progress: StateFlow<PlayerProgress> = player.progress
    val dsp: StateFlow<DspUiState> = player.dsp

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

    private val _unavailableIds = MutableStateFlow<Set<Long>>(emptySet())
    val unavailableIds = _unavailableIds.asStateFlow()

    private val _unavailableTrack = MutableStateFlow<Track?>(null)
    val unavailableTrack = _unavailableTrack.asStateFlow()

    private val _soundPresets = MutableStateFlow(prefs.soundPresets)
    val soundPresets = _soundPresets.asStateFlow()

    private val _folderName = MutableStateFlow(prefs.folderName)
    val folderName = _folderName.asStateFlow()

    private val _librarySorting = MutableStateFlow(
        TrackSorting.decode(prefs.librarySort, TrackSorting())
    )
    val librarySorting = _librarySorting.asStateFlow()

    private val _albumSorting = MutableStateFlow(TrackSorting(by = TrackSort.TRACK_NO))
    val albumSorting = _albumSorting.asStateFlow()

    private val _playlistSorting = MutableStateFlow(
        TrackSorting.decode(prefs.playlistSort, TrackSorting(by = TrackSort.PLAYLIST))
    )
    val playlistSorting = _playlistSorting.asStateFlow()

    private var metadataJob: Job? = null

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()

    private val _syncedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val syncedLyrics = _syncedLyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading = _lyricsLoading.asStateFlow()

    private val _lyricsTrackId = MutableStateFlow<Long?>(null)
    val lyricsTrackId = _lyricsTrackId.asStateFlow()

    private val _lyricQuotes = MutableStateFlow<List<LyricQuote>>(emptyList())
    val lyricQuotes = _lyricQuotes.asStateFlow()

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

    private val _hiddenAlbums = MutableStateFlow(prefs.hiddenAlbumKeys)

    val localTracks: StateFlow<List<Track>> = repo.tracks
        .map { list -> list.filter { it.source == Track.SOURCE_LOCAL } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = combine(repo.tracks, _hiddenAlbums) { tracks, hidden ->
        Albums.group(tracks.filter { it.source == Track.SOURCE_LOCAL }).filter { it.key !in hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _online = MutableStateFlow(OnlineUiState())
    val online: StateFlow<OnlineUiState> = _online.asStateFlow()

    private var onlineSearchJob: Job? = null

    private var onlineDebounceJob: Job? = null

    private val _openAlbumKey = MutableStateFlow<String?>(null)
    val openAlbumKey = _openAlbumKey.asStateFlow()

    private val _trackEditorId = MutableStateFlow<Long?>(null)
    val trackEditorTrack: StateFlow<Track?> = combine(_trackEditorId, repo.tracks) { id, list ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val openAlbum: StateFlow<Album?> = combine(_openAlbumKey, albums) { key, list ->
        list.firstOrNull { it.key == key }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val albumTracks: StateFlow<List<Track>> = combine(_openAlbumKey, repo.tracks) { key, list ->
        if (key == null) emptyList() else Albums.tracksOf(list, key)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val openPlaylist: StateFlow<Playlist?> = combine(_openPlaylistId, repo.playlists) { id, list ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentTrack: StateFlow<Track?> = combine(player.state, repo.tracks) { state, list ->
        list.firstOrNull { it.id == state.trackId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favoriteCount: StateFlow<Int> = repo.favorites.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        _unavailableIds.value = prefs.unavailableTracks
        viewModelScope.launch {
            _unavailableIds.collect { prefs.unavailableTracks = it }
        }
        viewModelScope.launch {
            runCatching { repo.refresh() }
            resolveUnavailable()
            startMetadataBackfill()
            runCatching { repo.pruneOnlineTracks(player.state.value.trackId) }
        }
        viewModelScope.launch {
            player.state.map { it.error }.collect { error ->
                if (!error.isNullOrBlank()) {
                    val state = player.state.value
                    val id = state.errorTrackId ?: state.trackId
                    val track = id?.let { trackId -> tracks.value.firstOrNull { it.id == trackId } }
                    if (track != null && !track.isOnline) {
                        _unavailableIds.update { it + track.id }
                        _unavailableTrack.value = track
                    } else {
                        notify(error)
                    }
                    player.clearError()
                }
            }
        }
        viewModelScope.launch {
            player.state.map { it.trackId to it.isPlaying }.distinctUntilChanged()
                .collect { (trackId, playing) ->
                    if (trackId != null && playing) {
                        _unavailableIds.update { it - trackId }
                    }
                }
        }
        viewModelScope.launch {
            player.state.map { it.trackId }.distinctUntilChanged().collect { trackId ->
                if (trackId != null) runCatching { repo.markOnlinePlayed(trackId) }
            }
        }
    }

    fun updateOnlineQuery(value: String) {
        _online.update { it.copy(query = value) }
        onlineDebounceJob?.cancel()
        val query = value.trim()
        if (query.length < MIN_ONLINE_QUERY) {
            onlineSearchJob?.cancel()
            _online.update {
                it.copy(status = OnlineStatus.IDLE, results = emptyList(), message = null)
            }
            return
        }
        onlineSearchJob?.cancel()
        onlineDebounceJob = viewModelScope.launch {
            delay(ONLINE_DEBOUNCE_MS)
            searchOnline(query)
        }
    }

    fun retryOnlineSearch() {
        val query = _online.value.query.trim()
        if (query.length < MIN_ONLINE_QUERY) return
        searchOnline(query)
    }

    private fun searchOnline(query: String) {
        onlineSearchJob?.cancel()
        onlineSearchJob = viewModelScope.launch {
            if (!hasInternet()) {
                _online.update {
                    it.copy(
                        status = OnlineStatus.FAILED,
                        results = emptyList(),
                        message = "Нет интернета — онлайн-каталог недоступен",
                    )
                }
                return@launch
            }
            _online.update { it.copy(status = OnlineStatus.LOADING, message = null) }
            runCatching { container.jioSaavn.search(query) }
                .onSuccess { tracks ->
                    _online.update {
                        it.copy(
                            status = if (tracks.isEmpty()) OnlineStatus.EMPTY else OnlineStatus.READY,
                            results = tracks,
                            message = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _online.update {
                        it.copy(
                            status = OnlineStatus.FAILED,
                            results = emptyList(),
                            message = "Каталог не отвечает. Проверьте интернет и попробуйте снова.",
                        )
                    }
                }
        }
    }

    fun playOnlineResult(track: OnlineTrack) {
        val playable = _online.value.results.filter { it.streamUrl != null }
        if (playable.none { it.id == track.id }) {
            notify("Трек недоступен")
            return
        }
        if (!hasInternet()) {
            notify("Нет интернета — трек не загрузится")
            return
        }
        viewModelScope.launch {
            val rows = runCatching { repo.saveOnlineTracks(playable.map { it.toTrack() }) }
                .getOrNull()
                .orEmpty()
            val start = rows.indexOfFirst { it.sourceId == track.id }
            if (start < 0) {
                notify("Не удалось открыть трек")
                return@launch
            }
            player.playQueue(rows, start)
        }
    }

    fun addOnlineToQueue(track: OnlineTrack) {
        if (track.streamUrl == null) {
            notify("Трек недоступен")
            return
        }
        if (!hasInternet()) {
            notify("Нет интернета — трек не загрузится")
            return
        }
        viewModelScope.launch {
            val row = saveOnlineRow(track) ?: return@launch
            player.addToQueue(row)
            notify("Добавлено в очередь")
        }
    }

    fun toggleOnlineFavorite(track: OnlineTrack) {
        viewModelScope.launch {
            val row = saveOnlineRow(track) ?: return@launch
            val favorite = runCatching { repo.toggleFavorite(row.id) }.getOrNull() ?: return@launch
            notify(if (favorite) "«${track.title}» в избранном" else "Убрано из избранного")
        }
    }

    fun addOnlineToPlaylist(playlistId: Long, track: OnlineTrack) {
        viewModelScope.launch {
            val row = saveOnlineRow(track) ?: return@launch
            addTrackToPlaylist(playlistId, row)
        }
    }

    fun playOnlineHistory(tracksToPlay: List<Track>, startIndex: Int) {
        if (tracksToPlay.isEmpty()) return
        if (!hasInternet()) {
            notify("Нет интернета — трек не загрузится")
            return
        }
        playTracks(tracksToPlay, startIndex)
    }

    fun removeOnlineFromHistory(track: Track) {
        viewModelScope.launch {
            runCatching { repo.removeOnlineFromHistory(track.id) }
                .onSuccess { notify("Удалено из истории") }
                .onFailure { notify(it.message ?: "Не удалось удалить из истории") }
        }
    }

    private suspend fun saveOnlineRow(track: OnlineTrack): Track? {
        val saved = runCatching { repo.saveOnlineTracks(listOf(track.toTrack())) }
            .getOrNull()
            .orEmpty()
        if (saved.isEmpty()) notify("Не удалось сохранить трек")
        return saved.firstOrNull()
    }

    fun skipOnboarding() {
        prefs.onboardingDone = true
        _onboardingVisible.value = false
        _onboardingError.value = null
    }

    fun setSortBy(target: SortTarget, sort: TrackSort) {
        sortingState(target).update { it.copy(by = sort) }
        persistSorting(target)
    }

    fun setSortReverse(target: SortTarget, reverse: Boolean) {
        sortingState(target).update { it.copy(reverse = reverse) }
        persistSorting(target)
    }

    private fun persistSorting(target: SortTarget) {
        when (target) {
            SortTarget.LIBRARY -> prefs.librarySort = _librarySorting.value.encode()
            SortTarget.PLAYLIST -> prefs.playlistSort = _playlistSorting.value.encode()
            SortTarget.ALBUM -> Unit
        }
    }

    private fun sortingState(target: SortTarget): MutableStateFlow<TrackSorting> = when (target) {
        SortTarget.LIBRARY -> _librarySorting
        SortTarget.ALBUM -> _albumSorting
        SortTarget.PLAYLIST -> _playlistSorting
    }

    private fun startMetadataBackfill() {
        if (metadataJob?.isActive == true) return
        metadataJob = viewModelScope.launch {
            fillFileDates()
            fillTrackNumbers()
            runCatching { repo.refresh() }
        }
    }

    private suspend fun fillFileDates() {
        val pending = runCatching { container.db.tracksWithoutFileDate() }.getOrNull().orEmpty()
        if (pending.isEmpty()) return
        val gate = Semaphore(METADATA_WORKERS)
        coroutineScope {
            pending.forEach { (id, uri) ->
                launch {
                    gate.withPermit {
                        val modifiedAt = runCatching { container.scanner.modifiedAt(uri) }
                            .getOrDefault(0L)
                        runCatching { container.db.setFileModifiedAt(id, modifiedAt) }
                    }
                }
            }
        }
    }

    private suspend fun fillTrackNumbers() {
        val pending = runCatching { container.db.tracksWithoutTrackNumber() }.getOrNull().orEmpty()
        if (pending.isEmpty()) return
        val gate = Semaphore(METADATA_WORKERS)
        coroutineScope {
            pending.forEach { (id, uri) ->
                launch {
                    gate.withPermit {
                        val number = runCatching { container.scanner.trackNumberFor(uri) }
                            .getOrDefault(0)
                        runCatching { container.db.setTrackNumber(id, number) }
                    }
                }
            }
        }
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
                resolveUnavailable()
                _scanStatus.value = "ГОТОВО"
                prefs.onboardingDone = true
                _onboardingVisible.value = false
                _tab.value = Tab.LIBRARY
                notify(
                    when {
                        saved > 0 -> "Добавлено треков: $saved"
                        result.newCount > 0 -> "Библиотека обновлена"
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

    fun onFilePicked(uri: Uri) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _onboardingError.value = null
            _scanFound.value = 0
            _scanStatus.value = "ОТКРЫВАЮ ФАЙЛ…"
            try {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val alreadyAdded = repo.tracks.value
                    .filter { it.source == Track.SOURCE_LOCAL }
                    .any { it.uri == uri.toString() }
                if (alreadyAdded) {
                    notify("Этот трек уже в библиотеке")
                    return@launch
                }
                val track = container.scanner.scanFile(uri)
                if (track == null) {
                    val error = "Не удалось прочитать аудиофайл"
                    if (_onboardingVisible.value) _onboardingError.value = error else notify(error)
                    return@launch
                }
                val saved = repo.addLocalTracks(listOf(track))
                resolveUnavailable()
                if (saved > 0) {
                    prefs.onboardingDone = true
                    _onboardingVisible.value = false
                    _tab.value = Tab.LIBRARY
                    _scanFound.value = 1
                    notify("Трек «${track.effectiveTitle}» добавлен")
                } else {
                    notify("Этот трек уже в библиотеке")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось добавить трек"
                if (_onboardingVisible.value) _onboardingError.value = error else notify(error)
            } finally {
                _scanning.value = false
                _scanStatus.value = ""
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

    private suspend fun resolveUnavailable() {
        val ids = _unavailableIds.value
        if (ids.isEmpty() || tracks.value.isEmpty()) return
        val stillMissing = ids.filterTo(mutableSetOf()) { id ->
            val track = tracks.value.firstOrNull { it.id == id } ?: return@filterTo false
            !container.scanner.exists(track.uri)
        }
        _unavailableIds.value = stillMissing
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
                                track.effectiveArtist,
                                track.effectiveTitle,
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
        if (tab != Tab.ALBUMS || _openAlbumKey.value != null) {
            closeAlbum()
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
            _trackEditorId.value != null -> {
                closeTrackEditor()
                true
            }

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

            _openAlbumKey.value != null -> {
                closeAlbum()
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
        val start = tracksToPlay.getOrNull(startIndex) ?: tracksToPlay.first()
        if (start.id in _unavailableIds.value) {
            _unavailableTrack.value = start
            return
        }
        player.playQueue(tracksToPlay, startIndex)
        if (openPlayer) _showPlayer.value = true
    }

    fun togglePlayPause() {
        val current = currentTrack.value
        if (current != null && !playerState.value.isPlaying && current.id in _unavailableIds.value) {
            _unavailableTrack.value = current
            return
        }
        player.togglePlayPause()
    }

    fun dismissUnavailableDialog() {
        _unavailableTrack.value = null
    }

    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun seekToLine(positionMs: Long) {
        player.seekTo(positionMs)
        if (!playerState.value.isPlaying) player.togglePlayPause()
    }

    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()

    fun setSpeed(value: Float) = player.setSpeed(value)

    fun setEqEnabled(enabled: Boolean) = player.setEqEnabled(enabled)

    fun setEqPreset(preset: EqPreset) = player.setEqPreset(preset)

    fun setBandLevel(index: Int, levelMb: Int) = player.setBandLevel(index, levelMb)

    fun setBassEnabled(enabled: Boolean) = player.setBassEnabled(enabled)

    fun setBassStrength(strength: Int) = player.setBassStrength(strength)

    fun setLoudnessEnabled(enabled: Boolean) = player.setLoudnessEnabled(enabled)

    fun setLoudnessGain(gainMb: Int) = player.setLoudnessGain(gainMb)

    fun setReverbEnabled(enabled: Boolean) = player.setReverbEnabled(enabled)

    fun setReverbRoom(value: Int) = player.setReverbRoom(value)

    fun setReverbDamp(value: Int) = player.setReverbDamp(value)

    fun setReverbWet(value: Int) = player.setReverbWet(value)

    fun resetDsp() = player.resetDsp()

    fun saveSoundPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val state = dsp.value
        val preset = SoundPreset(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            speed = state.speed,
            eqEnabled = state.eqEnabled,
            eqPreset = state.eqPreset.name,
            bands = state.bands.map { it.levelMb },
            bassEnabled = state.bassEnabled,
            bassStrength = state.bassStrength,
            loudnessEnabled = state.loudnessEnabled,
            loudnessGainMb = state.loudnessGainMb,
            reverbEnabled = state.reverbEnabled,
            reverbRoom = state.reverbRoom,
            reverbDamp = state.reverbDamp,
            reverbWet = state.reverbWet,
        )
        val existing = _soundPresets.value
        val sameName = existing.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val updated = if (sameName == null) {
            existing + preset
        } else {
            existing.map { if (it.id == sameName.id) preset.copy(id = sameName.id) else it }
        }
        _soundPresets.value = updated
        prefs.soundPresets = updated
        notify("Пресет сохранён")
    }

    fun applySoundPreset(preset: SoundPreset) {
        player.applySoundPreset(preset)
    }

    fun deleteSoundPreset(preset: SoundPreset) {
        val updated = _soundPresets.value.filterNot { it.id == preset.id }
        _soundPresets.value = updated
        prefs.soundPresets = updated
        notify("Пресет удалён")
    }

    fun fetchLyrics(track: Track) {
        if (_lyricsTrackId.value == track.id && (_lyrics.value != null || _lyricsLoading.value)) return
        viewModelScope.launch { loadLyrics(track) }
    }

    private suspend fun loadLyrics(track: Track) {
        _lyricsTrackId.value = track.id
        _lyricsLoading.value = true
        _lyrics.value = null
        _syncedLyrics.value = emptyList()
        _lyricQuotes.value = emptyList()
        refreshLyricQuotes(track.id)

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
                container.lyricsRepository.fetchLyrics(track.effectiveArtist, track.effectiveTitle)
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
        _lyricQuotes.value = emptyList()
        _lyricsTrackId.value = null
        _lyricsLoading.value = false
    }

    fun saveLyricQuote(trackId: Long, lines: List<LyricQuoteLine>) {
        if (lines.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.addLyricQuote(trackId, lines) }
            refreshLyricQuotes(trackId)
        }
    }

    fun deleteLyricQuote(trackId: Long, quoteId: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteLyricQuote(trackId, quoteId) }
            refreshLyricQuotes(trackId)
        }
    }

    fun removeLyricHighlights(trackId: Long, lineIndices: List<Int>) {
        if (lineIndices.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.deleteLyricHighlights(trackId, lineIndices) }
            refreshLyricQuotes(trackId)
        }
    }

    private suspend fun refreshLyricQuotes(trackId: Long) {
        val quotes = runCatching { repo.lyricQuotes(trackId) }
            .getOrDefault(emptyList())
            .sortedBy { it.firstLineIndex }
        if (_lyricsTrackId.value == trackId) _lyricQuotes.value = quotes
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = runCatching { repo.toggleFavorite(track.id) }.getOrNull() ?: return@launch
            notify(if (favorite) "«${track.effectiveTitle}» в избранном" else "Убрано из избранного")
        }
    }

    fun updateTrackCover(track: Track) {
        if (track.isOnline) {
            notify("Обложку можно обновить только для локального трека")
            return
        }
        if (artworkJob?.isActive == true) {
            notify("Дождитесь окончания обновления обложек")
            return
        }
        if (!hasInternet()) {
            notify("Нет интернета — обложку обновить не получится")
            return
        }
        viewModelScope.launch {
            val path = runCatching {
                container.coverRepository.downloadCover(
                    track.effectiveArtist,
                    track.effectiveTitle,
                    container.artworkStore.keyFor(track.uri),
                )
            }.getOrNull()
            if (path == null) {
                notify("Не удалось найти обложку — попробуйте позже")
                return@launch
            }
            val saved = runCatching {
                repo.setArtwork(track.id, path)
                repo.refresh()
            }.isSuccess
            if (!saved) {
                container.artworkStore.deleteReplacedArt(path)
                notify("Не удалось обновить обложку")
                return@launch
            }
            if (track.artwork != path) {
                container.artworkStore.deleteReplacedArt(track.artwork)
            }
            notify("Обложка обновлена")
        }
    }

    fun setCustomCover(track: Track, uri: Uri) {
        viewModelScope.launch {
            val path = container.artworkStore.saveCustomCover(uri)
            if (path == null) {
                notify("Не удалось установить обложку")
                return@launch
            }
            runCatching {
                repo.setArtwork(track.id, path)
                repo.refresh()
            }.onSuccess {
                container.artworkStore.deleteReplacedArt(track.artwork)
                notify("Обложка обновлена")
            }.onFailure {
                container.artworkStore.deleteReplacedArt(path)
                notify(it.message ?: "Не удалось установить обложку")
            }
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

    fun openAlbum(key: String) {
        _openAlbumKey.value = key
    }

    fun closeAlbum() {
        _openAlbumKey.value = null
    }

    fun openTrackEditor(track: Track) {
        _trackEditorId.value = track.id
    }

    fun closeTrackEditor() {
        _trackEditorId.value = null
    }

    fun saveTrackInfo(track: Track, title: String, artist: String) {
        val newTitle = title.trim()
        if (newTitle.isBlank()) {
            notify("У трека должно быть название")
            return
        }
        val titleOverride = newTitle.takeUnless { it == track.title.trim() }
        val artistOverride = artist.trim()
            .takeIf { it.isNotBlank() }
            ?.takeUnless { it == track.artist.trim() }
        val nextTitle = titleOverride ?: track.title.trim()
        val nextArtist = artistOverride ?: track.artist.trim()
        val changed = nextTitle != track.effectiveTitle || nextArtist != track.effectiveArtist
        viewModelScope.launch {
            runCatching { repo.setTrackInfo(track.id, titleOverride, artistOverride) }
                .onSuccess {
                    _trackEditorId.value = null
                    repo.tracks.value.firstOrNull { it.id == track.id }
                        ?.let(player::updateTrackMetadata)
                    val openAlbum = _openAlbumKey.value
                    if (openAlbum != null && repo.tracks.value.none { Albums.keyOf(it) == openAlbum }) {
                        closeAlbum()
                    }
                    notify(if (changed) "Трек обновлён" else "Изменений нет")
                }
                .onFailure { notify(it.message ?: "Не удалось сохранить трек") }
        }
    }

    fun deleteAlbum(album: Album) {
        deleteAlbums(listOf(album))
    }

    fun deleteAlbums(albums: List<Album>) {
        if (albums.isEmpty()) return
        val keys = albums.mapTo(HashSet()) { it.key }
        prefs.hideAlbums(keys)
        _hiddenAlbums.update { it + keys }
        if (_openAlbumKey.value in keys) closeAlbum()
        notify(
            if (albums.size == 1) {
                "Альбом «${albums.first().name}» убран из вкладки «Альбомы»"
            } else {
                "Убрано альбомов из вкладки «Альбомы»: ${albums.size}"
            }
        )
    }

    fun deleteTrack(track: Track) {
        deleteTracks(listOf(track.id))
    }

    fun deleteTracks(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val tracks = repo.tracks.value.filter { it.id in ids }
            if (tracks.isEmpty()) {
                notify("Треков для удаления нет")
                return@launch
            }
            if (playerState.value.trackId in ids) player.stop()
            val openPlaylist = _openPlaylistId.value
            runCatching { repo.deleteTracks(ids.toList()) }
                .onSuccess {
                    tracks.forEach { container.artworkStore.deleteReplacedArt(it.artwork) }
                    _unavailableIds.update { it - ids }
                    if (_unavailableTrack.value?.id in ids) _unavailableTrack.value = null
                    if (_lyricsTrackId.value in ids) clearLyrics()
                    if (openPlaylist != null) {
                        _playlistTracks.value = repo.playlistTracks(openPlaylist)
                    }
                    notify(
                        if (tracks.size == 1) {
                            "Трек «${tracks.first().effectiveTitle}» убран из приложения"
                        } else {
                            "Убрано треков: ${tracks.size}"
                        }
                    )
                }
                .onFailure { notify(it.message ?: "Не удалось убрать трек") }
        }
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
        deletePlaylists(listOf(playlist.id))
    }

    fun deletePlaylists(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val playlists = repo.playlists.value.filter { it.id in ids }
            if (playlists.isEmpty()) return@launch
            runCatching { playlists.forEach { repo.deletePlaylist(it.id) } }
                .onSuccess {
                    if (_openPlaylistId.value in ids) closePlaylist()
                    playlists.forEach { container.artworkStore.deleteCover(it.cover) }
                    notify(
                        if (playlists.size == 1) {
                            "Плейлист «${playlists.first().name}» удалён"
                        } else {
                            "Удалено плейлистов: ${playlists.size}"
                        }
                    )
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

        const val METADATA_WORKERS = 4

        const val MAX_NETWORK_ERRORS = 4

        const val MAX_LYRICS_RETRY_MS = 4 * 60_000L

        const val ONLINE_DEBOUNCE_MS = 600L

        const val MIN_ONLINE_QUERY = 2
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val glyphApp = app as GlyphApp
            return MainViewModel(app, glyphApp.container) as T
        }
    }
}
