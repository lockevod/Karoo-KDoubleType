package com.enderthor.kCustomField

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagSignatureTest {

    private fun streaming(vararg keys: String) =
        StreamState.Streaming(DataPoint("TYPE_POWER_ID", keys.associateWith { 1.0 }))

    @Test
    fun `same type and keys with new values is not a change`() {
        val sig = Diag.Signature()
        assertTrue(sig.changed(streaming("FIELD_POWER_ID")))
        assertFalse(sig.changed(StreamState.Streaming(DataPoint("TYPE_POWER_ID", mapOf("FIELD_POWER_ID" to 250.0)))))
    }

    @Test
    fun `different keys or state type is a change`() {
        val sig = Diag.Signature()
        sig.changed(streaming("FIELD_DISTANCE_TO_DESTINATION_ID", "FIELD_ON_ROUTE_ID"))
        assertTrue(sig.changed(streaming("FIELD_ON_ROUTE_ID")))
        assertTrue(sig.changed(StreamState.NotAvailable))
        assertFalse(sig.changed(StreamState.NotAvailable))
        assertTrue(sig.changed(StreamState.Searching))
    }
}
