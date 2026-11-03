package com.vova.glyph.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class MusicScanner(
    context: Context,
    private val artworkStore: ArtworkStore,
) {
    private val appContext = context.applicationContext

    data class ScanResult(
         
        val added: List<Track>,
        val scannedFiles: Int,
        val folderName: String,
         
        val newCount: Int = 0,
    )

    suspend fun scan(
        treeUri: Uri,
        existing: Map<String, Track>,
        onProgress: (scanned: Int, found: Int, current: String) -> Unit,
    ): ScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(appContext, treeUri)
            ?: throw IllegalStateException("Не удалось открыть папку")

        val docs = mutableListOf<DocumentFile>()
        collectAudio(root, docs, 0, coroutineContext)

        val result = mutableListOf<Track>()
        var scanned = 0
        var newCount = 0
        for (doc in docs) {
            coroutineContext.ensureActive()
            scanned++
            val uriString = doc.uri.toString()
            
            
            if (existing.containsKey(uriString)) {
                onProgress(scanned, result.size, doc.name.orEmpty())
                continue
            }
            onProgress(scanned, result.size, doc.name.orEmpty())
            val track = readTrack(doc) ?: continue
            result += track
            newCount++
        }
        onProgress(scanned, result.size, "")
        ScanResult(
            added = result,
            scannedFiles = docs.size,
            folderName = root.name?.takeIf { it.isNotBlank() } ?: "Музыка",
            newCount = newCount,
        )
    }

    private suspend fun collectAudio(
        dir: DocumentFile,
        out: MutableList<DocumentFile>,
        depth: Int,
        coroutineContext: kotlin.coroutines.CoroutineContext,
    ) {
        if (depth > 16) return
        val children = runCatching { dir.listFiles() }.getOrElse { emptyArray() }
        for (child in children) {
            coroutineContext.ensureActive()
            if (child.isDirectory) {
                if (!child.name.orEmpty().startsWith(".")) {
                    collectAudio(child, out, depth + 1, coroutineContext)
                }
            } else if (child.isFile && isAudio(child.name.orEmpty())) {
                out += child
            }
        }
    }

    private fun isAudio(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp3", "m4a", "flac", "wav", "ogg", "opus", "aac")
    }

    private fun readTrack(doc: DocumentFile): Track? {
        val uri = doc.uri
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val fallbackTitle = doc.name.orEmpty()
                .substringBeforeLast('.', doc.name.orEmpty())
                .trim()
                .ifBlank { "Без названия" }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim().orEmpty().ifBlank { fallbackTitle }
            val artist = (
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                )?.trim().orEmpty().ifBlank { "Неизвестный исполнитель" }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim().orEmpty()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                ?.equals("yes", ignoreCase = true) != false
            if (!hasAudio) return null
            val artwork = retriever.embeddedPicture?.let { bytes ->
                artworkStore.saveEmbeddedArt(bytes, artworkStore.keyFor(uri.toString()))
            }
            Track(
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                uri = uri.toString(),
                artwork = artwork,
                source = Track.SOURCE_LOCAL,
                fileSize = doc.length(),
            )
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
