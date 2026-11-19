package com.vova.glyph.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.vova.glyph.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

object TrackShare {
    private const val SHARE_DIR = "share"
    private const val FILE_PREFIX = "track_"
    private const val STALE_MS = 60L * 60 * 1000
    private const val MAX_NAME_LENGTH = 60
    private const val ILLEGAL_CHARS = "\\/:*?\"<>|"

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "aiff", "aif",
    )

    suspend fun share(context: Context, track: Track): Boolean {
        if (track.isOnline) return false
        val shared = withContext(Dispatchers.IO) {
            runCatching { prepare(context, track) }.getOrNull()
        } ?: return false
        return withContext(Dispatchers.Main) {
            runCatching {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = shared.mime
                    putExtra(Intent.EXTRA_STREAM, shared.uri)
                    clipData = ClipData.newRawUri(track.effectiveTitle, shared.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(send, tr("Поделиться")).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }.isSuccess
        }
    }

    private fun prepare(context: Context, track: Track): SharedFile {
        val sourceUri = Uri.parse(track.uri)
        val mime = runCatching { context.contentResolver.getType(sourceUri) }.getOrNull()
        val extension = extensionOf(track.uri, mime)
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        cleanStale(dir)
        val target = File(
            dir,
            FILE_PREFIX + track.id + "_" + safeName(track.effectiveTitle) + "." + extension,
        )
        if (target.exists()) target.delete()
        val input = openSource(context, track.uri) ?: error("source is not readable")
        input.use { source ->
            target.outputStream().use { output -> source.copyTo(output) }
        }
        if (target.length() <= 0L) {
            target.delete()
            error("empty file")
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".artprovider",
            target,
        )
        return SharedFile(uri, mimeOf(extension))
    }

    private fun openSource(context: Context, uri: String): InputStream? = runCatching {
        when {
            uri.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uri))
            uri.startsWith("file://") -> Uri.parse(uri).path?.let { File(it).inputStream() }
            else -> File(uri).inputStream()
        }
    }.getOrNull()

    private fun cleanStale(dir: File) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith(FILE_PREFIX) && now - file.lastModified() > STALE_MS) {
                file.delete()
            }
        }
    }

    private fun safeName(title: String): String {
        val cleaned = title
            .map { ch -> if (ch in ILLEGAL_CHARS || ch.isISOControl()) '_' else ch }
            .joinToString("")
            .trim()
            .trim('.')
            .take(MAX_NAME_LENGTH)
        return cleaned.ifBlank { "track" }
    }

    private fun extensionOf(uri: String, mime: String?): String {
        val fromName = uri
            .substringAfterLast('/')
            .substringBefore('?')
            .substringAfterLast('.')
            .lowercase()
        if (fromName in AUDIO_EXTENSIONS) return fromName
        return when (mime?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/ogg", "audio/vorbis" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            else -> "mp3"
        }
    }

    private fun mimeOf(extension: String): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/wav"
        else -> "audio/*"
    }

    private data class SharedFile(val uri: Uri, val mime: String)
}
