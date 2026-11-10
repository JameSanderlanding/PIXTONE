package com.vova.glyph.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.SortTarget
import com.vova.glyph.data.Playlist
import com.vova.glyph.data.Track
import com.vova.glyph.data.TrackSearch
import com.vova.glyph.data.TrackSort
import com.vova.glyph.data.TrackSorter
import com.vova.glyph.data.TrackSorting
import com.vova.glyph.ui.components.ConfirmDialog
import com.vova.glyph.ui.components.CreatePlaylistDialog
import com.vova.glyph.ui.components.DotSpinner
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphCover
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.LyricsDialog
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.ScreenHeader
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.SelectionBar
import com.vova.glyph.ui.components.SortRow
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.TrackRow
import com.vova.glyph.ui.theme.DotoFamily
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

private enum class LibrarySelection { TRACK, PLAYLIST }

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    val folderName by vm.folderName.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanStatus by vm.scanStatus.collectAsStateWithLifecycle()
    val scanFound by vm.scanFound.collectAsStateWithLifecycle()
    val sorting by vm.librarySorting.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var playlistTarget by remember { mutableStateOf<Track?>(null) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsTrack by remember { mutableStateOf<Track?>(null) }
    var lyricsCompletionBase by remember { mutableStateOf(0L) }

    var selectionType by remember { mutableStateOf<LibrarySelection?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val collapsed = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { atTop -> if (atTop) collapsed.value = false }
    }

    val searchScrollConnection = remember(density) {
        object : NestedScrollConnection {
            private val hideThreshold = with(density) { 40.dp.roundToPx().toFloat() }
            private val showThreshold = with(density) { 140.dp.roundToPx().toFloat() }
            private var drag = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y == 0f) return Offset.Zero
                if (collapsed.value) {
                    drag = if (consumed.y > 0f) drag + consumed.y else 0f
                    if (drag >= showThreshold) {
                        collapsed.value = false
                        drag = 0f
                    }
                } else {
                    drag = if (consumed.y < 0f) drag - consumed.y else 0f
                    if (drag >= hideThreshold) {
                        collapsed.value = true
                        drag = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    fun clearSelection() {
        selectionType = null
        selectedIds = emptySet()
    }

    fun toggleSelection(type: LibrarySelection, id: Long) {
        if (selectionType != type) return
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selectionType != null) { clearSelection() }

    LaunchedEffect(playerState.completionCount) {
        val track = lyricsTrack ?: return@LaunchedEffect
        if (!showLyrics || playerState.completionCount <= lyricsCompletionBase) return@LaunchedEffect
        if (playerState.completedTrackId == track.id) {
            showLyrics = false
            lyricsTrack = null
            vm.clearLyrics()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) coverUri = uri }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) vm.onFolderPicked(uri, fromOnboarding = false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.onFilePicked(uri) }

    val sorted = remember(tracks, query, sorting) {
        TrackSorter.apply(TrackSearch.filter(tracks, query), sorting)
    }

    val librarySubtitle = buildString {
        append("${tracks.size} ТРЕКОВ · ${favorites.size} В ИЗБРАННОМ")
        folderName?.takeIf { it.isNotBlank() }?.let {
            append(" · ${it.uppercase()}")
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (selectionType != null) {
            SelectionBar(
                selectedCount = selectedIds.size,
                allSelected = when (selectionType) {
                    LibrarySelection.TRACK ->
                        sorted.isNotEmpty() && sorted.all { it.id in selectedIds }
                    LibrarySelection.PLAYLIST ->
                        playlists.isNotEmpty() && playlists.all { it.id in selectedIds }
                    else -> false
                },
                onCancel = { clearSelection() },
                onToggleAll = {
                    val ids = when (selectionType) {
                        LibrarySelection.TRACK -> sorted.mapTo(HashSet()) { it.id }
                        LibrarySelection.PLAYLIST -> playlists.mapTo(HashSet()) { it.id }
                        else -> emptySet()
                    }
                    selectedIds = if (ids.isNotEmpty() && selectedIds.containsAll(ids)) {
                        emptySet()
                    } else {
                        ids
                    }
                },
                onConfirm = { if (selectedIds.isNotEmpty()) showDeleteConfirm = true },
            )
        }

        ScreenHeader(
            index = "01",
            title = "Библиотека",
            subtitle = librarySubtitle,
            onSettings = vm::openSettings,
        )
        Spacer(Modifier.height(18.dp))
        if (!scanning) {
            AnimatedVisibility(
                visible = !collapsed.value || query.isNotBlank(),
                enter = fadeIn(animationSpec = tween(120)) +
                    expandVertically(animationSpec = tween(180), expandFrom = Alignment.Top),
                exit = fadeOut(animationSpec = tween(90)) +
                    shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top),
            ) {
                Column {
                    LibrarySearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    SortRow(
                        options = listOf(
                            TrackSort.TITLE to "НАЗВАНИЕ",
                            TrackSort.ADDED to "ДОБАВЛЕНО",
                            TrackSort.MODIFIED to "ИЗМЕНЕНО",
                        ),
                        sorting = sorting,
                        onSortBy = { vm.setSortBy(SortTarget.LIBRARY, it) },
                        onReverse = { vm.setSortReverse(SortTarget.LIBRARY, it) },
                    )
                }
            }
        }

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(searchScrollConnection),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        if (scanning) {
            item {
                LibraryScanCard(
                    status = scanStatus,
                    found = scanFound,
                )
            }
        } else {
            stickyHeader(key = "playlists") {
                SectionHeader(
                    number = "02",
                    title = "ПЛЕЙЛИСТЫ",
                    modifier = Modifier.background(GlyphBlack).padding(top = 8.dp),
                    action = {
                        if (selectionType == null) {
                            Row(
                                Modifier.clickable { showCreate = true },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GlyphIcon(Glyph.Plus, tint = GlyphWhite, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(5.dp))
                                MicroLabel("СОЗДАТЬ", color = GlyphWhite)
                            }
                        }
                    },
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (selectionType == null) {
                        item(key = "create") {
                            CreatePlaylistCard(onClick = { showCreate = true })
                        }
                    }
                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { "playlist" },
                    ) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = {
                                when (selectionType) {
                                    LibrarySelection.PLAYLIST ->
                                        toggleSelection(LibrarySelection.PLAYLIST, playlist.id)
                                    null -> vm.openPlaylist(playlist.id)
                                    else -> Unit
                                }
                            },
                            selectionMode = selectionType == LibrarySelection.PLAYLIST,
                            selected = playlist.id in selectedIds,
                            onLongClick = {
                                if (selectionType == null) {
                                    selectionType = LibrarySelection.PLAYLIST
                                    selectedIds = setOf(playlist.id)
                                }
                            },
                        )
                    }
                }
            }
            stickyHeader(key = "tracks") {
                SectionHeader(
                    number = "03",
                    title = if (query.isBlank()) "ВСЕ ТРЕКИ" else "РЕЗУЛЬТАТЫ: ${sorted.size}",
                    modifier = Modifier.background(GlyphBlack).padding(top = 10.dp),
                    action = {
                        if (query.isBlank() && selectionType == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tracks.isNotEmpty()) {
                                    Row(
                                        Modifier.clickable { folderPicker.launch(null) },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        GlyphIcon(Glyph.Folder, tint = GlyphGray, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(5.dp))
                                        MicroLabel("ПАПКА", color = GlyphGray)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                }
                                Row(
                                    Modifier.clickable { filePicker.launch(arrayOf("audio/*")) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    GlyphIcon(Glyph.Plus, tint = GlyphWhite, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(5.dp))
                                    MicroLabel("ФАЙЛ", color = GlyphWhite)
                                }
                            }
                        }
                    },
                )
            }
            if (tracks.isEmpty()) {
                item {
                    EmptyState(
                        glyph = Glyph.Library,
                        title = "Здесь пока пусто",
                        body = "Выберите папку с музыкой на устройстве.",
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlyphButton(
                                    text = "ВЫБРАТЬ ПАПКУ",
                                    onClick = { folderPicker.launch(null) },
                                    leading = Glyph.Folder,
                                )
                                Spacer(Modifier.height(8.dp))
                                GlyphButton(
                                    text = "ВЫБРАТЬ ФАЙЛ",
                                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                                    filled = false,
                                    leading = Glyph.Plus,
                                )
                            }
                        },
                    )
                }
            } else if (sorted.isEmpty()) {
                item {
                    EmptyState(
                        glyph = Glyph.Search,
                        title = "Ничего не найдено",
                        body = "Попробуйте другое название, исполнителя или альбом.",
                    )
                }
            } else {
                itemsIndexed(
                    items = sorted,
                    key = { _, track -> track.id },
                    contentType = { _, _ -> "track" },
                ) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = playerState.isPlaying && currentTrack?.id == track.id,
                        onClick = {
                            when (selectionType) {
                                LibrarySelection.TRACK ->
                                    toggleSelection(LibrarySelection.TRACK, track.id)
                                null -> vm.playTracks(sorted, index)
                                else -> Unit
                            }
                        },
                        onFavorite = { vm.toggleFavorite(track) },
                        onLyrics = {
                            lyricsTrack = track
                            lyricsCompletionBase = playerState.completionCount
                            vm.fetchLyrics(track)
                            showLyrics = true
                        },
                        onAddToPlaylist = { playlistTarget = track },
                        onEditArtist = { vm.openArtistEditor(track) },
                        onDelete = { vm.deleteTrack(track) },
                        selectionMode = selectionType == LibrarySelection.TRACK,
                        selected = track.id in selectedIds,
                        onLongClick = {
                            if (selectionType == null) {
                                selectionType = LibrarySelection.TRACK
                                selectedIds = setOf(track.id)
                            }
                        },
                    )
                }
            }
        }
        }
    }

    if (showDeleteConfirm) {
        val type = selectionType
        val ids = selectedIds
        ConfirmDialog(
            title = if (type == LibrarySelection.TRACK) {
                "Удалить выбранные треки?"
            } else {
                "Удалить выбранные плейлисты?"
            },
            message = if (type == LibrarySelection.TRACK) {
                "Будет убрано треков: ${ids.size}. Файлы на устройстве останутся, при повторном сканировании папки треки вернутся в библиотеку."
            } else {
                "Будет удалено плейлистов: ${ids.size}. Плейлисты не восстанавливаются, треки останутся в библиотеке."
            },
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                clearSelection()
                when (type) {
                    LibrarySelection.TRACK -> vm.deleteTracks(ids)
                    LibrarySelection.PLAYLIST -> vm.deletePlaylists(ids)
                    null -> Unit
                }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showCreate) {
        CreatePlaylistDialog(
            coverUri = coverUri,
            onPickCover = { imagePicker.launch("image/*") },
            onDismiss = {
                showCreate = false
                coverUri = null
            },
            onCreate = { name, description ->
                vm.createPlaylist(name, description, coverUri)
                showCreate = false
                coverUri = null
            },
        )
    }

    playlistTarget?.let { track ->
        com.vova.glyph.ui.components.AddToPlaylistSheet(
            track = track,
            playlists = playlists,
            onDismiss = { playlistTarget = null },
            onSelect = { playlist ->
                vm.addTrackToPlaylist(playlist.id, track)
                playlistTarget = null
            },
            onCreateNew = {
                playlistTarget = null
                showCreate = true
            },
        )
    }

    if (showLyrics) {
        lyricsTrack?.let { track ->
            LibraryLyricsDialog(
                vm = vm,
                track = track,
                isCurrent = currentTrack?.id == track.id,
                onClose = {
                    showLyrics = false
                    lyricsTrack = null
                    vm.clearLyrics()
                },
            )
        }
    }
}

