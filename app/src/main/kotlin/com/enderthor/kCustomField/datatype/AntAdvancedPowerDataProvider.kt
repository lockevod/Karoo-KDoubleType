package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf

/**
 * Proveedor de datos para métricas ANT+ avanzadas de power meter
 */
class AntAdvancedPowerDataProvider(
    private val context: Context,
    private val karooSystem: KarooSystemService,
    private val scope: CoroutineScope
) {

    private val antService = AntPowerMeterService(context)

    init {
        // Intentar conectar al power meter automáticamente
        connectToPowerMeter()
    }

    private fun connectToPowerMeter() {
        antService.connectToPowerMeter()
    }

    /**
     * Obtiene el stream de datos para Platform Center Offset Left
     */
    fun streamPcoLeft(): Flow<StreamState> {
        return antService.pcoLeft.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-pco-left",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para Platform Center Offset Right
     */
    fun streamPcoRight(): Flow<StreamState> {
        return antService.pcoRight.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-pco-right",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para Rider Position
     */
    fun streamRiderPosition(): Flow<StreamState> {
        return antService.riderPosition.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> {
                    // Mapear la posición a un valor Double para cumplir con la firma esperada
                    val doubleValue = when (value) {
                        "Aero" -> 1.0
                        "Normal" -> 2.0
                        "Relaxed" -> 3.0
                        else -> 0.0
                    }
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId = "ant-rider-position",
                            values = mapOf(DataType.Field.SINGLE to doubleValue)
                        )
                    )
                }
            }
        }
    }

    /**
     * Obtiene el stream de datos para Power Phase Left
     */
    fun streamPowerPhaseLeft(): Flow<StreamState> {
        return antService.powerPhaseLeft.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-power-phase-left",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para Power Phase Right
     */
    fun streamPowerPhaseRight(): Flow<StreamState> {
        return antService.powerPhaseRight.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-power-phase-right",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para Peak Power Phase Left
     */
    fun streamPeakPowerPhaseLeft(): Flow<StreamState> {
        return antService.peakPowerPhaseLeft.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-peak-power-phase-left",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para Peak Power Phase Right
     */
    fun streamPeakPowerPhaseRight(): Flow<StreamState> {
        return antService.peakPowerPhaseRight.map { value ->
            when (value) {
                null -> StreamState.Searching
                else -> StreamState.Streaming(
                    DataPoint(
                        dataTypeId = "ant-peak-power-phase-right",
                        values = mapOf(DataType.Field.SINGLE to value)
                    )
                )
            }
        }
    }

    /**
     * Obtiene el stream de datos para cualquier métrica ANT+ personalizada
     */
    fun getStreamForDataType(dataTypeId: String): Flow<StreamState> {
        return when (dataTypeId) {
            "ant-pco-left" -> streamPcoLeft()
            "ant-pco-right" -> streamPcoRight()
            "ant-rider-position" -> streamRiderPosition()
            "ant-power-phase-left" -> streamPowerPhaseLeft()
            "ant-power-phase-right" -> streamPowerPhaseRight()
            "ant-peak-power-phase-left" -> streamPeakPowerPhaseLeft()
            "ant-peak-power-phase-right" -> streamPeakPowerPhaseRight()
            else -> flowOf(StreamState.NotAvailable)
        }
    }

    /**
     * Verifica si el servicio ANT+ está conectado
     */
    fun isConnected(): Boolean {
        return antService.isConnected()
    }

    /**
     * Desconecta el servicio ANT+
     */
    fun disconnect() {
        antService.disconnect()
    }
}
