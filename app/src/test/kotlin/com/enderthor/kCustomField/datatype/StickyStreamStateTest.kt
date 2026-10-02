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

    // StickyEmitter: el dedup va DESPUÉS del sticky. Si se dedupe el estado crudo, el segundo
    // NotAvailable nunca llega a process() y el valor caducado se queda congelado.
    @Test fun repeatedNotAvailableExpiresThroughDedup() {
        val e = StickyEmitter("E_TEST", isStickyExtStream = false, extStickyTimeoutMs = 15000, stickyTimeoutMs = 0)
        assertEquals(x, e.next(x))
        assertEquals(StreamState.NotAvailable, e.next(StreamState.NotAvailable))
        assertEquals(null, e.next(StreamState.NotAvailable))
    }

    // El congelamiento real: el 1er NotAvailable cae dentro de la ventana (cacheado = duplicado)
    // y el 2º, ya caducado, tiene que salir. Con dedup sobre el crudo el 2º se descartaba.
    @Test fun repeatedNotAvailableUnfreezesAfterWindow() {
        val e = StickyEmitter("I_TEST", isStickyExtStream = false, extStickyTimeoutMs = 15000, stickyTimeoutMs = 200)
        assertEquals(x, e.next(x))
        assertEquals(null, e.next(StreamState.NotAvailable))
        Thread.sleep(300)
        assertEquals(StreamState.NotAvailable, e.next(StreamState.NotAvailable))
    }

    @Test fun withinWindowDuplicateSuppressed() {
        val e = StickyEmitter("F_TEST", isStickyExtStream = false, extStickyTimeoutMs = 15000, stickyTimeoutMs = 7000)
        assertEquals(x, e.next(x))
        assertEquals(null, e.next(StreamState.NotAvailable))
    }

    // Re-suscripción: emisor nuevo → el re-seed con el mismo valor vuelve a pasar.
    @Test fun freshEmitterReemitsSameValue() {
        assertEquals(x, StickyEmitter("G_TEST", false, 15000).next(x))
        assertEquals(x, StickyEmitter("G_TEST", false, 15000).next(x))
    }

    @Test fun extStreamIdleInvalidates() {
        val e = StickyEmitter("H_TEST", isStickyExtStream = true, extStickyTimeoutMs = 15000)
        e.next(x)
        assertEquals(StreamState.Idle, e.next(StreamState.Idle))
    }
}
