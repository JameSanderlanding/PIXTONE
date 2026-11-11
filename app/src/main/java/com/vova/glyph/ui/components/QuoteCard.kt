package com.vova.glyph.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vova.glyph.R
import com.vova.glyph.data.fitQuoteText
import com.vova.glyph.i18n.tr
import com.vova.glyph.ui.theme.AppFont
import com.vova.glyph.ui.theme.CoverTint
import com.vova.glyph.ui.theme.ThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class QuoteCardColors(
    val background: Int,
    val text: Int,
    val accent: Int,
)

data class QuoteCardSpec(
    val quote: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val colors: QuoteCardColors,
    val quoteFont: AppFont,
    val titleFont: AppFont,
    val textSize: QuoteTextSize = QuoteTextSize.MEDIUM,
)

enum class QuoteTextSize(val scale: Float, val label: String) {
    SMALL(0.8f, "S"),
    MEDIUM(1f, "M"),
    LARGE(1.25f, "L"),
    ;

    companion object {
        fun fromName(name: String?): QuoteTextSize =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

suspend fun renderQuoteCard(
    context: Context,
    spec: QuoteCardSpec,
    size: Int = QUOTE_CARD_SIZE,
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val colors = CoverTint.paletteFor(spec.artwork)?.quoteCardColors() ?: spec.colors
        drawQuoteCard(context.applicationContext, spec, colors, size)
    }.getOrNull()
}

private fun ThemePalette.quoteCardColors(): QuoteCardColors = QuoteCardColors(
    background = background.toArgb(),
    text = text.toArgb(),
    accent = accent.toArgb(),
)

suspend fun shareQuoteCard(
    context: Context,
    bitmap: Bitmap,
    trackId: Long,
    quoteId: Long,
): Boolean {
    val uri = withContext(Dispatchers.IO) {
        runCatching { saveQuoteCard(context, bitmap, trackId, quoteId) }.getOrNull()
    } ?: return false
    return withContext(Dispatchers.Main) {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, tr("ПОДЕЛИТЬСЯ")).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        }.isSuccess
    }
}

private fun saveQuoteCard(
    context: Context,
    bitmap: Bitmap,
    trackId: Long,
    quoteId: Long,
): Uri {
    val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
    val file = File(dir, "quote_${trackId}_${quoteId}.png")
    file.outputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.artprovider", file)
}

private suspend fun drawQuoteCard(
    context: Context,
    spec: QuoteCardSpec,
    colors: QuoteCardColors,
    size: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(colors.background)

    val cover = runCatching { loadCoverBitmap(context, spec.artwork, size) }.getOrNull()
    if (cover != null) {
        drawCover(canvas, cover, size)
        cover.recycle()
        drawScrim(canvas, size)
    }

    val footerTop = drawFooter(context, canvas, spec, colors, size)
    drawQuote(context, canvas, spec, colors, size, footerTop)
    return bitmap
}

private fun drawCover(canvas: Canvas, cover: Bitmap, size: Int) {
    val scale = maxOf(size.toFloat() / cover.width, size.toFloat() / cover.height)
    val width = cover.width * scale
    val height = cover.height * scale
    val left = (size - width) / 2f
    val top = (size - height) / 2f
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(
        cover,
        Rect(0, 0, cover.width, cover.height),
        RectF(left, top, left + width, top + height),
        paint,
    )
}

