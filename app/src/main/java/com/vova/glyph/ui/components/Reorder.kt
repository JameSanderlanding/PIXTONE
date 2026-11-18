package com.vova.glyph.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vova.glyph.ui.theme.GlyphRed
import kotlin.math.abs

class ReorderState internal constructor(private val listState: LazyListState) {
    internal var onStart: (Long) -> Unit = {}
    internal var onDrop: (Long, Long) -> Unit = { _, _ -> }
    internal var keys: Set<Long> = emptySet()
    internal var edgePx: Float = 0f
    internal var maxStepPx: Float = 0f

    var draggingKey by mutableStateOf<Long?>(null)
        private set

    var targetKey by mutableStateOf<Long?>(null)
        private set

    var dragOffset by mutableFloatStateOf(0f)
        private set

    fun isDropTarget(key: Long): Boolean =
        draggingKey != null && key != draggingKey && key == targetKey

    fun start(key: Long) {
        draggingKey = key
        targetKey = key
        dragOffset = 0f
        onStart(key)
    }

    fun dragBy(delta: Float) {
        val key = draggingKey ?: return
        dragOffset += delta
        targetKey = resolveTarget(key)
    }

    internal suspend fun autoScroll() {
        val key = draggingKey ?: return
        if (edgePx <= 0f || maxStepPx <= 0f) return
        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val center = current.offset + dragOffset + current.size / 2f
        val topEdge = info.viewportStartOffset + edgePx
        val bottomEdge = info.viewportEndOffset - edgePx
        val step = when {
            center < topEdge ->
                -maxStepPx * ((topEdge - center) / edgePx).coerceIn(0f, 1f)
            center > bottomEdge ->
                maxStepPx * ((center - bottomEdge) / edgePx).coerceIn(0f, 1f)
            else -> 0f
        }
        if (step == 0f) return
        val consumed = listState.scrollBy(step)
        if (consumed == 0f) return
        dragOffset += consumed
        targetKey = resolveTarget(key)
    }

    fun cancel() = reset()

    fun finish() {
        val key = draggingKey ?: return
        val target = resolveTarget(key) ?: targetKey
        reset()
        if (target != null && target != key) onDrop(key, target)
    }

    private fun reset() {
        draggingKey = null
        targetKey = null
        dragOffset = 0f
    }

    private fun resolveTarget(key: Long): Long? {
        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return null
        val center = current.offset + dragOffset + current.size / 2f
        val tracks = info.visibleItemsInfo.filter { it.key is Long && it.key in keys }
        val hit = tracks.firstOrNull { center >= it.offset && center <= it.offset + it.size }
            ?: tracks.minByOrNull { abs(it.offset + it.size / 2f - center) }
        return hit?.key as? Long
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    keys: Set<Long>,
    onStart: (Long) -> Unit = {},
    onDrop: (fromKey: Long, toKey: Long) -> Unit,
): ReorderState {
    val state = remember(listState) { ReorderState(listState) }
    val latestStart = rememberUpdatedState(onStart)
    val latestDrop = rememberUpdatedState(onDrop)
    val density = LocalDensity.current
    SideEffect {
        state.onStart = latestStart.value
        state.onDrop = latestDrop.value
        state.keys = keys
        state.edgePx = with(density) { 80.dp.toPx() }
        state.maxStepPx = with(density) { 10.dp.toPx() }
    }
    val dragging = state.draggingKey != null
    LaunchedEffect(dragging) {
        if (!dragging) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            state.autoScroll()
        }
    }
    return state
}

@Composable
fun Modifier.reorderable(
    state: ReorderState,
    key: Long,
    dragEnabled: Boolean = true,
): Modifier {
    val active = state.draggingKey == key
    val lifted by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(140),
        label = "reorderLift",
    )
    val haptics = LocalHapticFeedback.current
    return this
        .zIndex(if (active) 1f else 0f)
        .graphicsLayer {
            translationY = if (active) state.dragOffset else 0f
            val scale = 1f + 0.025f * lifted
            scaleX = scale
            scaleY = scale
            alpha = 1f - 0.06f * lifted
        }
        .pointerInput(key, dragEnabled) {
            if (dragEnabled) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.start(key)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        state.dragBy(amount.y)
                    },
                    onDragEnd = { state.finish() },
                    onDragCancel = { state.cancel() },
                )
            } else {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.start(key)
                        state.cancel()
                    },
                    onDrag = { change, _ -> change.consume() },
                    onDragEnd = {},
                    onDragCancel = {},
                )
            }
        }
}

@Composable
fun Modifier.dropIndicator(state: ReorderState, key: Long): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (state.isDropTarget(key)) 1f else 0f,
        animationSpec = tween(100),
        label = "dropTarget",
    )
    val strokeWidth = 2.dp
    return drawWithContent {
        drawContent()
        if (alpha > 0f) {
            drawRect(GlyphRed.copy(alpha = 0.08f * alpha))
            val stroke = strokeWidth.toPx()
            drawLine(
                color = GlyphRed.copy(alpha = alpha),
                start = Offset(0f, stroke / 2f),
                end = Offset(size.width, stroke / 2f),
                strokeWidth = stroke,
            )
            drawLine(
                color = GlyphRed.copy(alpha = alpha),
                start = Offset(0f, size.height - stroke / 2f),
                end = Offset(size.width, size.height - stroke / 2f),
                strokeWidth = stroke,
            )
        }
    }
}
