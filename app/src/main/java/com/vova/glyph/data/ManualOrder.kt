package com.vova.glyph.data

object ManualOrder {
    const val LIBRARY = "library"
    const val FAVORITES = "favorites"
    const val ALBUM = "album"

    fun positions(ids: List<Long>): Map<Long, Int> =
        ids.withIndex().associate { (index, id) -> id to index }

    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from == to) return items
        if (from !in items.indices || to !in items.indices) return items
        val result = items.toMutableList()
        val item = result.removeAt(from)
        result.add(to, item)
        return result
    }
}
