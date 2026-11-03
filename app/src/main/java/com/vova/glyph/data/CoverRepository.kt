package com.vova.glyph.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CoverRepository(
    private val artworkStore: ArtworkStore,
    private val geniusGate: GeniusGate,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun downloadCover(artist: String, title: String, key: String): String? =
        withContext(Dispatchers.IO) {
            val candidates = buildSearchCandidatesWithTranslit(
                cleanArtistForSearch(artist),
                cleanTitleForSearch(title),
            )
            if (candidates.isEmpty()) return@withContext null

            var geniusAttempts = 0
            for ((candidateArtist, candidateTitle) in candidates) {
                for ((source, find) in SOURCES) {
                    if (source == GENIUS) {
                        if (geniusAttempts >= MAX_GENIUS_ATTEMPTS) continue

                        if (isUnknownArtist(candidateArtist)) continue
                        geniusAttempts++
                    }
                    val url = find(candidateArtist, candidateTitle) ?: continue
                    val bytes = downloadImage(url) ?: continue
                    val path = artworkStore.saveNetworkArt(bytes, key, source) ?: continue
                    return@withContext path
                }
            }
            null
        }

    private val SOURCES: List<Pair<String, (String, String) -> String?>> = listOf(
        GENIUS to ::geniusCoverUrl,
        "deezer" to ::deezerCoverUrl,
        "itunes" to ::itunesCoverUrl,
    )

    private fun geniusCoverUrl(artist: String, title: String): String? {
        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        val url = "https://genius.com/api/search/multi".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", "5")
            .addQueryParameter("q", query)
            .build()
            .toString()
        val body = get(url, browser = true) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val sections = json.optJSONObject("response")?.optJSONArray("sections") ?: return null

        var best: String? = null
        var bestScore = -1
        for (i in 0 until sections.length()) {
            val hits = sections.optJSONObject(i)?.optJSONArray("hits") ?: continue
            for (j in 0 until hits.length()) {
                val hit = hits.optJSONObject(j) ?: continue
                if (hit.optString("type") != "song") continue
                val result = hit.optJSONObject("result") ?: continue
                val art = result.optString("song_art_image_url")
                    .takeIf { it.startsWith("https://") } ?: continue

                val score = resultScore(
                    result.optJSONObject("primary_artist")?.optString("name").orEmpty(),
                    result.optString("title"),
                    artist,
                    title,
                )

                if (score == 0) continue
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
            }
        }
        return best
    }

    private fun deezerCoverUrl(artist: String, title: String): String? {
        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        val url = "https://api.deezer.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("limit", "10")
            .build()
            .toString()
        val body = get(url) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val results = json.optJSONArray("data") ?: return null

        var best: String? = null
        var bestScore = -1
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val album = item.optJSONObject("album") ?: continue
            val art = sequenceOf("cover_xl", "cover_big", "cover_medium")
                .map { album.optString(it) }
                .firstOrNull { it.startsWith("https://") } ?: continue

            val score = resultScore(
                item.optJSONObject("artist")?.optString("name").orEmpty(),
                item.optString("title"),
                artist,
                title,
            )
            if (score == 0) continue
            if (score > bestScore) {
                bestScore = score
                best = art
            }
        }
        return best
    }

    private fun itunesCoverUrl(artist: String, title: String): String? =
        itunesCoverUrl(artist, title, country = null)
            ?: itunesCoverUrl(artist, title, country = "RU")

    private fun itunesCoverUrl(artist: String, title: String, country: String?): String? {
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        val builder = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "10")
        if (country != null) builder.addQueryParameter("country", country)
        val url = builder.build().toString()
        val body = get(url) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val results = json.optJSONArray("results") ?: return null

        var best: String? = null
        var bestScore = -1
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val art = item.optString("artworkUrl100")
                .takeIf { it.isNotBlank() } ?: continue

            val score = resultScore(
                item.optString("artistName"),
                item.optString("trackName"),
                artist,
                title,
            )
            if (score == 0) continue
            if (score > bestScore) {
                bestScore = score

                best = art.replace("100x100bb", "600x600bb")
            }
        }
        return best
    }

    private fun get(url: String, browser: Boolean = false): String? {
        repeat(ATTEMPTS) { attempt ->
            if (browser) {
                if (geniusGate.isBlocked) return null

                val paused = geniusGate.coversPauseLeftMs
                if (paused > 0L) {
                    if (paused > MAX_COVER_PAUSE_WAIT_MS) return null
                    runCatching { Thread.sleep(paused) }
                }
                geniusGate.await()
            }
            val builder = Request.Builder().url(url)
            if (browser) {
                builder.header("User-Agent", BROWSER_UA)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                    .header("Referer", "https://genius.com/")
            } else {
                builder.header("User-Agent", APP_UA)
                    .header("Accept", "application/json")
            }
            val response = runCatching {
                client.newCall(builder.build()).execute()
            }.getOrElse { error ->

                throw IOException("Нет соединения", error)
            }

            response.use { r ->
                if (r.isSuccessful) return r.body?.string()
                val code = r.code
                if (browser && (code == 429 || code == 403)) {
                    geniusGate.block(code)
                    return null
                }

                if (code >= 500 && attempt < ATTEMPTS - 1) {
                    runCatching { Thread.sleep(BACKOFF_MS[attempt]) }
                    return@repeat
                }
                return null
            }
        }
        return null
    }

    private fun downloadImage(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", APP_UA)
            .header("Accept", "image/*")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                if (body.contentLength() > MAX_BYTES) return null
                val bytes = body.bytes()
                if (bytes.size.toLong() !in MIN_BYTES..MAX_BYTES) return null

                if (!looksLikeImage(bytes)) return null
                bytes
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val jpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        val png = bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        val webp = bytes.size > 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        return jpeg || png || webp
    }

    private companion object {
        const val MIN_BYTES = 1_024L
        const val MAX_BYTES = 4_000_000L

        const val GENIUS = "genius"

        const val MAX_GENIUS_ATTEMPTS = 1

        const val MAX_COVER_PAUSE_WAIT_MS = 22_000L

        const val ATTEMPTS = 3

        val BACKOFF_MS = longArrayOf(1_500L, 4_000L)

        const val APP_UA = "Pixtone Music Player/1.0 (Android)"
        const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    }
}
