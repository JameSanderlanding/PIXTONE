package com.vova.glyph.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vova.glyph.data.LyricLine
import com.vova.glyph.data.LyricQuote
import com.vova.glyph.data.LyricQuoteLine
import com.vova.glyph.data.Track
import com.vova.glyph.data.plainLyricLines
import com.vova.glyph.data.withGapMarkers
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import kotlinx.coroutines.delay
import androidx.compose.material3.Text as MaterialText

private data class LyricRow(
    val text: String,
    val timeMs: Long,
    val lineIndex: Int,
    val instrumental: Boolean = false,
) {
    val selectable: Boolean get() = !instrumental && lineIndex >= 0 && text.isNotBlank()
}

private fun syncedRows(lines: List<LyricLine>, durationMs: Long): List<LyricRow> {
    val raw = lines.sortedBy { it.timeMs }
    val display = withGapMarkers(raw, durationMs)
    var cursor = 0
    return display.map { line ->
        if (line.instrumental) {
            LyricRow(line.text, line.timeMs, lineIndex = -1, instrumental = true)
        } else {
            while (cursor < raw.size &&
                (raw[cursor].timeMs != line.timeMs || raw[cursor].text != line.text)
            ) {
                cursor++
            }
            val index = if (cursor < raw.size) cursor else -1
            cursor++
            LyricRow(line.text, line.timeMs, index)
        }
    }
}

private fun plainRows(lyrics: String?): List<LyricRow> =
    plainLyricLines(lyrics.orEmpty()).mapIndexed { index, line ->
        LyricRow(line, 0L, index)
    }

private fun isSaved(quotes: List<LyricQuote>, row: LyricRow): Boolean {
    if (row.lineIndex < 0) return false
    return quotes.any { quote ->
        quote.lines.any { it.matches(row.lineIndex, row.text, row.timeMs) }
    }
}

