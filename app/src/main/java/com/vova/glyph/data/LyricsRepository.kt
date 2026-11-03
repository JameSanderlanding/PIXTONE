package com.vova.glyph.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
















class LyricsRepository(
    private val geniusGate: GeniusGate,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun fetchLyrics(artist: String, title: String): LyricsResult? =
        withContext(Dispatchers.IO) {
            val candidates = buildSearchCandidatesWithTranslit(
                cleanArtistForSearch(artist),
                cleanTitleForSearch(title),
            )
            if (candidates.isEmpty()) return@withContext null

            coroutineScope {
                val channel = Channel<LyricsResult>(Channel.UNLIMITED)

                val exactJob = launch {
                    safeFetch {
                        for ((a, t) in getAttempts(candidates)) {
                            lrclibGet(a, t)?.let { return@safeFetch it }
                        }
                        null
                    }?.let { channel.send(it) }
                }
                val searchJob = launch {
                    safeFetch {
                        for ((a, t) in candidates) {
                            lrclibSearch(a, t)?.let { return@safeFetch it }
                        }
                        null
                    }?.let { channel.send(it) }
                }
                val ovhJob = launch {
                    safeFetch {
                        for ((a, t) in candidates) {
                            lyricsOvh(a, t)?.let { return@safeFetch it }
                        }
                        null
                    }?.let { channel.send(it) }
                }
                val geniusJob = launch {
                    
                    
                    
                    
                    
                    geniusGate.pauseCovers(LYRICS_COVER_PAUSE_MS)
                    try {
                        
                        
                        delay(GENIUS_HEAD_START_MS)
                        safeFetch {
                            for ((a, t) in candidates) {
                                genius(a, t)?.let { return@safeFetch it }
                            }
                            null
                        }?.let { channel.send(it) }
                    } finally {
                        geniusGate.resumeCovers()
                    }
                }
                val jobs = listOf(exactJob, searchJob, ovhJob, geniusJob)

                
                
                val closer = launch {
                    jobs.joinAll()
                    channel.close()
                }

                try {
                    val first = withTimeoutOrNull(FIRST_RESULT_TIMEOUT_MS) {
                        channel.receiveCatching().getOrNull()
                    } ?: return@coroutineScope null

                    if (first.synced.isNotEmpty()) return@coroutineScope first

                    
                    val deadline = System.currentTimeMillis() + SYNC_GRACE_MS
                    while (true) {
                        val left = deadline - System.currentTimeMillis()
                        if (left <= 0L) break
                        val next = withTimeoutOrNull(left) {
                            channel.receiveCatching().getOrNull()
                        } ?: break
                        if (next.synced.isNotEmpty()) return@coroutineScope next
                    }
                    first
                } finally {
                    
                    
                    jobs.forEach { it.cancel() }
                }
            }
        }

    






    private fun getAttempts(candidates: List<Pair<String, String>>): List<Pair<String, String>> {
        val result = LinkedHashSet<Pair<String, String>>()
        candidates.forEachIndexed { index, (artist, title) ->
            if (index == 0) {
                result += artistVariants(artist).map { it to title }
            } else {
                result += artist to title
            }
        }
        return result.take(MAX_GET_ATTEMPTS)
    }

    private fun artistVariants(artist: String): List<String> {
        if (artist.isBlank()) return emptyList()
        val result = LinkedHashSet<String>()
        result += artist
        result += artist.replace(", ", ",")
        result += artist.replace(",", ", ")
        val first = artist.substringBefore(',').substringBefore('/').trim()
        if (first.isNotBlank()) result += first
        return result.toList()
    }

    

    private suspend fun lrclibGet(artist: String, title: String): LyricsResult? {
        if (artist.isBlank()) return null
        val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("track_name", title)
            .build()
            .toString()
        val body = get(url) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (json.optBoolean("instrumental")) return null
        return toResult(json)
    }

    private suspend fun lrclibSearch(artist: String, title: String): LyricsResult? {
        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()
            .toString()
        val body = get(url) ?: return null
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return null

        val wantedTitle = title
        val wantedArtist = artist
        var best: LyricsResult? = null
        var bestScore = -1

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optBoolean("instrumental")) continue
            val candidate = toResult(item) ?: continue

            var score = resultScore(
                item.optString("artistName"),
                item.optString("trackName"),
                wantedArtist,
                wantedTitle,
            )
            
            if (score == 0) continue
            
            if (candidate.synced.isNotEmpty()) score += 1

            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun toResult(json: JSONObject): LyricsResult? {
        val plain = plainLyrics(json)
        val synced = syncedLyrics(json)
        if (plain == null && synced.isEmpty()) return null
        return LyricsResult(plain, synced)
    }

    private fun plainLyrics(json: JSONObject): String? {
        if (json.isNull("plainLyrics")) return null
        return json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
    }

    private fun syncedLyrics(json: JSONObject): List<LyricLine> {
        if (json.isNull("syncedLyrics")) return emptyList()
        return parseLrc(json.optString("syncedLyrics", ""))
    }

    

    private suspend fun lyricsOvh(artist: String, title: String): LyricsResult? {
        if (artist.isBlank()) return null
        val url = "https://api.lyrics.ovh/v1".toHttpUrl().newBuilder()
            .addPathSegment(artist)
            .addPathSegment(title)
            .build()
            .toString()
        val body = get(url) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (json.isNull("lyrics")) return null
        val text = json.optString("lyrics", "").takeIf { it.isNotBlank() } ?: return null
        return LyricsResult(text, emptyList())
    }

    

    private suspend fun genius(artist: String, title: String): LyricsResult? {
        val searchUrl = "https://genius.com/api/search/multi".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", "5")
            .addQueryParameter("q", listOf(artist, title).filter { it.isNotBlank() }.joinToString(" "))
            .build()
            .toString()
        val searchBody = get(searchUrl, browser = true) ?: return null
        val searchJson = runCatching { JSONObject(searchBody) }.getOrNull() ?: return null

        val songUrl = pickGeniusSong(searchJson, artist, title) ?: return null
        val page = get(songUrl, browser = true) ?: return null
        val text = parseGeniusLyrics(page) ?: return null
        return LyricsResult(text, emptyList())
    }

    private fun pickGeniusSong(json: JSONObject, artist: String, title: String): String? {
        val sections = json.optJSONObject("response")?.optJSONArray("sections") ?: return null
        val wantedTitle = title
        val wantedArtist = artist

        var bestUrl: String? = null
        var bestScore = -1
        for (i in 0 until sections.length()) {
            val hits = sections.optJSONObject(i)?.optJSONArray("hits") ?: continue
            for (j in 0 until hits.length()) {
                val hit = hits.optJSONObject(j) ?: continue
                if (hit.optString("type") != "song") continue
                val result = hit.optJSONObject("result") ?: continue
                val url = result.optString("url")
                if (!url.startsWith("https://genius.com/")) continue

                val score = resultScore(
                    result.optJSONObject("primary_artist")?.optString("name").orEmpty(),
                    result.optString("title"),
                    wantedArtist,
                    wantedTitle,
                )
                
                if (score == 0) continue
                if (score > bestScore) {
                    bestScore = score
                    bestUrl = url
                }
            }
        }
        return bestUrl
    }

    private fun parseGeniusLyrics(html: String): String? {
        val doc = Jsoup.parse(html)
        val containers = doc.select("div[data-lyrics-container=true]")
        if (containers.isEmpty()) return null

        val builder = StringBuilder()
        for (container in containers) {
            container.select("div[data-exclude-from-selection=true], script, style").remove()
            for (br in container.select("br")) {
                br.replaceWith(TextNode("\n"))
            }
            val text = container.wholeText()
                .replace("\u00A0", " ")
                .trim()
            if (text.isBlank()) continue
            if (builder.isNotEmpty()) builder.append("\n\n")
            builder.append(text)
        }
        return builder.toString().trim().takeIf { it.isNotBlank() }
    }

    

    



    private suspend fun safeFetch(block: suspend () -> LyricsResult?): LyricsResult? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private suspend fun get(url: String, browser: Boolean = false): String? {
        repeat(ATTEMPTS) { attempt ->
            if (browser) {
                
                if (geniusGate.isBlocked) return null
                geniusGate.awaitSuspend()
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
            val response = try {
                client.newCall(builder.build()).await()
            } catch (e: IOException) {
                
                if (!currentCoroutineContext().isActive) throw CancellationException("Отменено", e)
                return null
            }

            response.use { r ->
                if (r.isSuccessful) return r.body?.string()
                val code = r.code
                if (browser && (code == 429 || code == 403)) {
                    
                    geniusGate.block(code)
                    return null
                }
                
                if (code >= 500 && attempt < ATTEMPTS - 1) {
                    delay(BACKOFF_MS[attempt])
                    return@repeat
                }
                return null
            }
        }
        return null
    }

    



    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
    }

    private companion object {
        const val APP_UA = "Pixtone Music Player/1.0 (Android)"
        const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"

         
        const val ATTEMPTS = 2

         
        val BACKOFF_MS = longArrayOf(1_200L)

         
        const val MAX_GET_ATTEMPTS = 8

         
        const val GENIUS_HEAD_START_MS = 600L

         
        const val LYRICS_COVER_PAUSE_MS = 20_000L

         
        const val FIRST_RESULT_TIMEOUT_MS = 30_000L

         
        const val SYNC_GRACE_MS = 1_500L
    }
}
