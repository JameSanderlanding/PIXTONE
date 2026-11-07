package com.vova.glyph.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.data.Track
import com.vova.glyph.ui.components.ConfirmDialog
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphCover
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.GlyphIconButton
import com.vova.glyph.ui.components.Hairline
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.TrackRow
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun PlaylistDetailScreen(vm: MainViewModel) {
    val playlist by vm.openPlaylist.collectAsStateWithLifecycle()
    val tracks by vm.playlistTracks.collectAsStateWithLifecycle()
    val allTracks by vm.tracks.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    var showAddTracks by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentPlaylist = playlist ?: return

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIconButton(
                glyph = Glyph.Back,
                onClick = vm::closePlaylist,
                size = 36.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(4.dp))
            MicroLabel("ПЛЕЙЛИСТ / ${currentPlaylist.trackCount.toString().padStart(2, '0')}", color = GlyphDim)
            Spacer(Modifier.weight(1f))
            if (tracks.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(4.dp).background(com.vova.glyph.ui.theme.GlyphRed, androidx.compose.foundation.shape.CircleShape))
                    Spacer(Modifier.width(6.dp))
                    MicroLabel("ЛОКАЛЬНО", color = GlyphDim)
                }
            }
            Spacer(Modifier.width(6.dp))
            GlyphIconButton(
                glyph = Glyph.Trash,
                onClick = { showDeleteConfirm = true },
                size = 32.dp,
                iconSize = 16.dp,
                tint = GlyphGray,
                description = com.vova.glyph.i18n.tr("Удалить плейлист"),
            )
        }

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GlyphCover(
                    artwork = currentPlaylist.cover,
                    seed = currentPlaylist.name,
                    modifier = Modifier.size(146.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    MicroLabel("СОЗДАНО В PIXTONE", color = com.vova.glyph.ui.theme.GlyphRed)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentPlaylist.name,
                        style = MaterialTheme.typography.headlineLarge.copy(color = GlyphWhite),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "${currentPlaylist.trackCount} ТРЕКОВ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GlyphDim,
                            fontSize = 8.sp,
                        ),
                    )
                }
            }
        }
        if (currentPlaylist.description.isNotBlank()) {
            item {
                Text(
                    currentPlaylist.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
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
                    text = "ДОБАВИТЬ",
                    onClick = { showAddTracks = true },
                    filled = false,
                    leading = Glyph.Plus,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { Hairline(Modifier.padding(horizontal = 20.dp)) }
        stickyHeader(key = "tracks") {
            SectionHeader(
                number = "01",
                title = "ТРЕКИ В ПЛЕЙЛИСТЕ",
                modifier = Modifier.background(GlyphBlack),
            )
        }
        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    glyph = Glyph.Plus,
                    title = "Плейлист пуст",
                    body = "Добавьте треки из библиотеки — название, описание и обложка уже сохранены.",
                    action = {
                        GlyphButton(
                            text = "ДОБАВИТЬ ТРЕКИ",
                            onClick = { showAddTracks = true },
                            leading = Glyph.Plus,
                        )
                    },
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
                    onRemove = { vm.removeTrackFromPlaylist(currentPlaylist.id, track) },
                    onDelete = { vm.deleteTrack(track) },
                )
            }
        }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Удалить плейлист?",
            message = "«${currentPlaylist.name}» будет удалён. Треки останутся в библиотеке.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                vm.deletePlaylist(currentPlaylist)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showAddTracks) {
        AddTracksSheet(
            allTracks = allTracks,
            existing = tracks,
            onDismiss = { showAddTracks = false },
            onAdd = { track -> vm.addTrackToPlaylist(currentPlaylist.id, track) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTracksSheet(
    allTracks: List<Track>,
    existing: List<Track>,
    onDismiss: () -> Unit,
    onAdd: (Track) -> Unit,
) {
    val existingIds = remember(existing) { existing.map { it.id }.toSet() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlyphSurface,
        scrimColor = GlyphBlack.copy(alpha = 0.78f),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .background(GlyphLine, RoundedCornerShape(2.dp))
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            MicroLabel("ДОБАВИТЬ ТРЕКИ", color = com.vova.glyph.ui.theme.GlyphRed)
            Spacer(Modifier.height(8.dp))
            Text(
                "БИБЛИОТЕКА",
                style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite),
            )
            Spacer(Modifier.height(12.dp))
            if (allTracks.isEmpty()) {
                Text(
                    "В библиотеке нет треков. Сначала импортируйте папку или скачайте музыку.",
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(
                        items = allTracks,
                        key = { it.id },
                        contentType = { "track" },
                    ) { track ->
                        val added = track.id in existingIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !added) { onAdd(track) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlyphCover(
                                artwork = track.artwork,
                                seed = track.title + track.artist,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (added) GlyphDim else GlyphWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    track.displayArtist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (added) {
                                GlyphIcon(Glyph.Check, tint = com.vova.glyph.ui.theme.GlyphRed, modifier = Modifier.size(16.dp))
                            } else {
                                GlyphIcon(Glyph.Plus, tint = GlyphGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