private fun drawScrim(canvas: Canvas, size: Int) {
    val paint = Paint()
    paint.shader = LinearGradient(
        0f,
        0f,
        0f,
        size.toFloat(),
        intArrayOf(
            Color.argb(70, 0, 0, 0),
            Color.argb(105, 0, 0, 0),
            Color.argb(195, 0, 0, 0),
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
}

private fun drawFooter(
    context: Context,
    canvas: Canvas,
    spec: QuoteCardSpec,
    colors: QuoteCardColors,
    size: Int,
): Float {
    val pad = size * 0.09f
    val maxWidth = size - pad * 2f
    val titlePaint = textPaint(
        context = context,
        font = spec.titleFont,
        fontSize = size * 0.045f,
        color = colors.text,
        bold = true,
    ).apply {
        setShadowLayer(size * 0.009f, 0f, size * 0.003f, SHADOW_COLOR)
    }
    val artistPaint = textPaint(
        context = context,
        font = spec.titleFont,
        fontSize = size * 0.030f,
        color = colors.accent,
        bold = true,
    ).apply {
        setShadowLayer(size * 0.008f, 0f, size * 0.003f, SHADOW_COLOR)
    }

    val titleBaseline = size - pad
    var footerTop = titleBaseline + titlePaint.fontMetrics.ascent
    if (spec.artist.isNotBlank()) {
        val artistBaseline = titleBaseline - size * 0.076f
        canvas.drawText(
            ellipsize(spec.artist, artistPaint, maxWidth),
            pad,
            artistBaseline,
            artistPaint,
        )
        footerTop = artistBaseline + artistPaint.fontMetrics.ascent
    }
    if (spec.title.isNotBlank()) {
        canvas.drawText(
            ellipsize(spec.title, titlePaint, maxWidth),
            pad,
            titleBaseline,
            titlePaint,
        )
    }
    return footerTop
}

private fun drawQuote(
    context: Context,
    canvas: Canvas,
    spec: QuoteCardSpec,
    colors: QuoteCardColors,
    size: Int,
    footerTop: Float,
) {
    val top = size * 0.09f
    val bottom = footerTop - size * 0.07f
    if (bottom <= top) return

    val textTop = drawQuoteMark(context, canvas, spec, colors, size, top) + size * 0.035f
    if (bottom <= textTop) return

    val paint = textPaint(
        context = context,
        font = spec.quoteFont,
        fontSize = size * 0.07f,
        color = colors.text,
        bold = true,
    ).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val layout = fitQuoteText(
        text = spec.quote,
        maxWidth = size * 0.82f,
        maxHeight = bottom - textTop,
        maxFontSize = size * 0.07f * spec.textSize.scale,
        minFontSize = size * 0.024f,
        step = size * 0.004f,
        lineHeightFactor = 1.32f,
        measure = { text, fontSize ->
            paint.textSize = fontSize
            paint.measureText(text)
        },
    )
    if (layout.lines.isEmpty()) return

    paint.textSize = layout.fontSize
    val outlinePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = layout.fontSize * 0.05f
        color = OUTLINE_COLOR
        setShadowLayer(size * 0.014f, 0f, size * 0.005f, SHADOW_COLOR)
    }
    val metrics = paint.fontMetrics
    val glyphHeight = metrics.descent - metrics.ascent
    val blockTop = textTop + ((bottom - textTop) - layout.height) / 2f
    canvas.save()
    canvas.clipRect(0f, top, size.toFloat(), bottom)
    layout.lines.forEachIndexed { index, line ->
        if (line.isEmpty()) return@forEachIndexed
        val baseline = blockTop + index * layout.lineHeight +
            (layout.lineHeight - glyphHeight) / 2f - metrics.ascent
        canvas.drawText(line, size / 2f, baseline, outlinePaint)
        canvas.drawText(line, size / 2f, baseline, paint)
    }
    canvas.restore()
}

private fun drawQuoteMark(
    context: Context,
    canvas: Canvas,
    spec: QuoteCardSpec,
    colors: QuoteCardColors,
    size: Int,
    top: Float,
): Float {
    val paint = textPaint(
        context = context,
        font = spec.quoteFont,
        fontSize = size * 0.24f,
        color = colors.accent,
        bold = true,
    )
    val bounds = Rect()
    paint.getTextBounds(QUOTE_MARK, 0, QUOTE_MARK.length, bounds)
    if (bounds.height() <= 0) return top
    paint.textSize = paint.textSize * (size * 0.11f) / bounds.height()
    paint.getTextBounds(QUOTE_MARK, 0, QUOTE_MARK.length, bounds)
    if (bounds.height() <= 0) return top
    paint.setShadowLayer(size * 0.012f, 0f, size * 0.004f, SHADOW_COLOR)
    canvas.drawText(QUOTE_MARK, size * 0.09f, top - bounds.top, paint)
    return top + bounds.height()
}

private fun textPaint(
    context: Context,
    font: AppFont,
    fontSize: Float,
    color: Int,
    bold: Boolean,
): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    typeface = font.typeface(context, bold)
    textSize = fontSize
    this.color = color
}

private fun AppFont.typeface(context: Context, bold: Boolean): Typeface {
    val font = when (this) {
        AppFont.DOTO -> R.font.doto
        AppFont.PIXEL -> R.font.pixelify_sans
        AppFont.GROTESK -> R.font.space_grotesk
        AppFont.SYSTEM -> return if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    val base = ResourcesCompat.getFont(context, font) ?: Typeface.DEFAULT
    return if (bold) Typeface.create(base, Typeface.BOLD) else base
}

private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + ELLIPSIS) > maxWidth) {
        end--
    }
    return if (end <= 0) ELLIPSIS else text.substring(0, end).trimEnd() + ELLIPSIS
}

private suspend fun loadCoverBitmap(context: Context, artwork: String?, size: Int): Bitmap? {
    if (artwork.isNullOrBlank()) return null
    return if (artwork.startsWith("http://") || artwork.startsWith("https://")) {
        val request = ImageRequest.Builder(context)
            .data(artwork)
            .size(size)
            .allowHardware(false)
            .build()
        when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> drawableToBitmap(result.drawable, size)
            else -> null
        }
    } else {
        decodeLocalBitmap(context, artwork, size)
    }
}

private fun decodeLocalBitmap(context: Context, path: String, target: Int): Bitmap? {
    val fromContent = path.startsWith("content://")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    if (fromContent) {
        val uri = Uri.parse(path)
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
    } else {
        BitmapFactory.decodeFile(path, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return if (fromContent) {
        val uri = Uri.parse(path)
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } else {
        BitmapFactory.decodeFile(path, options)
    }
}

private fun sampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    while (minOf(width, height) / (sample * 2) >= target) {
        sample *= 2
    }
    return sample
}

private fun drawableToBitmap(drawable: Drawable, target: Int): Bitmap? {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    if (width <= 0 || height <= 0) return null
    val scale = minOf(1f, target.toFloat() / maxOf(width, height))
    val bitmap = Bitmap.createBitmap(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private const val QUOTE_CARD_SIZE = 1080

private const val SHARE_DIR = "share"

private const val ELLIPSIS = "…"

private const val QUOTE_MARK = "“"

private const val SHADOW_COLOR = 0xC8000000.toInt()

private const val OUTLINE_COLOR = 0xD9000000.toInt()
