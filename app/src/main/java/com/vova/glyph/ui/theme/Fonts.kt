package com.vova.glyph.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vova.glyph.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val DotoBaseFamily = FontFamily(
    Font(R.font.doto, FontWeight.Normal),
    Font(R.font.doto, FontWeight.Medium),
    Font(R.font.doto, FontWeight.Bold),
)

val PixelBaseFamily = FontFamily(
    Font(R.font.pixelify_sans, FontWeight.Normal),
    Font(R.font.pixelify_sans, FontWeight.Medium),
    Font(R.font.pixelify_sans, FontWeight.Bold),
)

val GroteskBaseFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

sealed interface FontFace {
    val id: String
    val title: String
    val family: FontFamily
}

enum class AppFont(
    override val id: String,
    override val title: String,
    override val family: FontFamily,
) : FontFace {
    DOTO("doto", "DOTO", DotoBaseFamily),
    PIXEL("pixel", "PIXELIFY SANS", PixelBaseFamily),
    GROTESK("grotesk", "SPACE GROTESK", GroteskBaseFamily),
    SYSTEM("system", "СИСТЕМНЫЙ", FontFamily.Default),
    ;

    companion object {
        fun fromId(id: String?, fallback: AppFont): AppFont =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

class UserFont(
    override val id: String,
    override val title: String,
    val path: String,
) : FontFace {
    override val family: FontFamily = runCatching {
        val file = File(path)
        FontFamily(
            Font(file, FontWeight.Normal),
            Font(file, FontWeight.Medium),
            Font(file, FontWeight.Bold),
        )
    }.getOrDefault(FontFamily.Default)
}

enum class FontRole(val label: String) {
    BODY("ОСНОВНОЙ ШРИФТ"),
    DISPLAY("ЗАГОЛОВКИ"),
    BRAND("АКЦЕНТНЫЙ ШРИФТ"),
}

object FontController {
    private var prefs: SharedPreferences? = null

    private var fontsDir: File? = null

    var body by mutableStateOf<FontFace>(AppFont.GROTESK)
        private set

    var display by mutableStateOf<FontFace>(AppFont.PIXEL)
        private set

    var brand by mutableStateOf<FontFace>(AppFont.DOTO)
        private set

    var customFonts by mutableStateOf<List<UserFont>>(emptyList())
        private set

    val allFonts: List<FontFace>
        get() = AppFont.entries + customFonts

    val isDefault: Boolean
        get() = body.id == AppFont.GROTESK.id &&
            display.id == AppFont.PIXEL.id &&
            brand.id == AppFont.DOTO.id

    fun attach(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val sp = app.getSharedPreferences("glyph_fonts", Context.MODE_PRIVATE)
        prefs = sp
        val dir = File(app.filesDir, "fonts").apply { mkdirs() }
        fontsDir = dir
        customFonts = readCustomFonts(sp, dir)
        val options = AppFont.entries + customFonts
        val storedBody = sp.getString(KEY_BODY, null)
        val storedDisplay = sp.getString(KEY_DISPLAY, null)
        val storedBrand = sp.getString(KEY_BRAND, null)
        body = resolve(storedBody, options, AppFont.GROTESK)
        display = resolve(storedDisplay, options, AppFont.PIXEL)
        brand = resolve(storedBrand, options, AppFont.DOTO)
        if (body.id != storedBody || display.id != storedDisplay || brand.id != storedBrand) {
            save()
        }
    }

    fun fontFor(role: FontRole): FontFace = when (role) {
        FontRole.BODY -> body
        FontRole.DISPLAY -> display
        FontRole.BRAND -> brand
    }

    fun select(role: FontRole, font: FontFace) {
        when (role) {
            FontRole.BODY -> body = font
            FontRole.DISPLAY -> display = font
            FontRole.BRAND -> brand = font
        }
        save()
    }

    fun reset() {
        body = AppFont.GROTESK
        display = AppFont.PIXEL
        brand = AppFont.DOTO
        save()
    }

    suspend fun importFont(context: Context, uri: Uri): Result<UserFont> {
        val app = context.applicationContext
        val existing = customFonts
        val imported = withContext(Dispatchers.IO) {
            runCatching { prepareFont(app, uri, existing) }
        }
        return imported.map { font ->
            customFonts = customFonts + font
            prefs?.let { writeCustomFonts(it, customFonts) }
            font
        }
    }

    fun deleteFont(font: UserFont) {
        runCatching { File(font.path).delete() }
        customFonts = customFonts.filterNot { it.id == font.id }
        if (body.id == font.id) body = AppFont.GROTESK
        if (display.id == font.id) display = AppFont.PIXEL
        if (brand.id == font.id) brand = AppFont.DOTO
        save()
        prefs?.let { writeCustomFonts(it, customFonts) }
    }

    private fun prepareFont(context: Context, uri: Uri, existing: List<UserFont>): UserFont {
        val rawName = displayName(context, uri)
        val baseTitle = rawName
            .substringBeforeLast('.', rawName)
            .trim()
            .ifBlank { "ШРИФТ" }
            .take(MAX_TITLE_LENGTH)
        var title = baseTitle
        var index = 2
        while (existing.any { it.title.equals(title, ignoreCase = true) }) {
            title = "$baseTitle $index"
            index++
        }
        val extension = rawName
            .substringAfterLast('.', "")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .take(5)
            .ifBlank { "ttf" }
        val id = "custom_${System.currentTimeMillis().toString(36)}"
        val dir = fontsDir ?: File(context.filesDir, "fonts").apply { mkdirs() }
        val file = File(dir, "$id.$extension")
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Не удалось прочитать файл")
        input.use { source ->
            file.outputStream().use { target -> source.copyTo(target) }
        }
        if (file.length() == 0L) {
            file.delete()
            error("Файл шрифта пустой")
        }
        if (file.length() > MAX_FONT_BYTES) {
            file.delete()
            error("Файл шрифта слишком большой")
        }
        val valid = runCatching { Typeface.createFromFile(file) }.getOrNull() != null
        if (!valid) {
            file.delete()
            error("Формат шрифта не поддерживается")
        }
        return UserFont(id, title, file.absolutePath)
    }

    private fun displayName(context: Context, uri: Uri): String {
        val fromProvider = runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
        return fromProvider
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }

    private fun resolve(id: String?, options: List<FontFace>, fallback: AppFont): FontFace =
        options.firstOrNull { it.id == id } ?: fallback

    private fun readCustomFonts(sp: SharedPreferences, dir: File): List<UserFont> {
        val raw = sp.getString(KEY_CUSTOM, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val title = item.optString("title")
                val name = item.optString("file")
                if (id.isBlank() || title.isBlank() || name.isBlank()) continue
                val file = File(dir, name)
                if (!file.isFile || file.length() == 0L) continue
                add(UserFont(id, title, file.absolutePath))
            }
        }
    }

    private fun writeCustomFonts(sp: SharedPreferences, fonts: List<UserFont>) {
        val array = JSONArray()
        fonts.forEach { font ->
            array.put(
                JSONObject()
                    .put("id", font.id)
                    .put("title", font.title)
                    .put("file", File(font.path).name),
            )
        }
        sp.edit().putString(KEY_CUSTOM, array.toString()).apply()
    }

    private fun save() {
        val sp = prefs ?: return
        sp.edit()
            .putString(KEY_BODY, body.id)
            .putString(KEY_DISPLAY, display.id)
            .putString(KEY_BRAND, brand.id)
            .apply()
    }

    private const val KEY_BODY = "body"
    private const val KEY_DISPLAY = "display"
    private const val KEY_BRAND = "brand"
    private const val KEY_CUSTOM = "custom_fonts"
    private const val MAX_TITLE_LENGTH = 40
    private const val MAX_FONT_BYTES = 8L * 1024 * 1024
}