@Composable
fun LyricsDialog(
    track: Track,
    lyrics: String?,
    syncedLines: List<LyricLine> = emptyList(),
    quotes: List<LyricQuote> = emptyList(),
    positionMs: Long? = null,
    durationMs: Long = 0L,
    loading: Boolean = false,
    onSeek: ((Long) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onSaveQuote: ((List<LyricQuoteLine>) -> Unit)? = null,
    onDeleteQuote: ((Long) -> Unit)? = null,
    onRemoveHighlights: ((List<Int>) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val useSynced = syncedLines.isNotEmpty()
    val hasLyrics = useSynced || !lyrics.isNullOrBlank()
    val rows = remember(syncedLines, lyrics, durationMs, useSynced) {
        if (useSynced) syncedRows(syncedLines, durationMs) else plainRows(lyrics)
    }
    val activeRow = remember(rows, useSynced, positionMs) {
        if (!useSynced || positionMs == null) {
            -1
        } else {
            rows.indexOfLast {
                !it.instrumental && it.lineIndex >= 0 && it.timeMs <= positionMs
            }
        }
    }

    var showQuotes by remember(track.id) { mutableStateOf(false) }
    var selected by remember(track.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var notice by remember(track.id) { mutableStateOf<Pair<Long, String>?>(null) }

    LaunchedEffect(rows) { selected = emptySet() }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(1800)
            notice = null
        }
    }

    val selectedRows = selected.sorted().mapNotNull { rows.getOrNull(it) }
    val allSaved = selectedRows.isNotEmpty() && selectedRows.all { isSaved(quotes, it) }
    val quoteActionEnabled = if (allSaved) onRemoveHighlights != null else onSaveQuote != null

    val context = LocalContext.current
    fun showNotice(text: String) {
        notice = System.nanoTime() to text
    }
    fun copySelection() {
        val text = selectedRows.joinToString("\n") { it.text }
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("PIXTONE", text))
        showNotice("СКОПИРОВАНО")
    }

    val maxLyricsHeight = (
        LocalConfiguration.current.screenHeightDp * 0.5f -
            if (selectedRows.isNotEmpty()) 38f else 0f
        ).coerceAtLeast(120f).dp

    Dialog(
        onDismissRequest = {
            when {
                selected.isNotEmpty() -> selected = emptySet()
                showQuotes -> showQuotes = false
                else -> onDismiss()
            }
        },
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
                LyricsHeader(
                    showQuotes = showQuotes,
                    quoteCount = quotes.size,
                    showSync = useSynced && positionMs != null && !showQuotes,
                    onOpenQuotes = {
                        selected = emptySet()
                        showQuotes = true
                    },
                    onDismiss = onDismiss,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    track.effectiveTitle,
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
                    when {
                        showQuotes -> QuotesView(
                            track = track,
                            quotes = quotes,
                            onOpenMoment = onSeek?.let { seek ->
                                { timeMs ->
                                    seek(timeMs)
                                    showQuotes = false
                                }
                            },
                            onDelete = onDeleteQuote,
                            modifier = Modifier.fillMaxSize(),
                        )

                        loading -> CenteredLyricsMessage("Ищу текст…")
                        !hasLyrics -> CenteredLyricsMessage(
                            "Повторите попытку\n" +
                                "Возможное решение: измените название трека или исполнителя, убрав лишние символы"
                        )
                        else -> LyricsLinesView(
                            rows = rows,
                            activeRow = activeRow,
                            quotes = quotes,
                            selected = selected,
                            synced = useSynced,
                            seekable = useSynced && onSeek != null,
                            onToggle = { index ->
                                selected = if (index in selected) selected - index else selected + index
                            },
                            onSeek = onSeek,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                notice?.let { (_, text) ->
                    MicroLabel(text, color = GlyphRed, fontSize = 9)
                    Spacer(Modifier.height(8.dp))
                }

                when {
                    selectedRows.isNotEmpty() -> SelectionPanel(
                        count = selectedRows.size,
                        allSaved = allSaved,
                        quoteActionEnabled = quoteActionEnabled,
                        onCopy = {
                            copySelection()
                            selected = emptySet()
                        },
                        onQuoteAction = {
                            if (allSaved) {
                                onRemoveHighlights?.invoke(selectedRows.map { it.lineIndex })
                                showNotice("УБРАНО ИЗ ЦИТАТ")
                            } else {
                                onSaveQuote?.invoke(
                                    selectedRows.map {
                                        LyricQuoteLine(it.lineIndex, it.text, it.timeMs)
                                    }
                                )
                                showNotice("ДОБАВЛЕНО В ЦИТАТЫ")
                            }
                            selected = emptySet()
                        },
                        onCancel = { selected = emptySet() },
                    )

                    showQuotes -> GlyphButton(
                        text = "К тексту",
                        onClick = { showQuotes = false },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    !loading && !hasLyrics && onRetry != null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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

                    else -> GlyphButton(
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
private fun LyricsHeader(
    showQuotes: Boolean,
    quoteCount: Int,
    showSync: Boolean,
    onOpenQuotes: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MicroLabel(if (showQuotes) "ЦИТАТЫ" else "ТЕКСТ ПЕСНИ", color = GlyphRed)
        Spacer(Modifier.weight(1f))
        if (showSync) {
            MicroLabel("СИНХРОНИЗАЦИЯ", color = GlyphDim)
            Spacer(Modifier.width(8.dp))
        }
        if (!showQuotes) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = onOpenQuotes)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(
                    Glyph.Quote,
                    tint = if (quoteCount == 0) GlyphDim else GlyphWhite,
                    modifier = Modifier.size(12.dp),
                    stroke = 1.4.dp,
                )
                Spacer(Modifier.width(5.dp))
                MicroLabel(
                    "ЦИТАТЫ · $quoteCount",
                    color = if (quoteCount == 0) GlyphDim else GlyphWhite,
                    fontSize = 8,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        GlyphIconButton(
            glyph = Glyph.Close,
            onClick = onDismiss,
            size = 32.dp,
            iconSize = 16.dp,
            tint = GlyphGray,
        )
    }
}

@Composable
private fun CenteredLyricsMessage(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(color = GlyphGray),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LyricsLinesView(
    rows: List<LyricRow>,
    activeRow: Int,
    quotes: List<LyricQuote>,
    selected: Set<Int>,
    synced: Boolean,
    seekable: Boolean,
    onToggle: (Int) -> Unit,
    onSeek: ((Long) -> Unit)?,
) {
    val listState = rememberLazyListState()
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
    LaunchedEffect(selected.isNotEmpty()) {
        if (selected.isNotEmpty()) followPlayback = false
    }
    LaunchedEffect(activeRow, followPlayback, selected.isNotEmpty()) {
        if (activeRow >= 0 && followPlayback && selected.isEmpty()) {
            listState.animateScrollToItem((activeRow - 2).coerceAtLeast(0))
        }
    }

    val selectionActive = selected.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 14.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            itemsIndexed(rows) { index, row ->
                val active = index == activeRow
                val saved = isSaved(quotes, row)
                LyricLineRow(
                    row = row,
                    active = active,
                    highlighted = saved,
                    selected = index in selected,
                    synced = synced,
                    onClick = {
                        when {
                            selectionActive -> onToggle(index)
                            seekable -> {
                                followPlayback = true
                                onSeek?.invoke(row.timeMs)
                            }
                        }
                    },
                    onLongClick = if (row.selectable) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle(index)
                        }
                    } else {
                        null
                    },
                )
            }
        }
        if (rows.size > 1) {
            LazyLyricsScrollbar(
                state = listState,
                totalItems = rows.size,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(4.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LyricLineRow(
    row: LyricRow,
    active: Boolean,
    highlighted: Boolean,
    selected: Boolean,
    synced: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(2.dp)
    val background = when {
        selected -> GlyphWhite.copy(alpha = 0.10f)
        highlighted -> GlyphRed.copy(alpha = 0.07f)
        else -> Color.Transparent
    }
    val bar = when {
        selected -> GlyphWhite
        highlighted -> GlyphRed
        else -> null
    }
    val textColor = when {
        row.instrumental -> if (active) GlyphRed else GlyphDim
        selected -> GlyphWhite
        active -> GlyphRed
        highlighted -> GlyphWhite.copy(alpha = 0.92f)
        else -> GlyphWhite.copy(alpha = if (synced) 0.55f else 1f)
    }
    val vertical = if (synced) {
        if (row.instrumental) 8.dp else 5.dp
    } else {
        0.dp
    }

    MaterialText(
        text = row.text.ifEmpty { " " },
        style = if (row.instrumental) {
            MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
        } else {
            MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                lineHeight = 22.sp,
                fontWeight = if (active || selected) FontWeight.Bold else FontWeight.Normal,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(
                if (bar == null) {
                    Modifier
                } else {
                    Modifier.drawBehind {
                        drawRect(bar, size = Size(2.dp.toPx(), size.height))
                    }
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 10.dp, end = 6.dp, top = vertical, bottom = vertical),
    )
}

@Composable
private fun SelectionPanel(
    count: Int,
    allSaved: Boolean,
    quoteActionEnabled: Boolean,
    onCopy: () -> Unit,
    onQuoteAction: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIconButton(
                glyph = Glyph.Close,
                onClick = onCancel,
                size = 30.dp,
                iconSize = 15.dp,
                tint = GlyphDim,
            )
            Spacer(Modifier.width(8.dp))
            MicroLabel("ВЫБРАНО: $count", color = GlyphRed)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphButton(
                text = "Копировать",
                onClick = onCopy,
                filled = false,
                modifier = Modifier.weight(1f),
            )
            GlyphButton(
                text = if (allSaved) "Убрать" else "В цитаты",
                onClick = onQuoteAction,
                filled = !allSaved,
                enabled = quoteActionEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuotesView(
    track: Track,
    quotes: List<LyricQuote>,
    onOpenMoment: ((Long) -> Unit)?,
    onDelete: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var imageQuote by remember(track.id) { mutableStateOf<LyricQuote?>(null) }

    if (quotes.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MicroLabel("НЕТ СОХРАНЁННЫХ ЦИТАТ", color = GlyphDim, fontSize = 10)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Выделите строку долгим нажатием и добавьте её в цитаты.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GlyphGray,
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()
    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 14.dp),
        ) {
            items(quotes, key = { it.id }) { quote ->
                QuoteItem(
                    quote = quote,
                    onOpenMoment = onOpenMoment,
                    onShareImage = { imageQuote = quote },
                    onDelete = onDelete?.let { remove -> { remove(quote.id) } },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlyphLine.copy(alpha = 0.5f))
                )
            }
        }
        if (quotes.size > 1) {
            LazyLyricsScrollbar(
                state = listState,
                totalItems = quotes.size,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(4.dp),
            )
        }
    }

    imageQuote?.let { quote ->
        QuoteImageDialog(
            track = track,
            quote = quote,
            onDismiss = { imageQuote = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuoteItem(
    quote: LyricQuote,
    onOpenMoment: ((Long) -> Unit)?,
    onShareImage: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val seekable = onOpenMoment != null && quote.timeMs > 0
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember(quote.id) { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(2.dp))
                .combinedClickable(
                    onClick = { if (seekable) onOpenMoment?.invoke(quote.timeMs) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(vertical = 2.dp),
        ) {
            MaterialText(
                text = quote.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GlyphWhite,
                    lineHeight = 22.sp,
                ),
            )
            if (quote.timeMs > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (seekable) {
                        GlyphIcon(
                            Glyph.Play,
                            tint = GlyphRed,
                            modifier = Modifier.size(9.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    MicroLabel(
                        formatDuration(quote.timeMs),
                        color = if (seekable) GlyphRed else GlyphDim,
                        fontSize = 9,
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Box {
            GlyphIconButton(
                glyph = Glyph.More,
                onClick = { menuOpen = true },
                size = 32.dp,
                iconSize = 15.dp,
                tint = GlyphDim,
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    onClick = {
                        menuOpen = false
                        onShareImage()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Удалить",
                            style = MaterialTheme.typography.bodyMedium.copy(color = GlyphRed),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onDelete?.invoke()
                    },
                    enabled = onDelete != null,
                )
            }
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
