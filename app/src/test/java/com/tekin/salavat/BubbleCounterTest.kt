package com.tekin.salavat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleCounterTest {
    private val colors = BubbleColors(0, 0, 0, 0, 0, 0, 0)
    private fun counter(count: Int = 0, target: Int = 33, onTarget: String = "round", every: Int = 33) =
        BubbleCounter("x", "Zikir", count, target, onTarget, every, finish = true, sound = false, colors = colors)

    @Test fun tapCountsAndRemembersTime() {
        val c = counter()
        assertEquals(BubbleCounter.Event.COUNTED, c.tap(100))
        assertEquals(1, c.count)
        assertEquals(listOf(100L), c.taps)
    }

    @Test fun roundStartsAgainAtTarget() {
        val c = counter(count = 32)
        assertEquals(BubbleCounter.Event.COMPLETED, c.tap(1))
        assertEquals(0, c.count)
    }

    @Test fun stopModeBlocksAfterTarget() {
        val c = counter(count = 32, onTarget = "stop")
        assertEquals(BubbleCounter.Event.COMPLETED, c.tap(1))
        assertEquals(33, c.count)
        assertEquals(BubbleCounter.Event.BLOCKED, c.tap(2))
        assertEquals(1, c.taps.size)
    }

    @Test fun milestoneEveryN() {
        val c = counter(count = 9, target = 100, every = 10)
        assertEquals(BubbleCounter.Event.MILESTONE, c.tap(1))
    }

    @Test fun undoOnlyOwnTaps() {
        val c = counter(count = 5)
        assertFalse(c.undo())
        c.tap(1); c.tap(2)
        assertTrue(c.undo())
        assertEquals(6, c.count)
        assertEquals(listOf(1L), c.taps)
    }

    @Test fun undoAcrossRound() {
        val c = counter(count = 32)
        c.tap(1)
        assertTrue(c.undo())
        assertEquals(32, c.count)
    }

    @Test fun takeEmptiesPending() {
        val c = counter()
        c.tap(1); c.tap(2)
        assertEquals(listOf(1L, 2L), c.take())
        assertTrue(c.taps.isEmpty())
        assertEquals(2, c.count)
    }

    @Test fun updateReplaysPendingOnNewBase() {
        val c = counter(count = 0, target = 100, every = 0)
        c.tap(1); c.tap(2)
        c.update(10, 100)     // uygulamada 10'a gelindi
        assertEquals(12, c.count)
        assertEquals(listOf(1L, 2L), c.taps)
    }
}
