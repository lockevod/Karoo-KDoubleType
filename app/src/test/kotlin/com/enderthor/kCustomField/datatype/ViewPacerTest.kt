package com.enderthor.kCustomField.datatype

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewPacerTest {

    private var clock = 0L
    private val pacer = ViewPacer { clock }

    @Test
    fun `first update never waits`() {
        clock = 5_000
        assertEquals(0L, pacer.waitMs())
    }

    @Test
    fun `a burst waits until 950 ms after the last accepted update`() {
        clock = 10_000; pacer.mark()
        clock = 10_300
        assertEquals(650L, pacer.waitMs())
    }

    @Test
    fun `steady 1 Hz input with compose time never waits so latency does not drift`() {
        // Entrada cada 1000 ms, composición de 100 ms: el updateView sale a T+100 cada ciclo.
        var t = 0L
        repeat(10) {
            clock = t + 100
            assertEquals(0L, pacer.waitMs())
            pacer.mark()
            t += 1_000
        }
    }
}
