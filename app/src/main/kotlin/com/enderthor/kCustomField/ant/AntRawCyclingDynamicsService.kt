package com.enderthor.kCustomField.ant

import android.content.Context
import com.dsi.ant.channel.AntChannel
import com.dsi.ant.channel.AntChannelProvider
import com.dsi.ant.channel.ChannelNotAvailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dsi.ant.AntService
import com.dsi.ant.message.fromant.BroadcastDataMessage

/**
 * Servicio para escuchar mensajes crudos ANT+ y extraer Cycling Dynamics (PCO, Power Phase, etc.)
 * usando la API de canal de android_antlib. Compatible con Garmin y Favero.
 */
class AntRawCyclingDynamicsService(private val context: Context) {
    private var antChannelProvider: AntChannelProvider? = null
    private var antChannel: AntChannel? = null

    // Métricas Cycling Dynamics
    private val _pcoLeft = MutableStateFlow<Double?>(null)
    val pcoLeft: StateFlow<Double?> = _pcoLeft.asStateFlow()
    private val _pcoRight = MutableStateFlow<Double?>(null)
    val pcoRight: StateFlow<Double?> = _pcoRight.asStateFlow()
    private val _powerPhaseLeft = MutableStateFlow<Int?>(null)
    val powerPhaseLeft: StateFlow<Int?> = _powerPhaseLeft.asStateFlow()
    private val _powerPhaseRight = MutableStateFlow<Int?>(null)
    val powerPhaseRight: StateFlow<Int?> = _powerPhaseRight.asStateFlow()
    private val _powerPhasePeakLeft = MutableStateFlow<Int?>(null)
    val powerPhasePeakLeft: StateFlow<Int?> = _powerPhasePeakLeft.asStateFlow()
    private val _powerPhasePeakRight = MutableStateFlow<Int?>(null)
    val powerPhasePeakRight: StateFlow<Int?> = _powerPhasePeakRight.asStateFlow()
    private val _riderPosition = MutableStateFlow<Int?>(null)
    val riderPosition: StateFlow<Int?> = _riderPosition.asStateFlow()

    fun start() {
        try {
            antChannelProvider = AntChannelProvider.getProvider(context, true)
            antChannel = antChannelProvider?.acquireChannel(context, AntChannelProvider.ChannelType.RECEIVE_ONLY)
            antChannel?.setChannelConfiguration(11, 0, 0, 57, 8182, false) // DeviceType, TxType, Pair, Freq, Period, lowPrio
            antChannel?.open()
            antChannel?.setChannelRxListener { message ->
                parseRawMessage(message.messageContent)
            }
        } catch (e: ChannelNotAvailableException) {
            // Manejar error, ej: ANT no disponible
        }
    }

    fun stop() {
        antChannel?.release()
        antChannel = null
        antChannelProvider?.release()
        antChannelProvider = null
    }

    private fun parseRawMessage(msg: ByteArray) {
        // Página 0x2E (46 decimal): Cycling Dynamics
        if (msg.size > 6 && msg[0] == 0x2E.toByte()) {
            val pedalPower = msg[2].toInt() and 0xFF
            val pco = msg[3].toInt().toByte().toInt() // signed
            val powerPhase = msg[4].toInt() and 0xFF
            val powerPhasePeak = msg[5].toInt() and 0xFF
            val riderPos = msg[6].toInt() and 0xFF

            // PCO: distinguir izquierda/derecha según pedalPower
            when (pedalPower) {
                0x01 -> _pcoLeft.value = pco.toDouble()
                0x02 -> _pcoRight.value = pco.toDouble()
            }
            // Power Phase: distinguir izquierda/derecha según pedalPower
            when (pedalPower) {
                0x01 -> {
                    _powerPhaseLeft.value = if (powerPhase != 255) powerPhase else null
                    _powerPhasePeakLeft.value = if (powerPhasePeak != 255) powerPhasePeak else null
                }
                0x02 -> {
                    _powerPhaseRight.value = if (powerPhase != 255) powerPhase else null
                    _powerPhasePeakRight.value = if (powerPhasePeak != 255) powerPhasePeak else null
                }
            }
            // Rider Position (solo si no es inválido)
            if (riderPos != 255) _riderPosition.value = riderPos
        }
    }
}
