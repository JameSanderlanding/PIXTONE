package com.vova.glyph.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.Tab
import com.vova.glyph.ui.components.AlbumCard
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.ScreenHeader

@Composable
fun AlbumsScreen(vm: MainViewModel) {
    val albums by vm.albums.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                ScreenHeader(
                    index = "02",
                    title = "Альбомы",
                    subtitle = "АЛЬБОМОВ: ${albums.size}",
                    onSettings = vm::openSettings,
                )
                Spacer(Modifier.height(18.dp))
            }
        }
        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    glyph = Glyph.Album,
                    title = "Альбомов пока нет",
                    body = "Добавьте папку с музыкой — альбомы соберутся из тегов файлов.",
                    action = {
                        GlyphButton(
                            text = "ОТКРЫТЬ БИБЛИОТЕКУ",
                            onClick = { vm.selectTab(Tab.LIBRARY) },
                            filled = false,
                            leading = Glyph.Library,
                        )
                    },
                )
            }
        } else {
            items(
                items = albums,
                key = { it.key },
                contentType = { "album" },
            ) { album ->
                AlbumCard(album = album, onClick = { vm.openAlbum(album.key) })
            }
        }
    }
}
