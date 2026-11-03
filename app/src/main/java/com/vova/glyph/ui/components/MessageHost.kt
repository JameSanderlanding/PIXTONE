package com.vova.glyph.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphWhite
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt








@Composable
fun GlyphMessageHost(
    messages: Flow<String>,
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf<Message?>(null) }
    val finished = remember { Channel<Unit>(Channel.CONFLATED) }

    LaunchedEffect(Unit) {
        var id = 0L
        messages.collect { text ->
            current = Message(++id, text)
            
            finished.receive()
        }
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        current?.let { message ->
            key(message.id) {
                AnimatedMessage(
                    text = message.text,
                    onFinished = {
                        current = null
                        finished.trySend(Unit)
                    },
                )
            }
        }
    }
}

private data class Message(val id: Long, val text: String)

@Composable
private fun AnimatedMessage(text: String, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val layer = rememberGraphicsLayer()
    var pixelLevels by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        
        progress.animateTo(1f, tween(FADE_IN_MS, easing = LinearOutSlowInEasing))
        delay(HOLD_MS)
        
        val snapshot = runCatching { layer.toImageBitmap() }.getOrNull()
        pixelLevels = snapshot?.let { buildPixelLevels(it) }.orEmpty()
        
        progress.animateTo(2f, tween(PIXELATE_MS, easing = LinearEasing))
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    Box(
        Modifier
            .graphicsLayer {
                val p = progress.value
                alpha = if (p <= 1f) p else (2f - p).coerceIn(0f, 1f)
            }
            .drawWithContent {
                val p = progress.value
                if (p > 1f && pixelLevels.isNotEmpty()) {
                    drawPixelated(pixelLevels, p - 1f)
                } else {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!finished) {
                    finished = true
                    onFinished()
                }
            },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(2.dp),
            color = GlyphWhite,
            contentColor = GlyphBlack,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = GlyphBlack,
            )
        }
    }
}

 
private fun DrawScope.drawPixelated(levels: List<ImageBitmap>, t: Float) {
    val index = (t * levels.size).toInt().coerceIn(0, levels.lastIndex)
    val image = levels[index]
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.None,
    )
}

 
private fun buildPixelLevels(source: ImageBitmap): List<ImageBitmap> {
    val original = source.asAndroidBitmap()
    val bitmap = if (original.config == Bitmap.Config.HARDWARE) {
        original.copy(Bitmap.Config.ARGB_8888, false) ?: original
    } else {
        original
    }
    val result = ArrayList<ImageBitmap>(PIXEL_STEPS)
    var width = bitmap.width
    var height = bitmap.height
    repeat(PIXEL_STEPS) {
        width = (width / 2).coerceAtLeast(1)
        height = (height / 2).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, width, height, false)
        result += small.asImageBitmap()
    }
    return result
}

private const val FADE_IN_MS = 220
private const val HOLD_MS = 1_600L
private const val PIXELATE_MS = 700
private const val PIXEL_STEPS = 6
