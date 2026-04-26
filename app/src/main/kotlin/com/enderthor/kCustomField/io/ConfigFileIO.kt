package com.enderthor.kCustomField.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

suspend fun writeStringToUri(context: Context, uri: Uri, text: String) {
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: throw IOException("No se pudo abrir OutputStream para URI: $uri")
        } catch (e: Exception) {
            Timber.e(e, "Error escribiendo en URI $uri")
            throw e
        }
    }
}

suspend fun readStringFromUri(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                String(input.readBytes(), Charsets.UTF_8)
            } ?: throw IOException("No se pudo abrir InputStream para URI: $uri")
        } catch (e: Exception) {
            Timber.e(e, "Error leyendo URI $uri")
            throw e
        }
    }
}
