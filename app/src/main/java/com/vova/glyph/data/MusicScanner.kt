package com.vova.glyph.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
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

        val pending = docs.filterNot { existing.containsKey(it.uri.toString()) }
        val scanned = AtomicInteger(docs.size - pending.size)
        val found = AtomicInteger(0)
        val results = arrayOfNulls<Track>(pending.size)
        val reportLock = Mutex()
        var lastReportAt = 0L

        suspend fun report(force: Boolean, name: String) {
            reportLock.withLock {
                val now = System.currentTimeMillis()
                if (!force && now - lastReportAt < PROGRESS_INTERVAL_MS) return
                lastReportAt = now
                onProgress(scanned.get(), found.get(), name)
            }
        }

        val lanes = minOf(METADATA_WORKERS, pending.size)
        coroutineScope {
            repeat(lanes) { lane ->
                launch {
                    var index = lane
                    while (index < pending.size) {
                        coroutineContext.ensureActive()
                        val doc = pending[index]
                        val track = runCatching { readTrack(doc) }.getOrNull()
                        if (track != null) {
                            results[index] = track
                            found.incrementAndGet()
                        }
                        scanned.incrementAndGet()
                        report(force = false, name = doc.name.orEmpty())
                        index += lanes
                    }
                }
            }
        }

        val added = results.filterNotNull()
        report(force = true, name = "")
        ScanResult(
            added = added,
            scannedFiles = docs.size,
            folderName = root.name?.takeIf { it.isNotBlank() } ?: "Музыка",
            newCount = added.size,
        )
    }

    suspend fun embeddedArtworkFor(uri: String): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, Uri.parse(uri))
            val bytes = retriever.embeddedPicture ?: return@withContext null
            artworkStore.saveEmbeddedArt(bytes, artworkStore.keyFor(uri))
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
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
            } else if (child.isFile && AudioFormats.isAudio(child.name.orEmpty())) {
                out += child
            }
        }
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

    private companion object {
        const val METADATA_WORKERS = 4

        const val PROGRESS_INTERVAL_MS = 120L
    }
}
