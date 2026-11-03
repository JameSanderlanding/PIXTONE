package com.vova.glyph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vova.glyph.ui.theme.GlyphWhite

enum class Glyph {
    Play, Pause, Next, Previous,
    Heart, HeartFilled,
    Search, Library, Chat, Settings, Close, Plus, Check,
    Back, Download, More, Shuffle, Repeat, RepeatOne,
    Folder, Sparkle, Trash, Text,
}

@Composable
fun GlyphIcon(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = GlyphWhite,
    stroke: Dp = 1.7.dp,
    filled: Boolean = false,
) {
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension / 24f
        val sw = stroke.toPx()
        val strokeStyle = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)

        fun path(block: Path.() -> Unit, fill: Boolean = false, close: Boolean = true): Path {
            val path = Path().apply {
                block()
                if (close) close()
            }
            if (fill) drawPath(path, tint) else drawPath(path, tint, style = strokeStyle)
            return path
        }

        fun strokeCircle(cx: Float, cy: Float, radius: Float, fill: Boolean = false) {
            drawCircle(
                color = tint,
                radius = radius * u,
                center = p(cx, cy),
                style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else strokeStyle,
            )
        }

        when (glyph) {
            Glyph.Play -> path({ moveTo(8f * u, 4.5f * u); lineTo(20f * u, 12f * u); lineTo(8f * u, 19.5f * u) }, fill = true)
            Glyph.Pause -> {
                drawRoundRect(tint, topLeft = p(6.5f, 5f), size = Size(4f * u, 14f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * u))
                drawRoundRect(tint, topLeft = p(13.5f, 5f), size = Size(4f * u, 14f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * u))
            }

            Glyph.Next -> {
                path({ moveTo(6f * u, 5f * u); lineTo(16f * u, 12f * u); lineTo(6f * u, 19f * u) }, fill = true)
                drawRoundRect(tint, topLeft = p(17f, 5f), size = Size(2f * u, 14f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * u))
            }

            Glyph.Previous -> {
                drawRoundRect(tint, topLeft = p(5f, 5f), size = Size(2f * u, 14f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * u))
                path({ moveTo(18f * u, 5f * u); lineTo(8f * u, 12f * u); lineTo(18f * u, 19f * u) }, fill = true)
            }

            Glyph.Heart, Glyph.HeartFilled -> {
                val heart = Path().apply {
                    moveTo(12f * u, 20f * u)
                    cubicTo(4.2f * u, 14.4f * u, 2.4f * u, 10.4f * u, 3.6f * u, 7.2f * u)
                    cubicTo(4.9f * u, 3.7f * u, 9.4f * u, 3.3f * u, 12f * u, 6.4f * u)
                    cubicTo(14.6f * u, 3.3f * u, 19.1f * u, 3.7f * u, 20.4f * u, 7.2f * u)
                    cubicTo(21.6f * u, 10.4f * u, 19.8f * u, 14.4f * u, 12f * u, 20f * u)
                    close()
                }
                if (filled || glyph == Glyph.HeartFilled) drawPath(heart, tint)
                else drawPath(heart, tint, style = strokeStyle)
            }

            Glyph.Search -> {
                strokeCircle(10.3f, 10.3f, 6.2f)
                line(15f, 15f, 20.5f, 20.5f)
            }

            Glyph.Library -> {
                line(4f, 6f, 20f, 6f)
                line(4f, 12f, 20f, 12f)
                line(4f, 18f, 13f, 18f)
                strokeCircle(17.5f, 18f, 1.6f, fill = true)
            }

            Glyph.Chat -> {
                drawRoundRect(
                    tint,
                    topLeft = p(3f, 4f),
                    size = Size(18f * u, 13f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * u),
                    style = strokeStyle,
                )
                line(8f, 17f, 8f, 21f)
                line(8f, 21f, 12.5f, 17f)
                strokeCircle(8f, 10.6f, 1f, fill = true)
                strokeCircle(12f, 10.6f, 1f, fill = true)
                strokeCircle(16f, 10.6f, 1f, fill = true)
            }

            Glyph.Settings -> {
                line(3f, 7f, 21f, 7f)
                line(3f, 12f, 21f, 12f)
                line(3f, 17f, 21f, 17f)
                strokeCircle(8.5f, 7f, 2f, fill = true)
                strokeCircle(16f, 12f, 2f, fill = true)
                strokeCircle(10.5f, 17f, 2f, fill = true)
            }

            Glyph.Close -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }

            Glyph.Plus -> {
                line(12f, 4f, 12f, 20f)
                line(4f, 12f, 20f, 12f)
            }

            Glyph.Check -> {
                path({ moveTo(4f * u, 12.5f * u); lineTo(9.5f * u, 18f * u); lineTo(20f * u, 6f * u) }, fill = false)
            }

            Glyph.Back -> {
                line(20f, 12f, 5f, 12f)
                path({ moveTo(11f * u, 6f * u); lineTo(5f * u, 12f * u); lineTo(11f * u, 18f * u) }, fill = false, close = false)
            }

            Glyph.Download -> {
                line(12f, 3f, 12f, 15f)
                path({ moveTo(7f * u, 10f * u); lineTo(12f * u, 15f * u); lineTo(17f * u, 10f * u) }, fill = false, close = false)
                line(4f, 20f, 20f, 20f)
            }

            Glyph.More -> {
                strokeCircle(12f, 5f, 1.4f, fill = true)
                strokeCircle(12f, 12f, 1.4f, fill = true)
                strokeCircle(12f, 19f, 1.4f, fill = true)
            }

            Glyph.Shuffle -> {
                line(3f, 7f, 8f, 7f)
                line(8f, 7f, 16f, 17f)
                line(16f, 17f, 21f, 17f)
                line(3f, 17f, 8f, 17f)
                line(8f, 17f, 16f, 7f)
                line(16f, 7f, 21f, 7f)
                path({ moveTo(18f * u, 4.5f * u); lineTo(21f * u, 7f * u); lineTo(18f * u, 9.5f * u) }, fill = false, close = false)
                path({ moveTo(18f * u, 14.5f * u); lineTo(21f * u, 17f * u); lineTo(18f * u, 19.5f * u) }, fill = false, close = false)
            }

            Glyph.Repeat, Glyph.RepeatOne -> {
                drawArc(
                    color = tint,
                    startAngle = -55f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = p(4.5f, 4.5f),
                    size = Size(15f * u, 15f * u),
                    style = strokeStyle,
                )
                path({ moveTo(16f * u, 2.5f * u); lineTo(20.5f * u, 5.5f * u); lineTo(16f * u, 8.5f * u) }, fill = false, close = false)
                if (glyph == Glyph.RepeatOne) {
                    line(12f, 9.5f, 12f, 15f)
                    line(10.3f, 11f, 12f, 9.5f)
                }
            }

            Glyph.Folder -> {
                path({
                    moveTo(3f * u, 6f * u)
                    lineTo(9.5f * u, 6f * u)
                    lineTo(11.5f * u, 8.5f * u)
                    lineTo(21f * u, 8.5f * u)
                    lineTo(21f * u, 19f * u)
                    lineTo(3f * u, 19f * u)
                }, fill = false)
            }

            Glyph.Sparkle -> {
                line(12f, 3f, 12f, 9f)
                line(12f, 15f, 12f, 21f)
                line(3f, 12f, 9f, 12f)
                line(15f, 12f, 21f, 12f)
                line(6f, 6f, 9f, 9f)
                line(15f, 15f, 18f, 18f)
                line(18f, 6f, 15f, 9f)
                line(9f, 15f, 6f, 18f)
            }

            Glyph.Text -> {
                drawRoundRect(
                    tint,
                    topLeft = p(5f, 4f),
                    size = Size(14f * u, 16f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * u),
                    style = strokeStyle,
                )
                line(7f, 7f, 17f, 7f)
                line(7f, 10f, 15f, 10f)
                line(7f, 13f, 13f, 13f)
                line(7f, 16f, 11f, 16f)
            }

            Glyph.Trash -> {
                line(4f, 6f, 20f, 6f)
                line(9f, 6f, 9.5f, 3.5f)
                line(9.5f, 3.5f, 14.5f, 3.5f)
                line(14.5f, 3.5f, 15f, 6f)
                path({
                    moveTo(6.5f * u, 6f * u)
                    lineTo(8f * u, 21f * u)
                    lineTo(16f * u, 21f * u)
                    lineTo(17.5f * u, 6f * u)
                }, fill = false, close = false)
                line(10.5f, 9.5f, 11f, 17f)
                line(13.5f, 9.5f, 13f, 17f)
            }
        }
    }
}
