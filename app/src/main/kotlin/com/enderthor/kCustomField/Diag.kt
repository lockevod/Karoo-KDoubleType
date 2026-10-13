package com.enderthor.kCustomField

import android.content.Context
import android.os.Build
import android.util.Log
import com.enderthor.kCustomField.datatype.GeneralSettings
import com.enderthor.kCustomField.datatype.KarooAction
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import java.util.concurrent.atomic.AtomicInteger

/**
 * Líneas de diagnóstico para el log a fichero ([FileLogTree]). Todas empiezan con un prefijo fijo:
 *
 *  BOOT      versión, modelo y firmware, al activar el log (o arrancar con él activo).
 *  SETTINGS  ajustes generales, cuando cambian.
 *  FIELD     START/STOP de cada vista (no preview), CONFIG con sus métricas cuando cambia, y
 *            ACTIVE para las vistas que ya estaban vivas al activar el log.
 *  RAWSTATE  estado que manda el Karoo, por suscripción (`POWER@s12`), solo cuando cambia
 *            (tipo + claves, sin valores).
 *  STATE     estado que llega al campo desde esa suscripción (tras sticky, reintentos y semillas),
 *            solo cuando cambia.
 *  HIDE      celdas visibles/vacías y el estado de cada una en un campo con "ocultar sin datos".
 *  RENDER    hay una actualización pendiente (entrada nueva tras el dedup) que lleva >30 s sin
 *            pintarse, y cuando por fin se pinta. Un campo con datos fijos no cuenta como parado.
 *  VALUE     valores crudos + unidades + valor convertido: primera muestra por métrica, luego cada
 *            5 min o al cambiar de unidades. Sin claves de posición (no se registra la ubicación).
 *  SUBEND    fin de una suscripción.
 *
 * Escribe directamente en [FileLogTree.write], no con Timber (R8 quita Timber.i en release).
 * El registro de vistas se mantiene aunque el log esté apagado (coste por ciclo de vida, no por
 * frame); el resto sale al instante si está apagado.
 */
object Diag {
    private const val STALL_MS = 30_000L
    private const val VALUE_EVERY_MS = 5 * 60_000L
    private const val PREVIEW_SOURCE = "kcustomfield"   // sourceId de los DataPoints de previewFlow
    private val LOCATION_KEY = Regex("LATITUDE|LONGITUDE")

    private class View(val detail: String) {
        var config: String? = null
        val render = RenderTracker()
    }

    private val lock = Any()
    private val views = HashMap<String, View>()
    private val lastLine = HashMap<String, String>()
    private val lastValue = HashMap<String, Pair<Long, UserProfile.PreferredUnit.UnitType>>()
    private val viewIds = AtomicInteger()
    private val subIds = AtomicInteger()

    private val on get() = FileLogTree.enabled

    // Sube en cada reset/enable: un SubTracker con otra generación vuelve a escribir su estado.
    @Volatile
    internal var generation = 0
        private set

    private fun emit(line: String) {
        FileLogTree.write(line)
        if (BuildConfig.DEBUG) Log.i("KDiag", line)
    }

    // Escribe solo si la línea de esta clave cambió.
    private fun changed(key: String, line: String) {
        val write = synchronized(lock) { lastLine.put(key, line) != line }
        if (write) emit(line)
    }

    /** Al apagar: olvida el dedup para que al volver a encender se reescriba todo. */
    fun reset() = synchronized(lock) {
        generation++
        lastLine.clear(); lastValue.clear()
        views.values.forEach { it.render.clear() }
    }

    fun onEnabled(context: Context) {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        generation++
        emit("BOOT v$version model=${Build.MODEL} fw=${Build.DISPLAY} sdk=${Build.VERSION.SDK_INT}")
        val active = synchronized(lock) {
            views.map { (k, v) -> "FIELD ACTIVE $k ${v.detail} ${v.config ?: ""}".trimEnd() }
                .also { views.values.forEach { it.render.clear() } }
        }
        active.forEach(::emit)
    }

