package com.enderthor.kCustomField

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderTrackerTest {

    private val stall = 30_000L

    @Test
    fun `unchanged field with no input never stalls`() {
        val t = RenderTracker()
        t.input(0); t.renderStart(); t.rendered(100)
        assertFalse(t.checkStall(1_000_000, stall))
    }

    @Test
    fun `input that is never rendered stalls once`() {
        val t = RenderTracker()
        t.input(0)
        assertFalse(t.checkStall(stall, stall))
        assertTrue(t.checkStall(stall + 1, stall))
        assertFalse(t.checkStall(stall + 2, stall))
    }

    @Test
    fun `input arriving during a render stays pending after that render completes`() {
        val t = RenderTracker()
        t.input(0)          // A
        t.renderStart()     // A empieza a pintarse
        t.input(10)         // B llega mientras A se pinta
        t.rendered(20)      // termina A
        assertTrue(t.checkStall(10 + stall + 1, stall))
    }

    @Test
    fun `stalled render reports how long it was pending when it finally renders`() {
        val t = RenderTracker()
        t.input(0)
        assertTrue(t.checkStall(stall + 1, stall))
        t.renderStart()
        assertEquals(40_000L, t.rendered(40_000))
        assertFalse(t.checkStall(100_000, stall))
    }

    @Test
    fun `clear forgets pending input`() {
        val t = RenderTracker()
        t.input(0)
        t.clear()
        assertFalse(t.checkStall(1_000_000, stall))
        t.renderStart()
        assertNull(t.rendered(1_000_001))
    }
}
