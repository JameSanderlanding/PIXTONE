package com.vova.glyph.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.OnlineStatus
import com.vova.glyph.data.OnlineTrack
import com.vova.glyph.data.Track
import com.vova.glyph.ui.components.AddToPlaylistSheet
import com.vova.glyph.ui.components.DotSpinner
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphCover
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.GlyphSearchField
import com.vova.glyph.ui.components.Hairline
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.PlayingBars
import com.vova.glyph.ui.components.ScreenHeader
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.formatDuration
import com.vova.glyph.ui.theme.DotoFamily
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun OnlineScreen(vm: MainViewModel) {
    val state by vm.online.collectAsStateWithLifecycle()
    val history by vm.onlineHistory.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    var playlistTarget by remember { mutableStateOf<OnlineTrack?>(null) }
    var historyPlaylistTarget by remember { mutableStateOf<Track?>(null) }

    val saved = remember(tracks) {
        tracks.filter { it.source == Track.SOURCE_JIOSAAVN }
            .mapNotNull { track -> track.sourceId?.let { it to track } }
            .toMap()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            index = "01",
            title = "Поиск",
            subtitle = "ОНЛАЙН-ТРЕКИ · ЗВУК ИЗ ИНТЕРНЕТА",
            onSettings = vm::openSettings,
        )
        Spacer(Modifier.height(16.dp))
        GlyphSearchField(
            query = state.query,
            onQueryChange = vm::updateOnlineQuery,
            placeholder = "Поиск треков и исполнителей",
            onSearch = vm::retryOnlineSearch,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))
        Hairline(Modifier.padding(horizontal = 20.dp))

        when {
            state.status == OnlineStatus.FAILED -> EmptyState(
                glyph = Glyph.Online,
                title = "КАТАЛОГ НЕ ОТВЕЧАЕТ",
                body = state.message ?: "Проверьте интернет и попробуйте снова.",
                action = {
                    GlyphButton(
                        text = "ПОВТОРИТЬ",
                        onClick = vm::retryOnlineSearch,
                        filled = false,
                        leading = Glyph.Repeat,
                    )
                },
            )

            state.status == OnlineStatus.EMPTY -> EmptyState(
                glyph = Glyph.Search,
                title = "Ничего не найдено",
                body = "Попробуйте другое название, исполнителя или альбом.",
            )

            state.status == OnlineStatus.IDLE -> {
                if (history.isEmpty()) {
                    EmptyState(
                        glyph = Glyph.Online,
                        title = "Поиск музыки",
                        body = "Найдите трек по названию или исполнителю. Прослушанные онлайн-треки появятся в истории.",
                    )
                } else {
                    HistoryList(
                        history = history,
                        currentTrackId = currentTrack?.id,
                        isPlaying = playerState.isPlaying,
                        onPlay = { index -> vm.playOnlineHistory(history, index) },
                        onFavorite = vm::toggleFavorite,
                        onEdit = vm::openTrackEditor,
                        onAddToPlaylist = { historyPlaylistTarget = it },
                        onRemove = vm::removeOnlineFromHistory,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            state.results.isEmpty() -> Box(
                Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                DotSpinner(color = GlyphGray)
            }

            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            ) {
                stickyHeader(key = "online-results") {
                    SectionHeader(
                        number = "01",
                        title = "РЕЗУЛЬТАТЫ",
                        modifier = Modifier.background(GlyphBlack),
                        action = {
                            Text(
                                "${state.results.size}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = GlyphGray,
                                    fontSize = 10.sp,
                                ),
                            )
                        },
                    )
                }
                itemsIndexed(
                    items = state.results,
                    key = { _, track -> track.id },
                    contentType = { _, _ -> "online" },
                ) { _, track ->
                    OnlineTrackRow(
                        track = track,
                        saved = saved[track.id],
                        isCurrent = currentTrack?.sourceId == track.id,
                        isPlaying = playerState.isPlaying && currentTrack?.sourceId == track.id,
                        onClick = { vm.playOnlineResult(track) },
                        onFavorite = { vm.toggleOnlineFavorite(track) },
                        onAddToPlaylist = { playlistTarget = track },
                        onQueue = { vm.addOnlineToQueue(track) },
                    )
                }
            }
        }
    }

    playlistTarget?.let { track ->
        AddToPlaylistSheet(
            track = track.toTrack(),
            playlists = playlists,
            onDismiss = { playlistTarget = null },
            onSelect = { playlist ->
                vm.addOnlineToPlaylist(playlist.id, track)
                playlistTarget = null
            },
        )
    }

    historyPlaylistTarget?.let { track ->
        AddToPlaylistSheet(
            track = track,
            playlists = playlists,
            onDismiss = { historyPlaylistTarget = null },
            onSelect = { playlist ->
                vm.addTrackToPlaylist(playlist.id, track)
                historyPlaylistTarget = null
            },
        )
    }
}

