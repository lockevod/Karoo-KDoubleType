package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyMetricTest {

    private fun streaming(type: String, values: Map<String, Double>) =
        StreamState.Streaming(DataPoint(type, values))

    @Test fun notAvailableIsEmpty() =
        assertTrue(isEmptyMetric(StreamState.NotAvailable, KarooAction.DISTANCE_REMAIN))

    @Test fun idleIsEmpty() = assertTrue(isEmptyMetric(StreamState.Idle, KarooAction.HR))

    @Test fun searchingIsNotEmpty() = assertFalse(isEmptyMetric(StreamState.Searching, KarooAction.HR))

    @Test fun nullIsNotEmpty() = assertFalse(isEmptyMetric(null, KarooAction.HR))

    @Test fun streamingWithKeyIsNotEmpty() = assertFalse(
        isEmptyMetric(
            streaming(DataType.Type.DISTANCE_TO_DESTINATION, mapOf(DataType.Field.DISTANCE_TO_DESTINATION to 1234.0)),
            KarooAction.DISTANCE_REMAIN
        )
    )

    @Test fun zeroWithKeyIsNotEmpty() = assertFalse(
        isEmptyMetric(
            streaming(DataType.Type.DISTANCE_TO_DESTINATION, mapOf(DataType.Field.DISTANCE_TO_DESTINATION to 0.0)),
            KarooAction.DISTANCE_REMAIN
        )
    )

    @Test fun streamingMissingOptionalKeyIsEmpty() {
        assertTrue(
            isEmptyMetric(
                streaming(DataType.Type.DISTANCE_TO_DESTINATION, mapOf(DataType.Field.ON_ROUTE to 0.0)),
                KarooAction.DISTANCE_REMAIN
            )
        )
        assertTrue(
            isEmptyMetric(
                streaming(DataType.Type.ELEVATION_REMAINING, mapOf(DataType.Field.ON_ROUTE to 0.0)),
                KarooAction.ELEV_REMAIN
            )
        )
    }

    @Test fun streamingOtherTypeIsNotEmpty() = assertFalse(
        isEmptyMetric(streaming(DataType.Type.HEART_RATE, mapOf(DataType.Field.SINGLE to 140.0)), KarooAction.HR)
    )

    @Test fun routeMetrics() {
        assertTrue(isRouteMetric(DataType.Type.DISTANCE_TO_DESTINATION))
        assertTrue(isRouteMetric(DataType.Type.TIME_TO_DESTINATION))
        assertTrue(isRouteMetric(DataType.Type.ELEVATION_REMAINING))
        assertFalse(isRouteMetric(DataType.Type.HEART_RATE))
        assertFalse(isRouteMetric(DataType.Type.DISTANCE))
    }

    @Test fun visibleIndicesOffReturnsAll() =
        assertEquals(listOf(0, 1), visibleIndices(false, listOf(false, true), 1))

    @Test fun visibleIndicesHidesEmpty() =
        assertEquals(listOf(0, 2), visibleIndices(true, listOf(false, true, false), 1))

    @Test fun visibleIndicesAllEmptyReturnsAll() =
        assertEquals(listOf(0, 1), visibleIndices(true, listOf(true, true), 1))

    @Test fun visibleIndicesRespectsMin() {
        assertEquals(listOf(0, 1), visibleIndices(true, listOf(false, true), 2))
        assertEquals(listOf(0, 1, 2), visibleIndices(true, listOf(false, true, true), 2))
    }
}