    fun settings(s: GeneralSettings) = changed("SETTINGS", "SETTINGS $s")

    /** Clave única de una vista: tipo#índice/vN. Null en preview (el editor no interesa). */
    fun viewKey(type: String, index: Int, preview: Boolean): String? =
        if (preview) null else "$type#$index/v${viewIds.incrementAndGet()}"

    fun fieldStart(key: String?, detail: String) {
        if (key == null) return
        synchronized(lock) { views[key] = View(detail) }
        if (on) emit("FIELD START $key $detail")
    }

    fun fieldStop(key: String?) {
        if (key == null) return
        synchronized(lock) {
            views.remove(key)
            lastLine.remove("FIELD $key"); lastLine.remove("HIDE $key")
        }
        if (on) emit("FIELD STOP $key")
    }

    fun fieldConfig(key: String?, actions: List<KarooAction>, extra: String = "") {
        if (key == null) return
        val config = "${actions.map { it.name }} $extra".trimEnd()
        synchronized(lock) { views[key]?.config = config }
        changedForView(key, "FIELD $key", "FIELD CONFIG $key $config")
    }

    // Como changed(), pero solo si la vista sigue registrada (comprobado bajo el mismo lock que
    // fieldStop): un callback tardío tras el STOP no recrea la clave ni escribe después del STOP.
    private fun changedForView(key: String, dedupKey: String, line: String) {
        if (!on) return
        val write = synchronized(lock) { key in views && lastLine.put(dedupKey, line) != line }
        if (write) emit(line)
    }

    /** Entrada nueva tras el dedup: hay un repintado pendiente. */
    fun input(key: String?) {
        if (!on || key == null) return
        val now = System.currentTimeMillis()
        synchronized(lock) { views[key]?.render?.input(now) }
    }

    /** El render empieza con la última entrada que haya llegado hasta ahora (tras conflate). */
    fun renderStart(key: String?) {
        if (!on || key == null) return
        synchronized(lock) { views[key]?.render?.renderStart() }
    }

    fun rendered(key: String?) {
        if (!on || key == null) return
        val now = System.currentTimeMillis()
        // Solo vistas registradas: un render que termina después del cancel no resucita la clave.
        val resumedAfter = synchronized(lock) { views[key]?.render?.rendered(now) }
        if (resumedAfter != null) emit("RENDER $key rendered after ${resumedAfter / 1000}s pending")
    }

    fun checkStalls() {
        val now = System.currentTimeMillis()
        val stalled = synchronized(lock) {
            views.filter { (_, v) -> v.render.checkStall(now, STALL_MS) }
                .map { (k, v) -> "$k ${v.config ?: ""}".trimEnd() }
        }
        stalled.forEach { emit("RENDER $it update pending >${STALL_MS / 1000}s, not rendered") }
    }

    /** Seguimiento de una suscripción de getFieldFlow, etiquetada `POWER@s12`. */
    fun sub(action: String): SubTracker = SubTracker("$action@s${subIds.incrementAndGet()}")

    /**
     * RAWSTATE/STATE/SUBEND de una suscripción. Llega una emisión por métrica y segundo (≈100/s en
     * una página llena), así que compara tipo + claves sin montar texto ni tomar el lock global, y
     * solo construye la línea cuando cambia. Montar el texto en cada emisión costaba ~5 puntos de
     * CPU con el log encendido. Un solo colector por suscripción: sin concurrencia.
     */
    class SubTracker internal constructor(private val tag: String) {
        private val raw = Signature()
        private val out = Signature()
        private var wrote = false

        fun raw(state: Any) {
            if (on && raw.changed(state)) { wrote = true; emit("RAWSTATE $tag ${summary(state)}") }
        }

        fun state(state: Any) {
            if (on && out.changed(state)) { wrote = true; emit("STATE $tag ${summary(state)}") }
        }

