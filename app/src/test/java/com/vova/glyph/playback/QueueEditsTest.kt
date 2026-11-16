package com.vova.glyph.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEditsTest {
    @Test
    fun `removes matching items from the end`() {
        val indices = QueueEdits.removalIndices(
            mediaIds = listOf("1", "2", "3", "4"),
            removing = setOf("2", "4"),
        )

        assertEquals(listOf(3, 1), indices)
    }

    @Test
    fun `next item after the deleted one keeps its place`() {
        val queue = listOf("1", "2", "3", "4")
        val indices = QueueEdits.removalIndices(queue, setOf("2"))

        val left = queue.filterIndexed { index, _ -> index !in indices }
        assertEquals(listOf("1", "3", "4"), left)
    }

    @Test
    fun `unknown ids do not touch the queue`() {
        assertTrue(QueueEdits.removalIndices(listOf("1", "2"), setOf("9")).isEmpty())
    }

    @Test
    fun `empty queue gives no indices`() {
        assertTrue(QueueEdits.removalIndices(emptyList(), setOf("1")).isEmpty())
    }

    @Test
    fun `removing everything returns every index in reverse`() {
        val indices = QueueEdits.removalIndices(listOf("1", "2"), setOf("1", "2"))

        assertEquals(listOf(1, 0), indices)
    }
}
