package com.vova.glyph.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val nativeName: String) {
    RU("ru", "Русский"),
    ZH("zh", "中文"),
    JA("ja", "日本語"),
    FR("fr", "Français"),
    EN("en", "English"),
    DE("de", "Deutsch"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

object LangController {
    private var prefs: SharedPreferences? = null

    var language by mutableStateOf(AppLanguage.EN)
        private set

    fun attach(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext
            .getSharedPreferences("glyph_language", Context.MODE_PRIVATE)
        prefs = sp
        language = AppLanguage.fromCode(sp.getString("language", null))
        clearTranslationCache()
    }

    fun select(value: AppLanguage) {
        language = value
        clearTranslationCache()
        prefs?.edit()?.putString("language", value.code)?.apply()
    }
}

fun tr(text: String): String {
    val language = LangController.language
    if (language == AppLanguage.RU || text.isEmpty()) return text

    if (!text.containsCyrillic()) return text
    return translate(text, language)
}

private fun String.containsCyrillic(): Boolean {
    for (char in this) {
        if (char in 'А'..'я' || char == 'Ё' || char == 'ё') return true
    }
    return false
}
