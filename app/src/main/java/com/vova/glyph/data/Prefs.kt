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

    var hiddenAlbumKeys: Set<String>
        get() = sp.getStringSet("hidden_albums", emptySet())
            ?.mapTo(HashSet()) { Uri.decode(it) }
            ?: emptySet()
        set(value) {
            sp.edit()
                .putStringSet("hidden_albums", value.mapTo(HashSet()) { Uri.encode(it) })
                .apply()
        }

    fun isAlbumHidden(key: String): Boolean = key in hiddenAlbumKeys

    fun hideAlbums(keys: Collection<String>) {
        if (keys.isEmpty()) return
        hiddenAlbumKeys = hiddenAlbumKeys + keys
    }

    fun folderUriOrNull(): Uri? = folderUri?.let(Uri::parse)
}
