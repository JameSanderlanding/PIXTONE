package com.vova.glyph.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.Tab
import com.vova.glyph.data.Track
import com.vova.glyph.i18n.tr
import com.vova.glyph.ui.components.AddToPlaylistSheet
import com.vova.glyph.ui.components.ConfirmDialog
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphCover
import com.vova.glyph.ui.components.GlyphIconButton
import com.vova.glyph.ui.components.Hairline
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.TrackRow
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun AlbumDetailScreen(vm: MainViewModel) {
    val album by vm.openAlbum.collectAsStateWithLifecycle()
    val tracks by vm.albumTracks.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var playlistTarget by remember { mutableStateOf<Track?>(null) }

    val currentAlbum = album ?: return

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIconButton(
                    glyph = Glyph.Back,
                    onClick = vm::closeAlbum,
                    size = 36.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(4.dp))
                MicroLabel(
                    if (currentAlbum.unknown) "БЕЗ АЛЬБОМА" else "АЛЬБОМ",
                    color = GlyphDim,
                )
                Spacer(Modifier.weight(1f))
                GlyphIconButton(
                    glyph = Glyph.Trash,
                    onClick = { showDeleteConfirm = true },
                    size = 32.dp,
                    iconSize = 16.dp,
                    tint = GlyphGray,
                    description = tr("Удалить альбом"),
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GlyphCover(
                    artwork = currentAlbum.artwork,
                    seed = currentAlbum.name + currentAlbum.artist,
                    modifier = Modifier.size(146.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    MicroLabel("АЛЬБОМ", color = GlyphRed)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentAlbum.name,
                        style = MaterialTheme.typography.headlineLarge.copy(color = GlyphWhite),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (currentAlbum.artist.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            currentAlbum.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    MicroLabel(
                        "${currentAlbum.trackCount} ТРЕКОВ",
                        color = GlyphDim,
                        fontSize = 8,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                GlyphButton(
                    text = "СЛУШАТЬ",
                    onClick = { vm.playTracks(tracks, 0, openPlayer = true) },
                    enabled = tracks.isNotEmpty(),
                    leading = Glyph.Play,
                    modifier = Modifier.weight(1f),
                )
                GlyphButton(
                    text = "К БИБЛИОТЕКЕ",
                    onClick = {
                        vm.closeAlbum()
                        vm.selectTab(Tab.LIBRARY)
                    },
                    filled = false,
                    leading = Glyph.Library,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { Hairline(Modifier.padding(horizontal = 20.dp)) }
        item { SectionHeader("01", "ТРЕКИ") }
        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    glyph = Glyph.Album,
                    title = "Альбом пуст",
                    body = "Треков этого альбома больше нет в библиотеке.",
                )
            }
        } else {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id },
                contentType = { _, _ -> "track" },
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = playerState.isPlaying && currentTrack?.id == track.id,
                    onClick = { vm.playTracks(tracks, index) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAddToPlaylist = { playlistTarget = track },
                )
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Удалить альбом?",
            message = "Альбом «${currentAlbum.name}» будет убран из приложения. Файлы на устройстве останутся.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                vm.deleteAlbum(currentAlbum)
            },
            onDismiss = { showDeleteConfirm = false },
        )
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
