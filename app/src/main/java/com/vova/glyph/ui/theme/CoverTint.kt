package com.vova.glyph.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.vova.glyph.data.OnlineEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object CoverTint {
    private const val CACHE_SIZE = 64
    private const val SAMPLE_SIDE = 24
    private const val MIN_COVER_SATURATION = 0.12f
    private const val MIN_PIXEL_SATURATION = 0.18f
    private const val MIN_PIXEL_LUMA = 0.06f
    private const val MAX_PIXEL_LUMA = 0.94f
    private const val MIN_ACCENT_CONTRAST = 3.5

    private val cache = LruCache<String, ThemePalette>(CACHE_SIZE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun paletteFor(path: String?): ThemePalette? {
        if (path.isNullOrBlank()) return null
        cache.get(path)?.let { return it }
        val palette = withContext(Dispatchers.IO) {
            runCatching { dominantColor(path)?.let(::fromCover) }.getOrNull()
        } ?: return null
        cache.put(path, palette)
        return palette
    }

    fun fromCover(color: Color): ThemePalette? {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        if (hsv[1] < MIN_COVER_SATURATION) return null
        val hue = hsv[0]
        val tone = hsv[1].coerceIn(0.32f, 0.82f)
        val background = tint(hue, tone * 0.55f, 0.030f)
        return ThemePalette(
            background = background,
            surface = tint(hue, tone * 0.55f, 0.052f),
            surfaceHigh = tint(hue, tone * 0.50f, 0.082f),
            text = tint(hue, tone * 0.12f, 0.962f),
            gray = tint(hue, tone * 0.22f, 0.590f),
            dim = tint(hue, tone * 0.30f, 0.368f),
            line = tint(hue, tone * 0.45f, 0.152f),
            accent = accentFor(hue, tone, background),
        )
    }

    private fun accentFor(hue: Float, tone: Float, background: Color): Color {
        val saturation = tone.coerceIn(0.42f, 0.72f)
        var value = 0.60f
        var color = tint(hue, saturation, value)
        while (contrast(color, background) < MIN_ACCENT_CONTRAST && value < 0.94f) {
            value += 0.04f
            color = tint(hue, saturation, value)
        }
        return color
    }

    private fun tint(hue: Float, saturation: Float, value: Float): Color = Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(
                hue.coerceIn(0f, 360f),
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f),
            ),
        ),
    )

    private fun dominantColor(path: String): Color? {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return remoteDominantColor(path)
        }
        val file = File(path)
        if (!file.isFile || file.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val side = min(bounds.outWidth, bounds.outHeight)
        if (side <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = max(1, side / SAMPLE_SIDE)
        }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        return try {
            dominantColor(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun remoteDominantColor(url: String): Color? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", OnlineEndpoints.USER_AGENT)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val side = min(bounds.outWidth, bounds.outHeight)
            if (side <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = max(1, side / SAMPLE_SIDE)
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return null
            try {
                dominantColor(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }.getOrNull()

    private fun dominantColor(bitmap: Bitmap): Color? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val hsv = FloatArray(3)
        val buckets = HashMap<Int, Int>()
        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            if (luma < MIN_PIXEL_LUMA || luma > MAX_PIXEL_LUMA) return@forEach
            android.graphics.Color.colorToHSV(pixel or ALPHA_MASK, hsv)
            if (hsv[1] < MIN_PIXEL_SATURATION) return@forEach
            val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        if (buckets.isEmpty()) return null
        var bestKey = 0
        var bestScore = 0f
        buckets.forEach { (key, count) ->
            val r = ((key shr 10) and 0x1F) shl 3
            val g = ((key shr 5) and 0x1F) shl 3
            val b = (key and 0x1F) shl 3
            android.graphics.Color.colorToHSV(
                android.graphics.Color.rgb(r, g, b) or ALPHA_MASK,
                hsv,
            )
            val score = count * (0.4f + hsv[1])
            if (score > bestScore) {
                bestScore = score
                bestKey = key
            }
        }
        val r = ((bestKey shr 10) and 0x1F) shl 3
        val g = ((bestKey shr 5) and 0x1F) shl 3
        val b = (bestKey and 0x1F) shl 3
        return Color(android.graphics.Color.rgb(r, g, b))
    }

    private fun contrast(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private const val ALPHA_MASK = 0xFF shl 24

    private const val HTTP_TIMEOUT_SECONDS = 10L
}
