package com.vova.glyph.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vova.glyph.data.Album
import com.vova.glyph.data.Playlist
import com.vova.glyph.data.Track
import com.vova.glyph.ui.theme.DotoFamily
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphSurfaceHigh
import com.vova.glyph.ui.theme.GlyphWhite
import com.vova.glyph.ui.theme.PixelFamily
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DotGrid(
    modifier: Modifier = Modifier,
    color: Color = GlyphWhite.copy(alpha = 0.045f),
    step: Dp = 22.dp,
    dot: Dp = 1.2.dp,
) {
    Canvas(modifier) {
        val stepPx = step.toPx()
        val radius = dot.toPx() / 2f
        var y = stepPx / 2f
        while (y < size.height) {
            var x = stepPx / 2f
            while (x < size.width) {
                drawCircle(color, radius, Offset(x, y))
                x += stepPx
            }
            y += stepPx
        }
    }
}

fun coverModel(path: String?): Any? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http://") || path.startsWith("https://") -> path
    path.startsWith("content://") -> Uri.parse(path)
    else -> File(path).takeIf { it.exists() && it.length() > 0 }
}

@Composable
fun GlyphCover(
    artwork: String?,
    seed: String,
    modifier: Modifier = Modifier,
    corner: Dp = 2.dp,
) {
    val shape = remember(corner) { RoundedCornerShape(corner) }
    val model = remember(artwork) { coverModel(artwork) }
    Box(
        modifier
            .clip(shape)
            .background(GlyphSurfaceHigh)
            .border(1.dp, GlyphLine, shape)
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to GlyphBlack.copy(alpha = 0.36f),
                        )
                    )
            )
        } else {
            GlyphPlaceholder(seed)
        }
    }
}

@Composable
fun GlyphPlaceholder(seed: String, modifier: Modifier = Modifier) {
    val pattern = remember(seed) {
        val random = Random(seed.hashCode())
        List(64) { random.nextFloat() > 0.46f }
    }
    val accent = remember(seed) { (seed.hashCode() and 0x7fffffff) % 64 }
    Canvas(modifier.fillMaxSize().background(GlyphBlack)) {
        val cols = 8
        val cell = size.minDimension / (cols + 1.1f)
        val startX = (size.width - cell * cols) / 2f + cell / 2f
        val startY = (size.height - cell * cols) / 2f + cell / 2f
        for (i in 0 until 64) {
            val col = i % cols
            val row = i / cols
            val active = pattern[i]
            val color = when {
                i == accent -> GlyphRed
                active -> GlyphWhite.copy(alpha = 0.82f)
                else -> GlyphWhite.copy(alpha = 0.08f)
            }
            drawCircle(
                color = color,
                radius = cell * if (i == accent) 0.22f else 0.15f,
                center = Offset(startX + col * cell, startY + row * cell),
            )
        }
    }
}

@Composable
fun MicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GlyphGray,
    fontSize: Int = 9,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = fontSize.sp,
            color = color,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, fontSize: Int = 14, color: Color = GlyphWhite) {
    Text(
        text = "PIXTONE",
        modifier = modifier,
        style = TextStyle(
            fontFamily = DotoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            letterSpacing = 2.sp,
            color = color,
        ),
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = GlyphLine) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun GlyphButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    leading: Glyph? = null,
) {
    val shape = RoundedCornerShape(2.dp)
    val contentColor = if (!enabled) {
        GlyphDim
    } else if (filled) {
        GlyphBlack
    } else {
        GlyphWhite
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(
                when {
                    !filled -> Color.Transparent
                    enabled -> GlyphWhite
                    else -> GlyphSurfaceHigh
                }
            )
            .border(1.dp, if (filled && enabled) GlyphWhite else GlyphLine, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            GlyphIcon(
                glyph = leading,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
                stroke = 1.6.dp,
            )
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = contentColor,
                fontSize = 11.sp,
                letterSpacing = 1.1.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlyphIconButton(
    glyph: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GlyphWhite,
    bordered: Boolean = false,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    description: String? = null,
) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(if (bordered) Modifier.border(1.dp, GlyphLine, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun GlyphChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) GlyphWhite else Color.Transparent)
            .border(1.dp, if (selected) GlyphWhite else GlyphLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) GlyphBlack else GlyphGray,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun ScreenHeader(
    index: String,
    title: String,
    subtitle: String? = null,
    onSettings: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
            Spacer(Modifier.width(8.dp))
            BrandMark(fontSize = 12)
            Spacer(Modifier.width(8.dp))
            MicroLabel(" / $index", color = GlyphDim, fontSize = 9)
            Spacer(Modifier.weight(1f))
            action?.invoke()
            if (action != null && onSettings != null) Spacer(Modifier.width(6.dp))
            if (onSettings != null) {
                GlyphIconButton(
                    glyph = Glyph.Settings,
                    onClick = onSettings,
                    modifier = Modifier.size(32.dp),
                    tint = GlyphGray,
                    size = 32.dp,
                    iconSize = 17.dp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 34.sp,
                lineHeight = 38.sp,
            ),
            color = GlyphWhite,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GlyphGray,
                    letterSpacing = 0.3.sp,
                ),
            )
        }
    }
}

