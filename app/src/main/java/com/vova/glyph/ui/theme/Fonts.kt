package com.vova.glyph.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vova.glyph.R

 
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

 
enum class AppFont(
    val id: String,
    val title: String,
    val family: FontFamily,
) {
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

 
enum class FontRole(val label: String) {
    BODY("ОСНОВНОЙ ШРИФТ"),
    DISPLAY("ЗАГОЛОВКИ"),
    BRAND("АКЦЕНТНЫЙ ШРИФТ"),
}





object FontController {
    private var prefs: SharedPreferences? = null

    var body by mutableStateOf(AppFont.GROTESK)
        private set

    var display by mutableStateOf(AppFont.PIXEL)
        private set

    var brand by mutableStateOf(AppFont.DOTO)
        private set

    val isDefault: Boolean
        get() = body == AppFont.GROTESK && display == AppFont.PIXEL && brand == AppFont.DOTO

    fun attach(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext
            .getSharedPreferences("glyph_fonts", Context.MODE_PRIVATE)
        prefs = sp
        body = AppFont.fromId(sp.getString(KEY_BODY, null), AppFont.GROTESK)
        display = AppFont.fromId(sp.getString(KEY_DISPLAY, null), AppFont.PIXEL)
        brand = AppFont.fromId(sp.getString(KEY_BRAND, null), AppFont.DOTO)
    }

    fun fontFor(role: FontRole): AppFont = when (role) {
        FontRole.BODY -> body
        FontRole.DISPLAY -> display
        FontRole.BRAND -> brand
    }

    fun select(role: FontRole, font: AppFont) {
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
}
