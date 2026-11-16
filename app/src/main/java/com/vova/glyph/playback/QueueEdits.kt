package com.vova.glyph.playback

internal object QueueEdits {
    fun removalIndices(mediaIds: List<String>, removing: Set<String>): List<Int> =
        mediaIds.indices
            .filter { mediaIds[it] in removing }
            .sortedDescending()
}