        fun end() {
            if (on && wrote) emit("SUBEND $tag")
        }
    }

    internal class Signature {
        private var gen = -1
        private var type: Class<*>? = null
        private var keys: Set<String>? = null

        fun changed(state: Any): Boolean {
            val k = (state as? StreamState.Streaming)?.dataPoint?.values?.keys
            val g = generation
            if (g == gen && state.javaClass == type && k == keys) return false
            gen = g; type = state.javaClass; keys = k
            return true
        }
    }

    fun hide(key: String?, visible: List<Int>, empty: List<Boolean>, states: List<Any?>) {
        if (!on || key == null) return
        changedForView(
            key,
            "HIDE $key",
            "HIDE $key visible=$visible empty=${empty.map { if (it) 'T' else 'F' }} states=${states.map { it?.let(::stateName) }}"
        )
    }

    fun value(state: StreamState, action: KarooAction, unit: UserProfile.PreferredUnit.UnitType, value: Double, valueRight: Double) {
        if (!on || state !is StreamState.Streaming || state.dataPoint.sourceId == PREVIEW_SOURCE) return
        val now = System.currentTimeMillis()
        val due = synchronized(lock) {
            val last = lastValue[action.name]
            (last == null || last.second != unit || now - last.first >= VALUE_EVERY_MS)
                .also { if (it) lastValue[action.name] = now to unit }
        }
        if (!due) return
        val raw = state.dataPoint.values
            .filterKeys { !LOCATION_KEY.containsMatchIn(it) }
            .entries.joinToString(",", "{", "}") { (k, v) -> "${shortKey(k)}=$v" }
        val converted = if (action.powerField) "$value/$valueRight" else value.toString()
        emit("VALUE ${action.name} raw$raw units=$unit convert=${action.convert} -> $converted")
    }

    private fun shortKey(k: String) = k.removePrefix("FIELD_").removeSuffix("_ID")

    private fun stateName(state: Any): String = state::class.simpleName ?: "?"

    private fun summary(state: Any): String = when (state) {
        is StreamState.Streaming -> "Streaming${state.dataPoint.values.keys.map(::shortKey).sorted()}"
        else -> stateName(state)
    }
}

/**
 * Seguimiento "hay un dato que el campo aún no ha pintado" de una vista. Sin locks propios: Diag lo
 * usa bajo su lock. Las entradas se numeran; un render solo confirma las que había al EMPEZAR
 * ([renderStart]), así que una entrada que llega mientras se pinta la anterior sigue pendiente.
 */
internal class RenderTracker {
    private var inputSeq = 0L
    private var renderingSeq = 0L
    private var ackedSeq = 0L
    private var pendingSince: Long? = null
    private var lastInputAt = 0L
    private var stalled = false

    fun input(now: Long) {
        inputSeq++
        lastInputAt = now
        if (pendingSince == null) pendingSince = now
    }

    fun renderStart() { renderingSeq = inputSeq }

    /** Devuelve cuánto llevaba pendiente si estaba marcado como parado; null si no. */
    fun rendered(now: Long): Long? {
        val since = pendingSince
        ackedSeq = maxOf(ackedSeq, renderingSeq)
        // ponytail: si quedan entradas sin confirmar se toma como inicio la ÚLTIMA (no la primera
        // posterior al render); retrasa la detección como mucho lo que separe ambas entradas.
        pendingSince = if (ackedSeq >= inputSeq) null else lastInputAt
        if (!stalled || since == null) return null
        stalled = pendingSince != null
        return if (stalled) null else now - since
    }

    /** true solo la primera vez que se detecta el atasco. */
    fun checkStall(now: Long, stallMs: Long): Boolean {
        val since = pendingSince ?: return false
        if (stalled || now - since <= stallMs) return false
        stalled = true
        return true
    }

    fun clear() { ackedSeq = inputSeq; renderingSeq = inputSeq; pendingSince = null; stalled = false }
}
