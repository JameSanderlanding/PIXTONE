package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ManualOrderTest {
    @Test
    fun `moves an item down`() {
        val moved = ManualOrder.move(listOf("a", "b", "c", "d"), from = 0, to = 2)

        assertEquals(listOf("b", "c", "a", "d"), moved)
    }

    @Test
    fun `moves an item up to the top`() {
        val moved = ManualOrder.move(listOf("a", "b", "c"), from = 2, to = 0)

        assertEquals(listOf("c", "a", "b"), moved)
    }

    @Test
    fun `same position keeps the very same list`() {
        val items = listOf("a", "b", "c")

        assertSame(items, ManualOrder.move(items, from = 1, to = 1))
    }

    @Test
    fun `out of range does nothing`() {
        val items = listOf("a", "b")

        assertSame(items, ManualOrder.move(items, from = 5, to = 0))
        assertSame(items, ManualOrder.move(items, from = 0, to = -1))
    }

    @Test
    fun `positions follow the list index`() {
        val positions = ManualOrder.positions(listOf(10L, 20L, 30L))

        assertEquals(mapOf(10L to 0, 20L to 1, 30L to 2), positions)
    }
}
