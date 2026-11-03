package com.vova.glyph.data

import android.content.Context
import kotlinx.coroutines.delay
















class GeniusGate(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var blockedUntil: Long
        get() = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_BLOCKED_UNTIL, value).apply()

    private var lastRequestAt: Long
        get() = prefs.getLong(KEY_LAST_REQUEST, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_REQUEST, value).apply()

    @Volatile
    private var coversPausedUntil = 0L

     
    val isBlocked: Boolean
        get() = System.currentTimeMillis() < blockedUntil

    



    val isPausedForCovers: Boolean
        get() = System.currentTimeMillis() < coversPausedUntil

     
    val coversPauseLeftMs: Long
        get() = (coversPausedUntil - System.currentTimeMillis()).coerceAtLeast(0L)

     
    fun pauseCovers(durationMs: Long = COVER_PAUSE_MS) {
        coversPausedUntil = maxOf(coversPausedUntil, System.currentTimeMillis() + durationMs)
    }

    




    fun resumeCovers() {
        coversPausedUntil = 0L
    }

     
    val blockedForMs: Long
        get() = (blockedUntil - System.currentTimeMillis()).coerceAtLeast(0L)

     
    fun await() {
        val wait = reserveSlot()
        if (wait > 0) runCatching { Thread.sleep(wait) }
    }

     
    suspend fun awaitSuspend() {
        val wait = reserveSlot()
        if (wait > 0) delay(wait)
    }

    



    private fun reserveSlot(): Long = synchronized(this) {
        val now = System.currentTimeMillis()
        val slot = maxOf(now, lastRequestAt + MIN_INTERVAL_MS)
        lastRequestAt = slot
        slot - now
    }

     
    @Synchronized
    fun block(code: Int) {
        val duration = if (code == 403) BLOCK_AFTER_403_MS else BLOCK_AFTER_429_MS
        blockedUntil = maxOf(blockedUntil, System.currentTimeMillis() + duration)
    }

    private companion object {
        const val PREFS_NAME = "genius_gate"
        const val KEY_BLOCKED_UNTIL = "blocked_until"
        const val KEY_LAST_REQUEST = "last_request_at"

         
        const val MIN_INTERVAL_MS = 2_200L

         
        const val BLOCK_AFTER_429_MS = 3 * 60_000L

         
        const val BLOCK_AFTER_403_MS = 10 * 60_000L

         
        const val COVER_PAUSE_MS = 10_000L
    }
}
