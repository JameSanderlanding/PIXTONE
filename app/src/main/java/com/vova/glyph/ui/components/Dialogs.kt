package com.vova.glyph.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.vova.glyph.data.LyricLine
import com.vova.glyph.data.Track
import com.vova.glyph.data.activeLineIndex
import com.vova.glyph.data.withGapMarkers
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphSurfaceHigh
import com.vova.glyph.ui.theme.GlyphWhite
import kotlinx.coroutines.delay

@Composable
fun CoverUpdateDialog(
    onFindMissing: () -> Unit,
    onUpdateAll: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                MicroLabel("ОБЛОЖКИ", color = GlyphRed)
                Spacer(Modifier.height(14.dp))
                CoverOptionCard(
                    title = "НЕДОСТАЮЩИЕ ОБЛОЖКИ",
                    body = "Найти обложки для треков, где PIXTONE нарисовал точки.",
                    onClick = onFindMissing,
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "ВСЕ ОБЛОЖКИ",
                    body = "Перекачать обложки для всех треков библиотеки.",
                    onClick = onUpdateAll,
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "ОБЛОЖКИ ПО УМОЛЧАНИЮ",
                    body = "Вернуть обложки, встроенные в файлы песен.",
                    onClick = onResetDefaults,
                    buttonText = "ВЕРНУТЬ ПО УМОЛЧАНИЮ",
                    filled = false,
                )
                Spacer(Modifier.height(16.dp))
                GlyphButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CoverOptionCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    buttonText: String = "ОБНОВИТЬ ОБЛОЖКИ",
    filled: Boolean = true,
) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, GlyphLine, shape)
            .padding(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
        )
        Spacer(Modifier.height(10.dp))
        GlyphButton(
            text = buttonText,
            onClick = onClick,
            filled = filled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    coverUri: Uri?,
    onPickCover: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shape = RoundedCornerShape(2.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .imePadding()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("НОВЫЙ ПЛЕЙЛИСТ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onDismiss,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(shape)
                            .background(GlyphBlack)
                            .border(1.dp, GlyphLine, shape)
                            .clickable(onClick = onPickCover),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (coverUri != null) {
                            AsyncImage(
                                model = coverUri,
                                contentDescription = com.vova.glyph.i18n.tr("Обложка плейлиста"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            GlyphPlaceholder(seed = name, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .background(GlyphWhite, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            GlyphIcon(Glyph.Plus, tint = GlyphBlack, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        MicroLabel("ОБЛОЖКА", color = GlyphGray)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (coverUri == null) "Выберите .PNG или .JPG" else "Обложка выбрана",
                            style = MaterialTheme.typography.bodySmall.copy(color = GlyphWhite),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Необязательно. Без обложки PIXTONE сам нарисует точечный узор.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlyphGray,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                GlyphTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        error = null
                    },
                    placeholder = "Название плейлиста",
                )
                Spacer(Modifier.height(10.dp))
                GlyphTextField(
                    value = description,
                    onValueChange = { description = it.take(240) },
                    placeholder = "Описание — настроение, повод, пара слов",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(color = GlyphRed),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "Создать",
                        onClick = {
                            if (name.isBlank()) {
                                error = "Дайте плейлисту название"
                            } else {
                                onCreate(name.trim(), description.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val shape = RoundedCornerShape(2.dp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                MicroLabel(if (destructive) "ПОДТВЕРДИТЕ ДЕЙСТВИЕ" else "ПОДТВЕРЖДЕНИЕ", color = GlyphRed)
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = confirmText,
                        onClick = onConfirm,
                        filled = !destructive,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun LyricsDialog(
    track: Track,
    lyrics: String?,
    syncedLines: List<LyricLine> = emptyList(),
    positionMs: Long? = null,
    durationMs: Long = 0L,
    loading: Boolean = false,
    onSeek: ((Long) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val text = when {
        loading -> "Ищу текст…"
        lyrics.isNullOrBlank() -> "Повторите попытку"
        else -> lyrics
    }
    val useSynced = syncedLines.isNotEmpty()

    val displayLines = remember(syncedLines, durationMs) {
        if (useSynced) withGapMarkers(syncedLines, durationMs) else emptyList()
    }
    val scrollState = rememberScrollState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxLyricsHeight = (screenHeightDp * 0.5f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("ТЕКСТ ПЕСНИ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    if (useSynced && positionMs != null) {
                        MicroLabel("СИНХРОНИЗАЦИЯ", color = GlyphDim)
                        Spacer(Modifier.width(8.dp))
                    }
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onDismiss,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                )
                Spacer(Modifier.height(18.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxLyricsHeight),
                ) {
                    if (useSynced) {
                        SyncedLyricsView(
                            lines = displayLines,
                            positionMs = positionMs,
                            onSeek = onSeek,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(end = 14.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GlyphWhite,
                                lineHeight = 22.sp,
                            ),
                        )
                        if (scrollState.maxValue > 0) {
                            LyricsScrollbar(
                                state = scrollState,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(4.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                if (!loading && lyrics.isNullOrBlank() && onRetry != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlyphButton(
                            text = "Повторить",
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        )
                        GlyphButton(
                            text = "Закрыть",
                            onClick = onDismiss,
                            filled = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    GlyphButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<LyricLine>,
    positionMs: Long?,
    onSeek: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val activeIndex = remember(lines, positionMs) {
        if (positionMs == null) -1 else activeLineIndex(lines, positionMs)
    }

    var followPlayback by remember { mutableStateOf(true) }
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        if (isDragged) {
            followPlayback = false
        } else {
            delay(6000)
            followPlayback = true
        }
    }

    LaunchedEffect(activeIndex, followPlayback) {
        if (activeIndex >= 0 && followPlayback) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 14.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                val active = index == activeIndex
                val tapModifier = if (onSeek != null) {
                    Modifier.clickable {
                        followPlayback = true
                        onSeek(line.timeMs)
                    }
                } else {
                    Modifier
                }
                if (line.instrumental) {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (active) GlyphRed else GlyphDim,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 0.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(tapModifier)
                            .padding(vertical = 8.dp),
                    )
                } else {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (active) GlyphRed else GlyphWhite.copy(alpha = 0.55f),
                            lineHeight = 22.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(tapModifier)
                            .padding(vertical = 5.dp),
                    )
                }
            }
        }
        if (lines.size > 1) {
            LazyLyricsScrollbar(
                state = listState,
                totalItems = lines.size,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(4.dp),
            )
        }
    }
}

@Composable
private fun LazyLyricsScrollbar(
    state: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    trackColor: Color = GlyphDim.copy(alpha = 0.25f),
    thumbColor: Color = GlyphGray,
) {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo.size
    val firstIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val canScroll = visibleItems in 1 until totalItems

    var dragTarget by remember { mutableIntStateOf(-1) }
    LaunchedEffect(dragTarget) {
        val target = dragTarget
        if (target >= 0) {
            state.scrollToItem(target)
            dragTarget = -1
        }
    }

    Box(
        modifier.pointerInput(totalItems, visibleItems) {
            if (!canScroll) return@pointerInput
            detectVerticalDragGestures { change, _ ->
                change.consume()
                val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                dragTarget = (fraction * (totalItems - 1)).toInt()
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val barWidth = size.width
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            drawRoundRect(color = trackColor, cornerRadius = radius)
            if (!canScroll) return@Canvas

            val thumbFraction = visibleItems.toFloat() / totalItems
            val thumbHeight = (thumbFraction * size.height).coerceAtLeast(barWidth * 3f)
            val maxTop = size.height - thumbHeight
            val scrollFraction = if (totalItems > visibleItems) {
                firstIndex.toFloat() / (totalItems - visibleItems)
            } else {
                0f
            }
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, maxTop * scrollFraction.coerceIn(0f, 1f)),
                size = Size(barWidth, thumbHeight),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun LyricsScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    trackColor: Color = GlyphDim.copy(alpha = 0.25f),
    thumbColor: Color = GlyphGray,
) {
    Box(
        modifier.pointerInput(state) {
            detectVerticalDragGestures { change, dragAmount ->
                change.consume()
                val contentSize = state.viewportSize + state.maxValue
                if (contentSize > 0 && state.viewportSize > 0) {
                    val ratio = contentSize.toFloat() / state.viewportSize
                    state.dispatchRawDelta(dragAmount * ratio)
                }
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val barWidth = size.width
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            drawRoundRect(color = trackColor, cornerRadius = radius)

            val contentSize = state.viewportSize + state.maxValue
            if (contentSize <= 0 || state.viewportSize <= 0 || size.height <= 0f) return@Canvas

            val minThumb = barWidth * 3f
            val thumbHeight = (state.viewportSize.toFloat() / contentSize * size.height)
                .coerceIn(minThumb, size.height)
            val fraction = if (state.maxValue > 0) {
                state.value.toFloat() / state.maxValue
            } else {
                0f
            }
            val top = (size.height - thumbHeight) * fraction
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, top),
                size = Size(barWidth, thumbHeight),
                cornerRadius = radius,
            )
        }
    }
}
