package com.vova.glyph.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.data.ManualOrder
import com.vova.glyph.data.Track
import com.vova.glyph.data.TrackSearch
import com.vova.glyph.data.TrackSort
import com.vova.glyph.data.TrackSorter
import com.vova.glyph.data.TrackSorting
import com.vova.glyph.ui.components.AddToPlaylistSheet
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.GlyphSearchField
import com.vova.glyph.ui.components.Hairline
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.ScreenHeader
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.TrackRow
import com.vova.glyph.ui.components.dropIndicator
import com.vova.glyph.ui.components.rememberReorderState
import com.vova.glyph.ui.components.reorderable
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    val unavailableIds by vm.unavailableIds.collectAsStateWithLifecycle()
    val favoriteOrder by vm.favoriteOrder.collectAsStateWithLifecycle()
    var playlistTarget by remember { mutableStateOf<Track?>(null) }
    var query by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }

    val visible = remember(favorites, query, favoriteOrder) {
        TrackSorter.apply(
            TrackSearch.filter(favorites, query),
            TrackSorting(by = TrackSort.MANUAL),
            favoriteOrder,
        )
    }

    val listState = rememberLazyListState()
    val reorder = rememberReorderState(
        listState,
        remember(visible) { visible.mapTo(HashSet()) { it.id } },
        onStart = { editMode = true },
    ) { fromId, toId ->
        val from = visible.indexOfFirst { it.id == fromId }
        val to = visible.indexOfFirst { it.id == toId }
        if (from >= 0 && to >= 0) vm.reorderFavorites(ManualOrder.move(visible, from, to))
    }

    BackHandler(editMode) { editMode = false }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            index = "04",
            title = "Избранное",
            subtitle = if (query.isBlank()) {
                "${favorites.size} ТРЕКОВ · СОБРАНО ВРУЧНУЮ"
            } else {
                "НАЙДЕНО: ${visible.size}"
            },
            onSettings = vm::openSettings,
        )
        Spacer(Modifier.height(18.dp))
        GlyphSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Поиск по названию или исполнителю",
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphButton(
                    text = "СЛУШАТЬ ВСЁ",
                    onClick = { vm.playTracks(visible, 0, openPlayer = true) },
                    enabled = visible.isNotEmpty(),
                    leading = Glyph.Play,
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
                    Spacer(Modifier.size(7.dp))
                    MicroLabel(
                        if (favorites.any { it.isOnline }) "ЛОКАЛЬНО И ОНЛАЙН" else "НА УСТРОЙСТВЕ",
                        color = GlyphDim,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(14) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (favorites.isEmpty()) GlyphLine
                                else if (index < (favorites.size.coerceAtMost(14))) GlyphWhite
                                else GlyphLine
                            )
                    )
                }
            }
        }
        item { Hairline(Modifier.padding(horizontal = 20.dp)) }
        stickyHeader(key = "favorites") {
            SectionHeader(
                number = "01",
                title = when {
                    editMode -> "ПЕРЕТАЩИТЕ ТРЕК"
                    query.isBlank() -> "ВАША КОЛЛЕКЦИЯ"
                    else -> "РЕЗУЛЬТАТЫ: ${visible.size}"
                },
                modifier = Modifier.background(GlyphBlack),
                action = {
                    if (editMode) {
                        Row(
                            Modifier.clickable { editMode = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MicroLabel("ГОТОВО", color = GlyphWhite)
                        }
                    } else {
                        Text(
                            "${visible.size}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = GlyphGray,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                },
            )
        }
        if (favorites.isEmpty()) {
            item {
                EmptyState(
                    glyph = Glyph.Heart,
                    title = "Здесь пока тихо",
                    body = "Отмечайте треки сердцем в библиотеке.",
                    action = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GlyphButton(
                                text = "ОТКРЫТЬ БИБЛИОТЕКУ",
                                onClick = { vm.selectTab(com.vova.glyph.Tab.LIBRARY) },
                                filled = false,
                                leading = Glyph.Library,
                            )
                        }
                    },
                )
            }
        } else if (visible.isEmpty()) {
            item {
                EmptyState(
                    glyph = Glyph.Search,
                    title = "Ничего не найдено",
                    body = "Попробуйте другое название, исполнителя или альбом.",
                    action = {
                        GlyphButton(
                            text = "СБРОСИТЬ",
                            onClick = { query = "" },
                            filled = false,
                            leading = Glyph.Close,
                        )
                    },
                )
            }
        } else {
            itemsIndexed(
                items = visible,
                key = { _, track -> track.id },
                contentType = { _, _ -> "track" },
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = playerState.isPlaying && currentTrack?.id == track.id,
                    unavailable = track.id in unavailableIds,
                    onClick = { if (!editMode) vm.playTracks(visible, index) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAddToPlaylist = { playlistTarget = track },
                    onEditInfo = { vm.openTrackEditor(track) },
                    onUpdateCover = if (track.isOnline) null else ({ vm.updateTrackCover(track) }),
                    onDelete = { vm.deleteTrack(track) },
                    modifier = if (query.isBlank()) {
                        Modifier
                            .animateItem()
                            .reorderable(reorder, track.id)
                            .dropIndicator(reorder, track.id)
                    } else {
                        Modifier.animateItem()
                    },
                )
            }
        }
        }
    }

    playlistTarget?.let { track ->
        AddToPlaylistSheet(
            track = track,
            playlists = playlists,
            onDismiss = { playlistTarget = null },
            onSelect = { playlist ->
                vm.addTrackToPlaylist(playlist.id, track)
                playlistTarget = null
            },
        )
    }
}