@Composable
private fun HistoryList(
    history: List<Track>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onPlay: (Int) -> Unit,
    onFavorite: (Track) -> Unit,
    onEdit: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        stickyHeader(key = "online-history") {
            SectionHeader(
                number = "01",
                title = "ИСТОРИЯ",
                modifier = Modifier.background(GlyphBlack),
                action = {
                    Text(
                        "${history.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GlyphGray,
                            fontSize = 10.sp,
                        ),
                    )
                },
            )
        }
        itemsIndexed(
            items = history,
            key = { _, track -> track.id },
            contentType = { _, _ -> "history" },
        ) { index, track ->
            HistoryTrackRow(
                track = track,
                isCurrent = currentTrackId == track.id,
                isPlaying = isPlaying && currentTrackId == track.id,
                onClick = { onPlay(index) },
                onFavorite = { onFavorite(track) },
                onEdit = { onEdit(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onRemove = { onRemove(track) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                GlyphCover(
                    artwork = track.artwork,
                    seed = track.effectiveTitle + track.effectiveArtist,
                    modifier = Modifier.size(50.dp),
                )
                if (isCurrent) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(5.dp)
                            .background(GlyphRed, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        PlayingBars(
                            playing = isPlaying,
                            modifier = Modifier.size(12.dp),
                            color = GlyphWhite,
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        track.effectiveTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isCurrent) GlyphWhite else GlyphWhite.copy(alpha = 0.94f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.displayArtist,
                        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(7.dp))
                    OnlineBadge()
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatDuration(track.durationMs),
                style = TextStyle(
                    fontFamily = DotoFamily,
                    fontSize = 10.sp,
                    color = GlyphDim,
                    letterSpacing = 0.4.sp,
                ),
            )
            Box(
                Modifier
                    .size(36.dp)
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(
                    glyph = if (track.favorite) Glyph.HeartFilled else Glyph.Heart,
                    tint = if (track.favorite) GlyphRed else GlyphGray,
                    modifier = Modifier.size(17.dp),
                    stroke = 1.5.dp,
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.More, tint = GlyphDim, modifier = Modifier.size(16.dp))
                TrackActionsMenu(
                    expanded = menuOpen,
                    favorite = track.favorite,
                    onDismiss = { menuOpen = false },
                    onFavorite = onFavorite,
                    onEdit = onEdit,
                    onAddToPlaylist = onAddToPlaylist,
                    onRemove = onRemove,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 82.dp)
                .height(1.dp)
                .background(GlyphLine.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun TrackActionsMenu(
    expanded: Boolean,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(if (favorite) "Убрать из избранного" else "В избранное") },
            onClick = {
                onDismiss()
                onFavorite()
            },
        )
        DropdownMenuItem(
            text = { Text("Изменить трек") },
            onClick = {
                onDismiss()
                onEdit()
            },
        )
        DropdownMenuItem(
            text = { Text("Добавить в плейлист") },
            onClick = {
                onDismiss()
                onAddToPlaylist()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    "Удалить из истории",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GlyphRed),
                )
            },
            onClick = {
                onDismiss()
                onRemove()
            },
        )
    }
}

@Composable
private fun OnlineBadge() {
    MicroLabel("ОНЛАЙН", color = GlyphRed, fontSize = 6)
}

@Composable
private fun OnlineTrackRow(
    track: OnlineTrack,
    saved: Track?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onQueue: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val favorite = saved?.favorite == true
    val subtitle = listOf(track.artist, track.album)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                GlyphCover(
                    artwork = track.coverUrl,
                    seed = track.title + track.artist,
                    modifier = Modifier.size(50.dp),
                )
                if (isCurrent) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(5.dp)
                            .background(GlyphRed, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        PlayingBars(
                            playing = isPlaying,
                            modifier = Modifier.size(12.dp),
                            color = GlyphWhite,
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isCurrent) GlyphWhite else GlyphWhite.copy(alpha = 0.94f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatDuration(track.durationMs),
                style = TextStyle(
                    fontFamily = DotoFamily,
                    fontSize = 10.sp,
                    color = GlyphDim,
                    letterSpacing = 0.4.sp,
                ),
            )
            Box(
                Modifier
                    .size(36.dp)
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(
                    glyph = if (favorite) Glyph.HeartFilled else Glyph.Heart,
                    tint = if (favorite) GlyphRed else GlyphGray,
                    modifier = Modifier.size(17.dp),
                    stroke = 1.5.dp,
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.More, tint = GlyphDim, modifier = Modifier.size(16.dp))
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (favorite) "Убрать из избранного" else "В избранное")
                        },
                        onClick = {
                            menuOpen = false
                            onFavorite()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Добавить в плейлист") },
                        onClick = {
                            menuOpen = false
                            onAddToPlaylist()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("В очередь") },
                        enabled = track.streamUrl != null,
                        onClick = {
                            menuOpen = false
                            onQueue()
                        },
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 82.dp)
                .height(1.dp)
                .background(GlyphLine.copy(alpha = 0.55f))
        )
    }
}
