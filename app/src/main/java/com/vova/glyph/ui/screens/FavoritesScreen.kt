package com.vova.glyph.ui.screens

import androidx.compose.foundation.background
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
import com.vova.glyph.data.Track
import com.vova.glyph.ui.components.AddToPlaylistSheet
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.Hairline
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.ScreenHeader
import com.vova.glyph.ui.components.SectionHeader
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.TrackRow
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
    var playlistTarget by remember { mutableStateOf<Track?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            index = "03",
            title = "Избранное",
            subtitle = "${favorites.size} ТРЕКОВ · СОБРАНО ВРУЧНУЮ",
            onSettings = vm::openSettings,
        )

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphButton(
                    text = "СЛУШАТЬ ВСЁ",
                    onClick = { vm.playTracks(favorites, 0, openPlayer = true) },
                    enabled = favorites.isNotEmpty(),
                    leading = Glyph.Play,
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
                    Spacer(Modifier.size(7.dp))
                    MicroLabel("НА УСТРОЙСТВЕ", color = GlyphDim)
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
                title = "ВАША КОЛЛЕКЦИЯ",
                modifier = Modifier.background(GlyphBlack),
                action = {
                    Text(
                        "${favorites.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GlyphGray,
                            fontSize = 10.sp,
                        ),
                    )
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
        } else {
            itemsIndexed(
                items = favorites,
                key = { _, track -> track.id },
                contentType = { _, _ -> "track" },
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = playerState.isPlaying && currentTrack?.id == track.id,
                    onClick = { vm.playTracks(favorites, index) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAddToPlaylist = { playlistTarget = track },
                    onEditArtist = { vm.openArtistEditor(track) },
                    onDelete = { vm.deleteTrack(track) },
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
