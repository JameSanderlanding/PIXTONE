package com.vova.glyph.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.Tab
import com.vova.glyph.ui.components.AlbumCard
import com.vova.glyph.ui.components.ConfirmDialog
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphIconButton
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.ScreenHeader
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphRed

@Composable
fun AlbumsScreen(vm: MainViewModel) {
    val albums by vm.albums.collectAsStateWithLifecycle()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun clearSelection() {
        selectionMode = false
        selectedKeys = emptySet()
    }

    BackHandler(enabled = selectionMode) { clearSelection() }

    Column(Modifier.fillMaxSize()) {
        if (selectionMode) {
            AlbumsSelectionBar(
                selectedCount = selectedKeys.size,
                onCancel = { clearSelection() },
                onConfirm = {
                    if (selectedKeys.isNotEmpty()) showDeleteConfirm = true
                },
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (selectionMode) 8.dp else 0.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!selectionMode) {
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
                    AlbumCard(
                        album = album,
                        onClick = {
                            if (selectionMode) {
                                selectedKeys = if (album.key in selectedKeys) {
                                    selectedKeys - album.key
                                } else {
                                    selectedKeys + album.key
                                }
                            } else {
                                vm.openAlbum(album.key)
                            }
                        },
                        selectionMode = selectionMode,
                        selected = album.key in selectedKeys,
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedKeys = setOf(album.key)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val chosen = albums.filter { it.key in selectedKeys }
        ConfirmDialog(
            title = "Удалить выбранные альбомы?",
            message = if (chosen.size == 1) {
                "Альбом «${chosen.first().name}» будет убран из вкладки «Альбомы». Треки останутся в библиотеке."
            } else {
                "Будет убрано альбомов: ${chosen.size}. Треки останутся в библиотеке."
            },
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                clearSelection()
                vm.deleteAlbums(chosen)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun AlbumsSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIconButton(
            glyph = Glyph.Close,
            onClick = onCancel,
            size = 34.dp,
            iconSize = 17.dp,
            tint = GlyphDim,
        )
        Spacer(Modifier.width(10.dp))
        MicroLabel("ВЫБРАНО: $selectedCount", color = GlyphRed, fontSize = 9)
        Spacer(Modifier.weight(1f))
        GlyphIconButton(
            glyph = Glyph.Check,
            onClick = onConfirm,
            size = 34.dp,
            iconSize = 18.dp,
            tint = if (selectedCount > 0) GlyphRed else GlyphDim,
            bordered = true,
        )
    }
}
