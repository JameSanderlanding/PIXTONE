package com.vova.glyph.data

import android.content.Context
import android.net.Uri

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(value) = sp.edit().putBoolean("onboarding_done", value).apply()

    var folderUri: String?
        get() = sp.getString("folder_uri", null)
        set(value) = sp.edit().putString("folder_uri", value).apply()

    var folderName: String?
        get() = sp.getString("folder_name", null)
        set(value) = sp.edit().putString("folder_name", value).apply()

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(value) = sp.edit().putBoolean("shuffle", value).apply()

    fun folderUriOrNull(): Uri? = folderUri?.let(Uri::parse)
}
