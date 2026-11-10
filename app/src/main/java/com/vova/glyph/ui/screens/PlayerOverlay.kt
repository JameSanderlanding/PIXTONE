package com.vova.glyph.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.vova.glyph.MainViewModel
import com.vova.glyph.data.Track
import com.vova.glyph.ui.components.AddToPlaylistSheet
import com.vova.glyph.ui.components.DotGrid
import com.vova.glyph.ui.components.EmptyState
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphCover
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.LyricsDialog
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.SegmentedProgress
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.components.formatDuration
import com.vova.glyph.ui.theme.DotoFamily
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun PlayerOverlay(vm: MainViewModel) {
    val track by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var showPlaylists by remember { mutableStateOf(false) }
    var showSound by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsCompletionBase by remember { mutableStateOf(0L) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val target = track
            if (target != null) vm.setCustomCover(target, uri)
        }
    }

    Box(Modifier.fillMaxSize().background(GlyphBlack)) {
        DotGrid(
            Modifier.fillMaxSize(),
            color = GlyphWhite.copy(alpha = 0.035f),
            step = 26.dp,
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 26.dp)
        ) {
            PlayerHeader(
                track = track,
                isPlaying = playerState.isPlaying,
                onClose = vm::closePlayer,
            )
            Spacer(Modifier.height(18.dp))

            val current = track
            if (current == null) {
                EmptyState(
                    glyph = Glyph.Play,
                    title = "Тишина",
                    body = "Выберите трек в библиотеке.",
                    action = {
                        GlyphButton(
                            text = "К БИБЛИОТЕКЕ",
                            onClick = {
                                vm.closePlayer()
                                vm.selectTab(com.vova.glyph.Tab.LIBRARY)
                            },
                            leading = Glyph.Library,
                        )
                    },
                )
                return@Column
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { vm.togglePlayPause() }
            ) {
                GlyphCover(
                    artwork = current.artwork,
                    seed = current.title + current.effectiveArtist,
                    modifier = Modifier.fillMaxSize(),
                    corner = 3.dp,
                )
                GlyphCorners(Modifier.fillMaxSize())
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(6.dp)
                        .background(GlyphRed, CircleShape)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(GlyphBlack.copy(alpha = 0.78f), RoundedCornerShape(2.dp))
                        .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    MicroLabel("ЛОКАЛЬНЫЙ ФАЙЛ", color = GlyphWhite, fontSize = 7)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    MicroLabel(
                        if (playerState.isPlaying) "СЕЙЧАС ИГРАЕТ" else "ПАУЗА",
                        color = if (playerState.isPlaying) GlyphRed else GlyphDim,
                        fontSize = 8,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        current.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = GlyphWhite,
                            fontSize = 23.sp,
                            lineHeight = 27.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        current.displayArtist,
                        style = MaterialTheme.typography.bodyMedium.copy(color = GlyphGray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (current.album.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            current.album,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlyphDim,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerAction(
                        glyph = Glyph.Image,
                        tint = GlyphWhite,
                        label = "ОБЛОЖКА",
                        onClick = { coverPicker.launch("image/*") },
                    )
                    PlayerAction(
                        glyph = if (current.favorite) Glyph.HeartFilled else Glyph.Heart,
                        tint = if (current.favorite) GlyphRed else GlyphWhite,
                        label = "ИЗБРАННОЕ",
                        onClick = { vm.toggleFavorite(current) },
                    )
                    PlayerAction(
                        glyph = Glyph.Text,
                        tint = GlyphWhite,
                        label = "ТЕКСТ",
                        onClick = {
                            lyricsCompletionBase = playerState.completionCount
                            vm.fetchLyrics(current)
                            showLyrics = true
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            PlayerProgressBlock(vm = vm, trackDurationMs = current.durationMs)

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerControl(
                    glyph = Glyph.Shuffle,
                    tint = if (playerState.shuffle) GlyphRed else GlyphGray,
                    size = 40.dp,
                ) { vm.toggleShuffle() }
                PlayerControl(
                    glyph = Glyph.Previous,
                    tint = GlyphWhite,
                    size = 46.dp,
                    iconSize = 22.dp,
                ) { vm.previous() }
                Box(
                    Modifier
                        .size(76.dp)
                        .background(GlyphWhite, CircleShape)
                        .clickable { vm.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(
                        glyph = if (playerState.isPlaying) Glyph.Pause else Glyph.Play,
                        tint = GlyphBlack,
                        modifier = Modifier.size(28.dp),
                        stroke = 2.dp,
                    )
                }
                PlayerControl(
                    glyph = Glyph.Next,
                    tint = GlyphWhite,
                    size = 46.dp,
                    iconSize = 22.dp,
                ) { vm.next() }
                PlayerControl(
                    glyph = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                        Glyph.RepeatOne
                    } else {
                        Glyph.Repeat
                    },
                    tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) {
                        GlyphRed
                    } else {
                        GlyphGray
                    },
                    size = 40.dp,
                ) { vm.cycleRepeat() }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GlyphSurface, RoundedCornerShape(2.dp))
                    .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.Folder, tint = GlyphDim, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    "ОЧЕРЕДЬ · ${if (playerState.hasNext) "ЕСТЬ СЛЕДУЮЩИЙ" else "ПОСЛЕДНИЙ ТРЕК"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GlyphDim,
                        fontSize = 7.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (playerState.shuffle) "ПЕРЕМЕШКА ВКЛ" else "ПЕРЕМЕШКА ВЫКЛ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (playerState.shuffle) GlyphRed else GlyphDim,
                            fontSize = 7.sp,
                        ),
                        maxLines = 1,
                    )
                    if (playerState.repeatMode != Player.REPEAT_MODE_OFF) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                                "ПОВТОР ОДНОГО ТРЕКА"
                            } else {
                                "ПОВТОР ВСЕГО СПИСКА"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlyphRed,
                                fontSize = 7.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            PlayerSoundSection(
                vm = vm,
                expanded = showSound,
                onToggle = { showSound = !showSound },
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GlyphButton(
                    text = "В ПЛЕЙЛИСТ",
                    onClick = { showPlaylists = true },
                    filled = false,
                    leading = Glyph.Plus,
                    modifier = Modifier.weight(1f),
                )
                GlyphButton(
                    text = "К БИБЛИОТЕКЕ",
                    onClick = {
                        vm.closePlayer()
                        vm.selectTab(com.vova.glyph.Tab.LIBRARY)
                    },
                    filled = false,
                    leading = Glyph.Library,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }

    val current = track
    if (showPlaylists && current != null) {
        AddToPlaylistSheet(
            track = current,
            playlists = playlists,
            onDismiss = { showPlaylists = false },
            onSelect = { playlist ->
                vm.addTrackToPlaylist(playlist.id, current)
                showPlaylists = false
            },
        )
    }

    if (showLyrics && current != null) {
        PlayerLyricsDialog(
            vm = vm,
            track = current,
            completionCount = playerState.completionCount,
            completionBase = lyricsCompletionBase,
            onClose = {
                showLyrics = false
                vm.clearLyrics()
            },
        )
    }
}

@Composable
private fun PlayerProgressBlock(vm: MainViewModel, trackDurationMs: Long) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val duration = if (progress.durationMs > 0) progress.durationMs else trackDurationMs
    val fraction = if (duration > 0) {
        (progress.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }

    SegmentedProgress(
        progress = fraction,
        onSeek = { value -> if (duration > 0) vm.seekTo((value * duration).toLong()) },
        segments = 48,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatDuration(progress.positionMs),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = DotoFamily,
                fontSize = 10.sp,
                color = GlyphGray,
                letterSpacing = 0.5.sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        Text(
            formatDuration(duration),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = DotoFamily,
                fontSize = 10.sp,
                color = GlyphDim,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

@Composable
private fun PlayerLyricsDialog(
    vm: MainViewModel,
    track: Track,
    completionCount: Long,
    completionBase: Long,
    onClose: () -> Unit,
) {
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val syncedLyrics by vm.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsLoading by vm.lyricsLoading.collectAsStateWithLifecycle()
    val lyricQuotes by vm.lyricQuotes.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()

    var requestedTrackId by remember { mutableLongStateOf(track.id) }
    LaunchedEffect(track.id) {
        if (requestedTrackId != track.id) {
            requestedTrackId = track.id
            vm.fetchLyrics(track)
        }
    }
    LaunchedEffect(completionCount) {
        if (completionCount > completionBase) onClose()
    }

    LyricsDialog(
        track = track,
        lyrics = lyrics,
        syncedLines = syncedLyrics,
        quotes = lyricQuotes,
        positionMs = progress.positionMs,
        durationMs = progress.durationMs,
        onSeek = vm::seekToLine,
        loading = lyricsLoading,
        onRetry = { vm.fetchLyrics(track) },
        onSaveQuote = { lines -> vm.saveLyricQuote(track.id, lines) },
        onDeleteQuote = { quoteId -> vm.deleteLyricQuote(track.id, quoteId) },
        onRemoveHighlights = { indices -> vm.removeLyricHighlights(track.id, indices) },
        onDismiss = onClose,
    )
}

@Composable
private fun PlayerHeader(track: Track?, isPlaying: Boolean, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(
                Glyph.Back,
                tint = GlyphWhite,
                modifier = Modifier.size(21.dp).rotate(-90f),
                stroke = 1.8.dp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Column {
            MicroLabel("PIXTONE / ПЛЕЕР", color = GlyphDim, fontSize = 8)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(if (isPlaying) GlyphRed else GlyphDim, CircleShape)
                )
                Spacer(Modifier.width(5.dp))
                MicroLabel(
                    when {
                        track == null -> "НЕТ ТРЕКА"
                        isPlaying -> "ВОСПРОИЗВЕДЕНИЕ"
                        else -> "ПАУЗА"
                    },
                    color = GlyphGray,
                    fontSize = 8,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        MicroLabel("СЕЙЧАС", color = GlyphDim, fontSize = 8)
    }
}

@Composable
private fun PlayerControl(
    glyph: Glyph,
    tint: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun PlayerAction(
    glyph: Glyph,
    tint: androidx.compose.ui.graphics.Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(38.dp)
                .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(5.dp))
        MicroLabel(label, color = GlyphDim, fontSize = 6)
    }
}

@Composable
private fun GlyphCorners(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val u = 14.dp.toPx()
        val len = 18.dp.toPx()
        val color = GlyphWhite.copy(alpha = 0.55f)
        val w = 1.dp.toPx()
        drawLine(color, androidx.compose.ui.geometry.Offset(u, u), androidx.compose.ui.geometry.Offset(u + len, u), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(u, u), androidx.compose.ui.geometry.Offset(u, u + len), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width - u, u), androidx.compose.ui.geometry.Offset(size.width - u - len, u), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width - u, u), androidx.compose.ui.geometry.Offset(size.width - u, u + len), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(u, size.height - u), androidx.compose.ui.geometry.Offset(u + len, size.height - u), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(u, size.height - u), androidx.compose.ui.geometry.Offset(u, size.height - u - len), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width - u, size.height - u), androidx.compose.ui.geometry.Offset(size.width - u - len, size.height - u), w)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width - u, size.height - u), androidx.compose.ui.geometry.Offset(size.width - u, size.height - u - len), w)
    }
}
