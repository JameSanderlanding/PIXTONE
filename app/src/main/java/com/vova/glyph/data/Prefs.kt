package com.vova.glyph.data

import android.content.Context
import android.content.SharedPreferences
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

    var unavailableTracks: Set<Long>
        get() = sp.getStringSet("unavailable_tracks", emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        set(value) = sp.edit()
            .putStringSet("unavailable_tracks", value.map { it.toString() }.toSet())
            .apply()

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(value) = sp.edit().putBoolean("shuffle", value).apply()

    var fxSpeed: Float
        get() = sp.getFloat("fx_speed", 1f).coerceIn(0.5f, 1.5f)
        set(value) = sp.edit().putFloat("fx_speed", value.coerceIn(0.5f, 1.5f)).apply()

    var fxEqEnabled: Boolean
        get() = sp.getBoolean("fx_eq_enabled", false)
        set(value) = sp.edit().putBoolean("fx_eq_enabled", value).apply()

    var fxEqPreset: String
        get() = sp.getString("fx_eq_preset", "FLAT").orEmpty()
        set(value) = sp.edit().putString("fx_eq_preset", value).apply()

    var fxEqBands: String
        get() = sp.getString("fx_eq_bands", "").orEmpty()
        set(value) = sp.edit().putString("fx_eq_bands", value).apply()

    var fxBassEnabled: Boolean
        get() = sp.getBoolean("fx_bass_enabled", false)
        set(value) = sp.edit().putBoolean("fx_bass_enabled", value).apply()

    var fxBassStrength: Int
        get() = sp.getInt("fx_bass_strength", 0).coerceIn(0, 1000)
        set(value) = sp.edit().putInt("fx_bass_strength", value.coerceIn(0, 1000)).apply()

    var fxLoudnessEnabled: Boolean
        get() = sp.getBoolean("fx_loudness_enabled", false)
        set(value) = sp.edit().putBoolean("fx_loudness_enabled", value).apply()

    var fxLoudnessGain: Int
        get() = sp.getInt("fx_loudness_gain", 0).coerceIn(0, 1500)
        set(value) = sp.edit().putInt("fx_loudness_gain", value.coerceIn(0, 1500)).apply()

    var fxReverbEnabled: Boolean
        get() = sp.getBoolean("fx_reverb_enabled", false)
        set(value) = sp.edit().putBoolean("fx_reverb_enabled", value).apply()

    var fxReverbRoom: Int
        get() = sp.getInt("fx_reverb_room", 70).coerceIn(0, 100)
        set(value) = sp.edit().putInt("fx_reverb_room", value.coerceIn(0, 100)).apply()

    var fxReverbDamp: Int
        get() = sp.getInt("fx_reverb_damp", 40).coerceIn(0, 100)
        set(value) = sp.edit().putInt("fx_reverb_damp", value.coerceIn(0, 100)).apply()

    var fxReverbWet: Int
        get() = sp.getInt("fx_reverb_wet", 30).coerceIn(0, 100)
        set(value) = sp.edit().putInt("fx_reverb_wet", value.coerceIn(0, 100)).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun resetFx() {
        sp.edit()
            .putFloat("fx_speed", 1f)
            .putBoolean("fx_eq_enabled", false)
            .putString("fx_eq_preset", "FLAT")
            .putString("fx_eq_bands", "")
            .putBoolean("fx_bass_enabled", false)
            .putInt("fx_bass_strength", 0)
            .putBoolean("fx_loudness_enabled", false)
            .putInt("fx_loudness_gain", 0)
            .putBoolean("fx_reverb_enabled", false)
            .putInt("fx_reverb_room", 70)
            .putInt("fx_reverb_damp", 40)
            .putInt("fx_reverb_wet", 30)
            .apply()
    }

    fun selectEqPreset(preset: String) {
        sp.edit()
            .putString("fx_eq_preset", preset)
            .putBoolean("fx_eq_enabled", true)
            .apply()
    }

    fun setCustomEq(levels: String) {
        sp.edit()
            .putString("fx_eq_bands", levels)
            .putString("fx_eq_preset", "CUSTOM")
            .putBoolean("fx_eq_enabled", true)
            .apply()
    }

    var hiddenAlbumKeys: Set<String>
        get() = sp.getStringSet("hidden_albums", emptySet())
            ?.mapTo(HashSet()) { Uri.decode(it) }
            ?: emptySet()
        set(value) {
            sp.edit()
                .putStringSet("hidden_albums", value.mapTo(HashSet()) { Uri.encode(it) })
                .apply()
        }

    var librarySort: String?
        get() = sp.getString("sort_library", null)
        set(value) = sp.edit().putString("sort_library", value).apply()

    var playlistSort: String?
        get() = sp.getString("sort_playlist", null)
        set(value) = sp.edit().putString("sort_playlist", value).apply()

    var quoteFont: String?
        get() = sp.getString("quote_font", null)
        set(value) = sp.edit().putString("quote_font", value).apply()

    var quoteSize: String?
        get() = sp.getString("quote_size", null)
        set(value) = sp.edit().putString("quote_size", value).apply()

    var onlineProxy: String
        get() = sp.getString("online_proxy", OnlineEndpoints.DEFAULT_PROXY).orEmpty()
        set(value) = sp.edit().putString("online_proxy", value.trim()).apply()

    fun isAlbumHidden(key: String): Boolean = key in hiddenAlbumKeys

    fun hideAlbums(keys: Collection<String>) {
        if (keys.isEmpty()) return
        hiddenAlbumKeys = hiddenAlbumKeys + keys
    }

    fun folderUriOrNull(): Uri? = folderUri?.let(Uri::parse)
}
