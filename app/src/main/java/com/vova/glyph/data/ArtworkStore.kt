package com.vova.glyph.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

class ArtworkStore(context: Context) {
    private val appContext = context.applicationContext
    private val artDir = File(appContext.filesDir, "art").apply { mkdirs() }
    private val coverDir = File(appContext.filesDir, "covers").apply { mkdirs() }

    fun keyFor(value: String): String = Integer.toHexString(value.hashCode())

    fun saveEmbeddedArt(bytes: ByteArray, key: String): String? = runCatching {
        val file = File(artDir, "embedded_$key.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }.getOrNull()

    




    fun saveNetworkArt(bytes: ByteArray, key: String, source: String): String? = runCatching {
        val file = File(artDir, "net_${source}_$key.jpg")
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return@runCatching null
        val largest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (largest > MAX_ART_SIDE) {
            val ratio = MAX_ART_SIDE.toFloat() / largest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== bitmap) bitmap.recycle() }
        } else {
            bitmap
        }
        try {
            file.outputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }
        } finally {
            scaled.recycle()
        }
        file.absolutePath
    }.getOrNull()

    





    fun needsRemoteCover(path: String?, preferGenius: Boolean = true): Boolean {        if (path.isNullOrBlank()) return true
        if (path.startsWith("http://") || path.startsWith("https://")) return false
        val name = File(path).name
        if (name.startsWith("embedded_")) return true
        if (!preferGenius) return false
        return name.startsWith("net_deezer_") || name.startsWith("net_itunes_")
    }

    



    fun hasArtwork(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        if (path.startsWith("http://") || path.startsWith("https://")) return true
        return File(path).exists()
    }

     
    fun deleteReplacedArt(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.parentFile != artDir) return
        if (!file.name.startsWith("embedded_") && !file.name.startsWith("net_")) return
        runCatching { file.delete() }
    }

    fun copyPlaylistCover(uri: Uri): String? = runCatching {
        val mime = appContext.contentResolver.getType(uri).orEmpty()
        val extension = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val file = File(coverDir, "cover_${UUID.randomUUID()}.$extension")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        if (file.length() < 16) {
            file.delete()
            return null
        }
        file.absolutePath
    }.getOrNull()

    fun deleteCover(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() && it.parentFile == coverDir }?.delete() }
    }

    private companion object {
         
        const val MAX_ART_SIDE = 640
    }
}