@Composable
fun SectionHeader(
    number: String,
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicroLabel(number, color = GlyphRed, fontSize = 9)
        Spacer(Modifier.width(10.dp))
        MicroLabel(title, color = GlyphGray, fontSize = 10)
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun SegmentedProgress(
    progress: Float,
    onSeek: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    segments: Int = 42,
    activeColor: Color = GlyphWhite,
    inactiveColor: Color = GlyphLine,
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val scrub = remember { ScrubState() }
    val shown = (scrub.fraction ?: progress).coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .then(
                if (onSeek != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                scrub.move(seekFraction(offset.x, size.width), currentOnSeek, true)
                                scrub.finish()
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    scrub.move(seekFraction(offset.x, size.width), currentOnSeek, true)
                                },
                                onDragEnd = { scrub.finish() },
                                onDragCancel = { scrub.finish() },
                            ) { change, _ ->
                                scrub.move(
                                    seekFraction(change.position.x, size.width),
                                    currentOnSeek,
                                    false,
                                )
                            }
                        }
                } else Modifier
            )
    ) {
        val gap = 3.dp.toPx()
        val segmentWidth = ((size.width - gap * (segments - 1)) / segments).coerceAtLeast(1f)
        val barHeight = 10.dp.toPx()
        val top = (size.height - barHeight) / 2f
        val filled = (shown * segments).toInt()
        for (i in 0 until segments) {
            val x = i * (segmentWidth + gap)
            drawRoundRect(
                color = if (i < filled) activeColor else inactiveColor,
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(segmentWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
            )
        }
    }
}

private class ScrubState {
    var fraction by mutableStateOf<Float?>(null)
        private set

    private var lastSeekAt = 0L

    fun move(value: Float, onSeek: ((Float) -> Unit)?, force: Boolean) {
        fraction = value
        if (onSeek == null) return
        val now = System.currentTimeMillis()
        if (!force && now - lastSeekAt < SEEK_INTERVAL_MS) return
        lastSeekAt = now
        onSeek(value)
    }

    fun finish() {
        fraction = null
    }

    private companion object {
        const val SEEK_INTERVAL_MS = 100L
    }
}

private fun seekFraction(x: Float, width: Int): Float {
    if (width <= 0) return 0f
    return (x / width).coerceIn(0f, 1f)
}

@Composable
fun PlayingBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = GlyphWhite,
) {
    val transition = rememberInfiniteTransition(label = "bars")
    val a by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    val b by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(410, easing = LinearEasing), RepeatMode.Reverse),
        label = "b",
    )
    val c by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "c",
    )
    val levels = if (playing) listOf(a, b, c) else listOf(0.35f, 0.65f, 0.45f)
    Canvas(modifier.size(14.dp)) {
        val barWidth = size.width / 5f
        val gap = barWidth / 2f
        for (i in 0..2) {
            val h = size.height * levels[i]
            drawRoundRect(
                color = if (playing) color else GlyphDim,
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2),
            )
        }
    }
}

@Composable
fun DotSpinner(
    modifier: Modifier = Modifier,
    color: Color = GlyphWhite,
    dotCount: Int = 8,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "angle",
    )
    Canvas(modifier.size(24.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until dotCount) {
            val a = ((angle + i * (360f / dotCount)) * PI / 180f).toFloat()
            val r = radius * 0.68f
            drawCircle(
                color = color.copy(alpha = 0.15f + 0.85f * (i + 1) / dotCount),
                radius = radius * 0.11f,
                center = Offset(center.x + cos(a) * r, center.y + sin(a) * r),
            )
        }
    }
}

