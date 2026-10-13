package com.enderthor.kCustomField

import android.content.Context
import android.util.Log
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log de diagnóstico a FICHERO, activable desde Ajustes generales y válido en release (el modo debug
 * no rinde igual y el buffer de logcat del Karoo, 256 KiB, no aguanta una salida). Mismo patrón que
 * KGhost: buffer en memoria drenado por una corrutina IO, sin I/O en el hilo que loguea.
 *
 * Qué escribe: las líneas de [Diag] (vía [write], NO vía Timber: proguard-rules.pro elimina
 * Timber.v/d/i/w en release) y, como Tree, lo que Timber deje pasar (en release solo ERROR).
 * Ficheros `logs/kdouble-<arranque>-<parte>.log`: rota a [MAX_FILE_BYTES] y se queda con los
 * [MAX_LOG_FILES] más recientes dentro de [MAX_TOTAL_BYTES].
 *
 * Coste apagado: una lectura volátil por llamada.
 */
object FileLogTree : Timber.Tree() {

    @Volatile
    var enabled: Boolean = false
        private set

    private const val MAX_BUFFER = 4000
    private const val MAX_LINE = 2000
    private const val FLUSH_INTERVAL_MS = 1_000L
    private const val MAX_LOG_FILES = 6
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 20L * 1024 * 1024

    private val buffer = ArrayDeque<String>()
    private var dropped = 0
    private val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
        Log.e("FileLogTree", "diagnostic log failure", e)
    })

    // Solo bajo writerLock: el bucle de flush y el handler de crash.
    private val writerLock = Any()
    private var appContext: Context? = null
    private var writer: BufferedWriter? = null
    private var fileBytes = 0L
    private var part = 0

    /** Sin I/O: el directorio se resuelve en el hilo IO al abrir el primer fichero. */
    fun start(context: Context) {
        val app = context.applicationContext
        appContext = app
        scope.launch {
            app.streamGeneralSettings()
                .retryWhen { e, _ -> Log.e("FileLogTree", "settings stream", e); delay(5_000); true }
                .collect { settings ->
                    val was = enabled
                    enabled = settings.isfilelogging
                    if (enabled && !was) Diag.onEnabled(app)
                    if (!enabled && was) { Diag.reset(); flush(); closeWriter() }
                    if (enabled) Diag.settings(settings)
                }
        }
        scope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                if (enabled) { Diag.checkStalls(); flush() }
            }
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            if (enabled) runCatching {
                write("CRASH thread=${thread.name}\n${Log.getStackTraceString(e)}")
                flush()
            }
            previous?.uncaughtException(thread, e)
        }
    }

    /** Añade una línea con marca de tiempo. Para [Diag]: llamar solo si [enabled]. */
    fun write(line: String) {
        if (!enabled) return
        synchronized(buffer) {
            if (buffer.size >= MAX_BUFFER) { buffer.removeFirst(); dropped++ }
            buffer.addLast(ts.format(Date()) + ' ' + line.take(MAX_LINE))
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = enabled && priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        write(if (t == null) message else message + '\n' + Log.getStackTraceString(t))
    }

    private fun flush() { synchronized(writerLock) {
        val lines = synchronized(buffer) {
            if (buffer.isEmpty() && dropped == 0) return
            val note = if (dropped > 0) "${ts.format(Date())} DROPPED $dropped lines (buffer full)" else null
            dropped = 0
            (buffer.toList() + listOfNotNull(note)).also { buffer.clear() }
        }
        runCatching {
            // Rota ANTES de pasar del tope, línea a línea (un lote lleno puede ocupar varios MB).
            for (line in lines) {
                val size = line.toByteArray(Charsets.UTF_8).size + 1L
                if (writer != null && fileBytes + size > MAX_FILE_BYTES) closeWriter()
                val w = writer ?: openNext() ?: return
                w.write(line); w.newLine()
                fileBytes += size
            }
            writer?.flush()
        }.onFailure {
            Log.e("FileLogTree", "write failed", it)
            closeWriter()
        }
    } }

    private fun openNext(): BufferedWriter? {
        val ctx = appContext ?: return null
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "logs").apply { mkdirs() }
        val file = File(dir, "kdouble-$stamp-${part++}.log")
        purgeOldLogs(dir, file)
        fileBytes = 0
        return FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).also { writer = it }
    }

    private fun closeWriter() = synchronized(writerLock) {
        runCatching { writer?.close() }
        writer = null
    }

    // Al abrir un fichero nuevo: los cerrados, más recientes primero, caben en lo que deja el
    // activo (que nunca pasa de MAX_FILE_BYTES). Así el total, activo incluido, ≤ MAX_TOTAL_BYTES.
    private fun purgeOldLogs(dir: File, active: File) {
        val logs = dir.listFiles { f -> f.name.endsWith(".log") && f != active } ?: return
        var total = 0L
        logs.sortedByDescending { it.lastModified() }.forEachIndexed { i, f ->
            total += f.length()
            if (i >= MAX_LOG_FILES - 1 || total > MAX_TOTAL_BYTES - MAX_FILE_BYTES) f.delete()
        }
    }
}
