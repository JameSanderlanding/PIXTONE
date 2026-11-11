package com.vova.glyph.data

data class QuoteTextLayout(
    val fontSize: Float,
    val lines: List<String>,
    val lineHeight: Float,
) {
    val height: Float get() = lines.size * lineHeight
}

fun wrapQuoteText(
    text: String,
    maxWidth: Float,
    measure: (String) -> Float,
): List<String> {
    if (text.isEmpty()) return emptyList()
    if (maxWidth <= 0f) return text.split('\n')
    val result = mutableListOf<String>()
    for (source in text.split('\n')) {
        val words = source.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            result += ""
            continue
        }
        val line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (measure(candidate) <= maxWidth) {
                line.setLength(0)
                line.append(candidate)
                continue
            }
            if (line.isNotEmpty()) {
                result += line.toString()
                line.setLength(0)
            }
            if (measure(word) <= maxWidth) {
                line.append(word)
                continue
            }
            val chunks = splitLongWord(word, maxWidth, measure)
            chunks.dropLast(1).forEach { result += it }
            line.append(chunks.last())
        }
        if (line.isNotEmpty()) result += line.toString()
    }
    return result
}

fun fitQuoteText(
    text: String,
    maxWidth: Float,
    maxHeight: Float,
    maxFontSize: Float,
    minFontSize: Float,
    step: Float,
    lineHeightFactor: Float,
    measure: (String, Float) -> Float,
): QuoteTextLayout {
    var fontSize = maxFontSize
    while (fontSize > minFontSize) {
        val lines = wrapQuoteText(text, maxWidth) { measure(it, fontSize) }
        val lineHeight = fontSize * lineHeightFactor
        if (lines.size * lineHeight <= maxHeight) {
            return QuoteTextLayout(fontSize, lines, lineHeight)
        }
        fontSize = (fontSize - step).coerceAtLeast(minFontSize)
    }
    val lines = wrapQuoteText(text, maxWidth) { measure(it, minFontSize) }
    return QuoteTextLayout(minFontSize, lines, minFontSize * lineHeightFactor)
}

private fun splitLongWord(
    word: String,
    maxWidth: Float,
    measure: (String) -> Float,
): List<String> {
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    for (char in word) {
        current.append(char)
        if (current.length > 1 && measure(current.toString()) > maxWidth) {
            current.deleteCharAt(current.lastIndex)
            chunks += current.toString()
            current.setLength(0)
            current.append(char)
        }
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}
