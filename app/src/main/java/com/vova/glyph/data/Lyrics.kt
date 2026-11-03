package com.vova.glyph.data

 
data class LyricLine(
    val timeMs: Long,
    val text: String,
     
    val instrumental: Boolean = false,
)

 
const val INTRO_MARKER = "♪ интро ♪"
const val GAP_MARKER = "♪ отыгрыш ♪"










fun withGapMarkers(
    lines: List<LyricLine>,
    durationMs: Long = 0L,
    minGapMs: Long = 8_000L,
): List<LyricLine> {
    if (lines.isEmpty()) return lines
    val sorted = lines.sortedBy { it.timeMs }
    val result = ArrayList<LyricLine>(sorted.size + 4)

    if (sorted.first().timeMs >= minGapMs) {
        result += LyricLine(0L, INTRO_MARKER, instrumental = true)
    }

    for (line in sorted) {
        val previous = result.lastOrNull()
        if (previous != null && !previous.instrumental &&
            line.timeMs - previous.timeMs >= minGapMs
        ) {
            
            val at = (previous.timeMs + 2_500L).coerceAtMost(line.timeMs - 500L)
            if (at > previous.timeMs) result += LyricLine(at, GAP_MARKER, instrumental = true)
        }
        result += line
    }

    val last = result.lastOrNull()
    if (last != null && !last.instrumental && durationMs > 0 &&
        durationMs - last.timeMs >= minGapMs
    ) {
        val at = last.timeMs + 2_500L
        if (at < durationMs) result += LyricLine(at, GAP_MARKER, instrumental = true)
    }

    return result
}

 
data class LyricsResult(
    val plain: String?,
    val synced: List<LyricLine> = emptyList(),
)








data class CachedLyrics(
    val plain: String?,
    val synced: List<LyricLine> = emptyList(),
    val found: Boolean = true,
    val fetchedAt: Long = 0L,
)

 
fun List<LyricLine>.toLrc(): String = joinToString("\n") { line ->
    val minutes = line.timeMs / 60_000
    val seconds = (line.timeMs % 60_000) / 1_000
    val centis = (line.timeMs % 1_000) / 10
    "[%02d:%02d.%02d]%s".format(minutes, seconds, centis, line.text)
}

private val LRC_TIME = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")





fun parseLrc(lrc: String): List<LyricLine> {
    if (lrc.isBlank()) return emptyList()
    val result = mutableListOf<LyricLine>()
    for (raw in lrc.lineSequence()) {
        val matches = LRC_TIME.findAll(raw).toList()
        if (matches.isEmpty()) continue
        val text = raw.substring(matches.last().range.last + 1).trim()
        if (text.isEmpty()) continue
        for (match in matches) {
            val minutes = match.groupValues[1].toLongOrNull() ?: continue
            val seconds = match.groupValues[2].toLongOrNull() ?: continue
            val fraction = match.groupValues[3]
            val millis = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            result += LyricLine(minutes * 60_000 + seconds * 1_000 + millis, text)
        }
    }
    return result.sortedBy { it.timeMs }
}

 
fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.size - 1
    var answer = -1
    while (low <= high) {
        val mid = (low + high) / 2
        if (lines[mid].timeMs <= positionMs) {
            answer = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return answer
}
