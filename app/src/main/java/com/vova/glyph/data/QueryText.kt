package com.vova.glyph.data

 
private val ARTIST_TITLE_SPLIT = Regex("\\s+[-–—|]\\s+")








internal fun cleanText(raw: String): String {
    val cleaned = raw
        .replace(Regex("\\([^)]*\\)"), " ")      
        .replace(Regex("\\[[^\\]]*\\]"), " ")    
        .replace(Regex("（[^）]*）"), " ")         
        .replace(Regex("【[^】]*】"), " ")         
        .replace(Regex("(?i)\\s+-\\s+(remaster(ed)?|official.*|lyrics?|audio|video|mv|clip)$"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    
    return cleaned.ifBlank { raw.replace(Regex("\\s+"), " ").trim() }
}





internal fun cleanTitleForSearch(raw: String): String =
    cleanText(raw)
        .replace("_", "")
        .replace(Regex("\\s+"), " ")
        .trim()





internal fun cleanArtistForSearch(raw: String): String =
    cleanText(raw.replace('_', ' '))






internal fun transliteratedCandidate(artist: String, title: String): Pair<String, String>? {
    val artistNeeds = hasLatinLetters(artist) && !hasCyrillicLetters(artist)
    val titleNeeds = hasLatinLetters(title) && !hasCyrillicLetters(title)
    if (!artistNeeds && !titleNeeds) return null

    val newTitle = if (titleNeeds) transliterateToCyrillic(title) else title
    if (newTitle.isBlank()) return null
    val newArtist = if (artistNeeds) transliterateToCyrillic(artist) else artist
    return newArtist to newTitle
}






internal fun buildSearchCandidatesWithTranslit(
    artist: String,
    title: String,
): List<Pair<String, String>> {
    val result = LinkedHashSet(buildSearchCandidates(artist, title))
    transliteratedCandidate(artist, title)?.let { result += it }
    return result.toList()
}

private fun hasLatinLetters(value: String): Boolean =
    value.any { it in 'a'..'z' || it in 'A'..'Z' }

private fun hasCyrillicLetters(value: String): Boolean =
    value.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }





private fun transliterateToCyrillic(value: String): String {
    val lower = value.lowercase()
    val out = StringBuilder(lower.length)
    var index = 0
    while (index < lower.length) {
        val ch = lower[index]
        var consumed = 0
        if (ch in 'a'..'z') {
            for (length in 4 downTo 1) {
                if (index + length > lower.length) continue
                val chunk = lower.substring(index, index + length)
                val russian = when {
                    
                    chunk == "c" ->
                        if (index + 1 < lower.length && lower[index + 1] in "eiy") "ц" else "к"

                    else -> TRANSLIT[chunk]
                }
                if (russian != null) {
                    out.append(russian)
                    consumed = length
                    break
                }
            }
        }
        if (consumed == 0) {
            out.append(ch)
            consumed = 1
        }
        index += consumed
    }
    return out.toString()
        .replace(Regex("\\s+"), " ")
        .trim()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

 
private val TRANSLIT: Map<String, String> = mapOf(
    "shch" to "щ",
    "sch" to "щ",
    "tch" to "ч",
    "dzh" to "дж",
    "zh" to "ж",
    "kh" to "х",
    "sh" to "ш",
    "ch" to "ч",
    "ts" to "ц",
    "ya" to "я",
    "yu" to "ю",
    "yo" to "ё",
    "ye" to "е",
    "ja" to "я",
    "ju" to "ю",
    "ph" to "ф",
    "th" to "т",
    "ck" to "к",
    "qu" to "к",
    "oo" to "у",
    "ee" to "и",
    "a" to "а",
    "b" to "б",
    "d" to "д",
    "e" to "е",
    "f" to "ф",
    "g" to "г",
    "h" to "х",
    "i" to "и",
    "j" to "дж",
    "k" to "к",
    "l" to "л",
    "m" to "м",
    "n" to "н",
    "o" to "о",
    "p" to "п",
    "q" to "к",
    "r" to "р",
    "s" to "с",
    "t" to "т",
    "u" to "у",
    "v" to "в",
    "w" to "в",
    "x" to "кс",
    "y" to "й",
    "z" to "з",
)

 
internal fun splitArtistTitle(value: String): Pair<String, String>? {
    val match = ARTIST_TITLE_SPLIT.find(value) ?: return null
    val left = value.substring(0, match.range.first).trim()
    val right = value.substring(match.range.last + 1).trim()
    if (left.isBlank() || right.isBlank()) return null
    return left to right
}








internal fun buildSearchCandidates(artist: String, title: String): List<Pair<String, String>> {
    val result = LinkedHashSet<Pair<String, String>>()

    fun add(a: String, t: String) {
        val cleanArtist = a.trim()
        val cleanTitle = t.trim()
        if (cleanTitle.isNotBlank()) result += cleanArtist to cleanTitle
    }

    add(artist, title)

    val titleSplit = splitArtistTitle(title)
    if (titleSplit != null) {
        add(titleSplit.first, titleSplit.second)
    }

    val artistSplit = splitArtistTitle(artist)
    if (artistSplit != null) {
        add(artistSplit.first, artistSplit.second)
    }

    
    if (titleSplit != null) {
        add(titleSplit.second, titleSplit.first)
    }

    return result.toList()
}





internal fun matchScore(candidate: String, wanted: String, weight: Int): Int {
    val a = artistKey(candidate)
    val b = artistKey(wanted)
    if (a.isBlank() || b.isBlank()) return 0
    return when {
        a == b -> weight + 2
        a.contains(b) || b.contains(a) -> weight
        else -> 0
    }
}

 
private fun artistKey(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

 
internal const val UNKNOWN_ARTIST = "Неизвестный исполнитель"

 
internal fun isUnknownArtist(value: String): Boolean =
    value.isBlank() || value.trim().equals(UNKNOWN_ARTIST, ignoreCase = true)










internal fun resultScore(
    candidateArtist: String,
    candidateTitle: String,
    wantedArtist: String,
    wantedTitle: String,
): Int {
    val titleScore = titleMatchScore(candidateTitle, wantedTitle)
    if (titleScore == 0) return 0

    if (isUnknownArtist(wantedArtist)) return titleScore

    val artistScore = matchScore(
        cleanText(candidateArtist),
        cleanText(wantedArtist),
        ARTIST_WEIGHT,
    )
    if (artistScore == 0) return 0

    return titleScore + artistScore
}

 
internal fun titlesMatch(candidate: String, wanted: String): Boolean =
    titleMatchScore(candidate, wanted) > 0









private fun titleMatchScore(candidate: String, wanted: String): Int {
    val a = titleKey(candidate)
    val b = titleKey(wanted)
    if (a.isEmpty() || b.isEmpty()) return 0
    if (a == b) return TITLE_WEIGHT + 4

    val shortest = minOf(a.length, b.length)
    if (shortest < 4) return 0
    val allowed = if (maxOf(a.length, b.length) >= 12) 2 else 1
    return if (editDistance(a, b, allowed) <= allowed) TITLE_WEIGHT + 2 else 0
}

private fun titleKey(value: String): String =
    cleanText(value)
        .lowercase()
        .replace('ё', 'е')
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")

 
private fun editDistance(a: String, b: String, limit: Int): Int {
    if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)
    for (i in 1..a.length) {
        current[0] = i
        var rowMin = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + cost,
            )
            if (current[j] < rowMin) rowMin = current[j]
        }
        if (rowMin > limit) return limit + 1
        val swap = previous
        previous = current
        current = swap
    }
    return previous[b.length]
}

private const val TITLE_WEIGHT = 3
private const val ARTIST_WEIGHT = 2
