package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Test

// El mapa de StickyStreamState es global: cada test usa su propia clave.
class StickyStreamStateTest {

    private val x = StreamState.Streaming(DataPoint(DataType.Type.HEART_RATE, mapOf(DataType.Field.SINGLE to 140.0)))

    @Test fun withinWindowReturnsCached() {
        StickyStreamState.process(x, "A_TEST", 7000)
        assertEquals(x, StickyStreamState.process(StreamState.NotAvailable, "A_TEST", 7000))
    }

    @Test fun expiredReturnsRawState() {
        StickyStreamState.process(x, "B_TEST")
        assertEquals(StreamState.NotAvailable, StickyStreamState.process(StreamState.NotAvailable, "B_TEST", stickyTimeoutMs = 0))
    }

    @Test fun searchingBridged() {
        StickyStreamState.process(x, "C_TEST", 7000)
        assertEquals(x, StickyStreamState.process(StreamState.Searching, "C_TEST", 7000))
    }

    @Test fun invalidateClears() {
        StickyStreamState.process(x, "D_TEST")
        StickyStreamState.invalidate("D_TEST")
        assertEquals(StreamState.NotAvailable, StickyStreamState.process(StreamState.NotAvailable, "D_TEST", 7000))
    }
}