@Composable
fun EmptyState(
    glyph: Glyph,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .border(1.dp, GlyphLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, tint = GlyphGray, modifier = Modifier.size(28.dp), stroke = 1.4.dp)
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(4.dp).background(GlyphRed, CircleShape))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = GlyphWhite,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun GlyphTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(2.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(GlyphBlack, shape)
            .border(1.dp, GlyphLine, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = GlyphWhite),
        cursorBrush = SolidColor(GlyphRed),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(color = GlyphDim),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showCover: Boolean = true,
    divider: Boolean = true,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val coverSeed = remember(track.title, track.artist) { track.title + track.artist }
    val durationLabel = remember(track.durationMs) { formatDuration(track.durationMs) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCover) {
                Box {
                    GlyphCover(
                        artwork = track.artwork,
                        seed = coverSeed,
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
            }
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
                    track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                durationLabel,
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
            onLyrics?.let {
                Box(
                    Modifier
                        .size(36.dp)
                        .clickable(onClick = it),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(
                        glyph = Glyph.Text,
                        tint = GlyphGray,
                        modifier = Modifier.size(17.dp),
                        stroke = 1.5.dp,
                    )
                }
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
                        text = { Text(if (track.favorite) "Убрать из избранного" else "В избранное") },
                        onClick = {
                            menuOpen = false
                            onFavorite()
                        },
                    )
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Добавить в плейлист") },
                            onClick = {
                                menuOpen = false
                                onAddToPlaylist()
                            },
                        )
                    }
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Убрать из плейлиста") },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Удалить",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GlyphRed),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        }
        if (divider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = if (showCover) 82.dp else 20.dp)
                    .height(1.dp)
                    .background(GlyphLine.copy(alpha = 0.55f))
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить трек?",
            message = "Трек «${track.title}» будет убран из приложения. Файл на устройстве останется.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete?.invoke()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val duration = if (durationMs > 0) durationMs else track.durationMs
    val progress = if (duration > 0) (positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().background(GlyphBlack)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine)) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(1.dp)
                    .background(GlyphWhite)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphCover(
                artwork = track.artwork,
                seed = track.title + track.artist,
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = GlyphWhite,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(
                    if (isPlaying) Glyph.Pause else Glyph.Play,
                    tint = GlyphWhite,
                    modifier = Modifier.size(18.dp),
                    stroke = 1.8.dp,
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(34.dp).clickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.Next, tint = GlyphGray, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        GlyphCover(
            artwork = album.artwork,
            seed = album.name + album.artist,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (album.unknown) "${album.trackCount} ТРЕКОВ" else album.artist.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = GlyphDim, fontSize = 8.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlyphBottomNav(
    selected: com.vova.glyph.Tab,
    onSelect: (com.vova.glyph.Tab) -> Unit,
) {
    val items = listOf(
        Triple(com.vova.glyph.Tab.LIBRARY, Glyph.Library, "БИБЛИОТЕКА"),
        Triple(com.vova.glyph.Tab.ALBUMS, Glyph.Album, "АЛЬБОМЫ"),
        Triple(com.vova.glyph.Tab.FAVORITES, Glyph.Heart, "ИЗБРАННОЕ"),
    )
    Column(Modifier.fillMaxWidth().background(GlyphBlack)) {
        Hairline()
        Row(Modifier.fillMaxWidth().height(62.dp)) {
            items.forEach { (tab, glyph, label) ->
                val active = tab == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GlyphIcon(
                        glyph = glyph,
                        tint = if (active) GlyphWhite else GlyphDim,
                        modifier = Modifier.size(19.dp),
                        stroke = if (active) 1.8.dp else 1.5.dp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (active) GlyphWhite else GlyphDim,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .size(if (active) 4.dp else 0.dp)
                            .background(GlyphRed, CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Playlist) -> Unit,
    onCreateNew: (() -> Unit)? = null,
) {
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 22.dp)) {
            MicroLabel("ДОБАВИТЬ В ПЛЕЙЛИСТ", color = GlyphRed)
            Spacer(Modifier.height(8.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            if (playlists.isEmpty()) {
                Text(
                    "Плейлистов пока нет. Создайте первый — добавьте название, описание и обложку.",
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                )
                Spacer(Modifier.height(18.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(playlist) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlyphCover(
                                artwork = playlist.cover,
                                seed = playlist.name,
                                modifier = Modifier.size(42.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${playlist.trackCount} ТРЕКОВ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlyphDim,
                                        fontSize = 8.sp,
                                    ),
                                )
                            }
                            GlyphIcon(Glyph.Plus, tint = GlyphGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (onCreateNew != null) {
                GlyphButton(
                    text = "Новый плейлист",
                    onClick = onCreateNew,
                    filled = false,
                    leading = Glyph.Plus,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "—"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
