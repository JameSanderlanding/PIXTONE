package com.vova.glyph.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val PROXY_BODY_TYPE = "application/json; charset=utf-8".toMediaType()

internal object OnlineProxy {
    fun isAllowed(target: String): Boolean =
        OnlineEndpoints.PROXY_TARGETS.any { target.startsWith(it) }

    fun envelope(target: String): String = JSONObject()
        .put("url", target)
        .put("method", "GET")
        .put("headers", JSONObject().put("User-Agent", OnlineEndpoints.USER_AGENT))
        .toString()

    fun requestBody(target: String): RequestBody = envelope(target).toRequestBody(PROXY_BODY_TYPE)

    fun unwrap(payload: String): String {
        val envelope = runCatching { JSONObject(payload) }
            .getOrElse { throw IOException("Каталог вернул непонятный ответ") }
        val status = envelope.optInt("status", 0)
        if (status != 200) throw IOException("HTTP $status")
        val body = envelope.optString("body")
        if (body.isBlank()) throw IOException("Каталог вернул пустой ответ")
        return body
    }
}

class OnlineTransport(private val prefs: Prefs) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun getJson(url: HttpUrl): String = withContext(Dispatchers.IO) {
        val target = url.toString()
        if (!OnlineProxy.isAllowed(target)) throw IOException("Адрес не разрешён")
        var lastError: IOException? = null
        for (attempt in 1..ATTEMPTS) {
            try {
                return@withContext fetch(url)
            } catch (e: IOException) {
                lastError = e
                if (attempt < ATTEMPTS) delay(RETRY_DELAY_MS)
            }
        }
        throw lastError ?: IOException("Каталог недоступен")
    }

    private fun fetch(url: HttpUrl): String {
        val proxy = prefs.onlineProxy.trim()
        if (proxy.isBlank()) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", OnlineEndpoints.USER_AGENT)
                .get()
                .build()
            return execute(request)
        }
        val request = Request.Builder()
            .url(proxy)
            .post(OnlineProxy.requestBody(url.toString()))
            .build()
        return OnlineProxy.unwrap(execute(request))
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        body
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 12L

        const val READ_TIMEOUT_SECONDS = 20L

        const val CALL_TIMEOUT_SECONDS = 25L

        const val ATTEMPTS = 2

        const val RETRY_DELAY_MS = 1_000L
    }
}
