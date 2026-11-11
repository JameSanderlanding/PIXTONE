package com.vova.glyph.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    val streamUrl: String?,
) {
    fun toTrack(): Track = Track(
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = streamUrl.orEmpty(),
        artwork = coverUrl,
        source = Track.SOURCE_JIOSAAVN,
        sourceId = id,
        addedAt = System.currentTimeMillis(),
    )
}

class JioSaavnClient(private val transport: OnlineTransport) {
    suspend fun search(query: String, limit: Int = SEARCH_LIMIT): List<OnlineTrack> {
        if (query.isBlank()) return emptyList()
        val url = JioSaavnParser.searchUrl(query, limit)
        return JioSaavnParser.parseSongs(transport.getJson(url), limit)
    }

    companion object {
        const val SEARCH_LIMIT = 30
    }
}

object JioSaavnParser {
    private const val DES_KEY = "38346591"

    private val QUALITY = Regex("_(12|48|96|160)\\.(mp4|m4a)$")

    private val ENTITY = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")

    fun searchUrl(query: String, limit: Int = JioSaavnClient.SEARCH_LIMIT): HttpUrl =
        OnlineEndpoints.JIOSAAVN_API.toHttpUrl().newBuilder()
            .addQueryParameter("__call", "search.getResults")
            .addQueryParameter("q", query.trim())
            .addQueryParameter("ctx", "web6dot0")
            .addQueryParameter("api_version", "4")
            .addQueryParameter("_format", "json")
            .addQueryParameter("_marker", "0")
            .addQueryParameter("n", limit.coerceIn(1, 50).toString())
            .build()

    fun parseSongs(json: String, limit: Int = JioSaavnClient.SEARCH_LIMIT): List<OnlineTrack> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val results = root.optJSONArray("results") ?: return emptyList()
        val seen = HashSet<String>(results.length())
        val tracks = ArrayList<OnlineTrack>(minOf(limit, results.length()))
        for (index in 0 until results.length()) {
            if (tracks.size >= limit) break
            val song = results.optJSONObject(index) ?: continue
            val id = song.optString("id").trim()
            val title = decodeEntities(song.optString("title")).trim()
            if (id.isEmpty() || title.isEmpty() || !seen.add(id)) continue
            tracks += song.toOnlineTrack(id, title)
        }
        return tracks
    }

    fun decrypt(encrypted: String): String? = runCatching {
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES"))
        String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), Charsets.UTF_8)
            .trimEnd('\u0000')
            .trim()
    }.getOrNull()?.takeIf { it.startsWith("http") }

    fun qualityUrl(url: String): String = url.replace(QUALITY, "_320.\$2")

    fun coverUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        if (url.isEmpty()) return null
        return url.replace("-150x150.", "-500x500.")
    }

    fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return ENTITY.replace(text) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.drop(2).toIntOrNull(16)?.let(::charOf) ?: match.value
                entity.startsWith("#") ->
                    entity.drop(1).toIntOrNull()?.let(::charOf) ?: match.value
                else -> when (entity.lowercase()) {
                    "amp" -> "&"
                    "lt" -> "<"
                    "gt" -> ">"
                    "quot" -> "\""
                    "apos" -> "'"
                    "nbsp" -> " "
                    else -> match.value
                }
            }
        }
    }

    private fun JSONObject.toOnlineTrack(id: String, title: String): OnlineTrack {
        val info = optJSONObject("more_info")
        val album = decodeEntities(info?.optString("album").orEmpty()).trim()
        val subtitle = decodeEntities(optString("subtitle"))
        val artist = artistFromSubtitle(subtitle, album)
            .ifBlank { primaryArtists(info) }
            .trim()
        val encrypted = info?.optString("encrypted_media_url").orEmpty()
        val stream = encrypted
            .takeIf { it.isNotBlank() }
            ?.let(::decrypt)
            ?.let(::qualityUrl)
        val duration = info?.optString("duration")?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.times(1_000L)
            ?: 0L
        return OnlineTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            coverUrl = coverUrl(optString("image")),
            durationMs = duration,
            streamUrl = stream,
        )
    }

    private fun artistFromSubtitle(subtitle: String, album: String): String = when {
        album.isNotBlank() && subtitle.endsWith(" - $album") -> subtitle.removeSuffix(" - $album")
        " - " in subtitle -> subtitle.substringBeforeLast(" - ")
        else -> ""
    }

    private fun primaryArtists(info: JSONObject?): String {
        val array = info?.optJSONObject("artistMap")?.optJSONArray("primary_artists")
            ?: return ""
        val names = LinkedHashSet<String>(array.length())
        for (index in 0 until array.length()) {
            val name = decodeEntities(array.optJSONObject(index)?.optString("name").orEmpty())
                .trim()
            if (name.isNotEmpty()) names += name
        }
        return names.joinToString(", ")
    }

    private fun charOf(code: Int): String =
        runCatching { String(Character.toChars(code)) }.getOrDefault("")
}
