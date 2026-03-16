package com.enderthor.kCustomField.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

object BackupFolderManager {
    private const val PREFS = "kcustomfield_prefs"
    private const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"

    fun persistBackupFolder(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_BACKUP_FOLDER_URI, uri.toString()).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error persisting backup folder uri")
        }
    }

    fun getPersistedBackupFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val s = prefs.getString(KEY_BACKUP_FOLDER_URI, null) ?: return null
        return try { Uri.parse(s) } catch (e: Exception) { null }
    }

    fun listFilesInFolder(context: Context): List<String> {
        val tree = getPersistedBackupFolderUri(context) ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val cr = context.contentResolver
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val list = mutableListOf<String>()
        cr.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                val mime = cursor.getString(mimeIdx) ?: ""
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                    list.add(name)
                }
            }
        }
        return list
    }

    suspend fun readFileFromFolder(context: Context, name: String): String? = withContext(Dispatchers.IO) {
        val tree = getPersistedBackupFolderUri(context) ?: return@withContext null
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val cr = context.contentResolver
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        cr.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            while (cursor.moveToNext()) {
                val found = cursor.getString(nameIdx)
                if (found == name) {
                    val docId = cursor.getString(idIdx)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                    return@withContext try {
                        cr.openInputStream(docUri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    } catch (e: IOException) {
                        Timber.e(e, "Error reading file from folder")
                        null
                    }
                }
            }
        }
        return@withContext null
    }

    suspend fun writeFileToFolder(context: Context, name: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tree = getPersistedBackupFolderUri(context) ?: return@withContext false
            val cr = context.contentResolver
            // createDocument returns a Uri for the newly created document
            val created = DocumentsContract.createDocument(cr, tree, "application/json", name) ?: return@withContext false
            cr.openOutputStream(created)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Error writing file to folder")
            return@withContext false
        }
    }
}
