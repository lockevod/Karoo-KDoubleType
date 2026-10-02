package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState

// Clave con nombre que convertValue lee para estos tipos: si falta, no hay dato (p.ej. sin ruta).
private val optionalKeys = mapOf(
    DataType.Type.DISTANCE_TO_DESTINATION to DataType.Field.DISTANCE_TO_DESTINATION,
    DataType.Type.ELEVATION_REMAINING to DataType.Field.ASCENT_REMAINING,
    DataType.Type.TIME_TO_DESTINATION to DataType.Field.TIME_TO_DESTINATION,
)

// Sin dato: Idle/NotAvailable, o Streaming sin la clave opcional. Searching/null NO cuentan
// (un dropout de sensor no debe hacer saltar el layout).
fun isEmptyMetric(state: StreamState?, action: KarooAction): Boolean = when (state) {
    is StreamState.NotAvailable, is StreamState.Idle -> true
    is StreamState.Streaming -> optionalKeys[action.action]?.let { it !in state.dataPoint.values } ?: false
    else -> false
}

// Índices a mostrar: todos si la opción está apagada, si todo está vacío o si quedarían menos de minVisible.
fun visibleIndices(hideEmpty: Boolean, empty: List<Boolean>, minVisible: Int): List<Int> {
    val visible = empty.indices.filter { !empty[it] }
    return if (!hideEmpty || visible.isEmpty() || visible.size < minVisible) empty.indices.toList() else visible
}