@Composable
private fun LibraryLyricsDialog(
    vm: MainViewModel,
    track: Track,
    isCurrent: Boolean,
    onClose: () -> Unit,
) {
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val syncedLyrics by vm.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsLoading by vm.lyricsLoading.collectAsStateWithLifecycle()
    val lyricQuotes by vm.lyricQuotes.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()

    LyricsDialog(
        track = track,
        lyrics = lyrics,
        syncedLines = syncedLyrics,
        quotes = lyricQuotes,
        positionMs = if (isCurrent) progress.positionMs else null,
        durationMs = if (isCurrent) progress.durationMs else 0L,
        onSeek = if (isCurrent) vm::seekToLine else null,
        loading = lyricsLoading,
        onRetry = { vm.fetchLyrics(track) },
        onSaveQuote = { lines -> vm.saveLyricQuote(track.id, lines) },
        onDeleteQuote = { quoteId -> vm.deleteLyricQuote(track.id, quoteId) },
        onRemoveHighlights = { indices -> vm.removeLyricHighlights(track.id, indices) },
        onDismiss = onClose,
    )
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(GlyphSurface, RoundedCornerShape(2.dp))
            .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.Search, tint = GlyphGray, modifier = Modifier.size(16.dp), stroke = 1.5.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Поиск по названию или исполнителю",
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = GlyphWhite,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(GlyphRed),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier.size(26.dp).clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.Close, tint = GlyphGray, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .width(116.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Box {
            GlyphCover(
                artwork = playlist.cover,
                seed = playlist.name,
                modifier = Modifier
                    .size(116.dp)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, GlyphRed, RoundedCornerShape(2.dp))
                        } else {
                            Modifier
                        }
                    ),
            )
            if (selectionMode) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(22.dp)
                        .background(
                            if (selected) GlyphRed else GlyphBlack.copy(alpha = 0.72f),
                            RoundedCornerShape(2.dp),
                        )
                        .border(
                            1.dp,
                            if (selected) GlyphRed else GlyphLine,
                            RoundedCornerShape(2.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        GlyphIcon(
                            Glyph.Check,
                            tint = GlyphWhite,
                            modifier = Modifier.size(13.dp),
                            stroke = 2.dp,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            playlist.name,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${playlist.trackCount} ТРЕКОВ",
            style = MaterialTheme.typography.labelSmall.copy(color = GlyphDim, fontSize = 8.sp),
        )
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .width(116.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(116.dp)
                .clip(shape)
                .background(GlyphSurface)
                .border(1.dp, GlyphLine, shape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(38.dp)
                        .border(1.dp, GlyphDim, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Plus, tint = GlyphWhite, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                MicroLabel("НОВЫЙ", color = GlyphGray, fontSize = 8)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Создать плейлист",
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphGray),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "ОБЛОЖКА · ОПИСАНИЕ",
            style = MaterialTheme.typography.labelSmall.copy(color = GlyphDim, fontSize = 8.sp),
        )
    }
}

@Composable
private fun LibraryScanCard(status: String, found: Int) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .background(GlyphSurface, shape)
            .border(1.dp, GlyphLine, shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotSpinner(Modifier.size(30.dp), color = GlyphWhite)
        Spacer(Modifier.height(14.dp))
        Text(
            "ОБНОВЛЯЮ",
            style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite, fontSize = 17.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            status.ifBlank { "ПОДГОТОВКА…" },
            style = MaterialTheme.typography.labelSmall.copy(
                color = GlyphGray,
                fontSize = 8.sp,
                letterSpacing = 0.4.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroLabel("НАЙДЕНО", color = GlyphDim)
            Spacer(Modifier.width(8.dp))
            Text(
                found.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = DotoFamily,
                    color = GlyphRed,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                ),
            )
            Spacer(Modifier.width(8.dp))
            MicroLabel("ТРЕКОВ", color = GlyphDim)
        }
    }
}
