package com.enderthor.kCustomField.manager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

object BackupStorage {

    private const val BACKUPS_FOLDER = "backups"

    fun getBackupsDir(context: Context): File? {
        val dir = context.getExternalFilesDir(BACKUPS_FOLDER)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun writeBackupAtomic(context: Context, filename: String, content: String, writeMeta: Boolean = true): File? = withContext(Dispatchers.IO) {
        try {
            val backupsDir = getBackupsDir(context) ?: throw IOException("No backups dir")
            val tmp = File(backupsDir, "$filename.tmp")
            FileOutputStream(tmp).use { out ->
                val bytes = content.toByteArray(Charsets.UTF_8)
                out.write(bytes)
                out.fd.sync()
                out.flush()
            }
            val dest = File(backupsDir, filename)
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                throw IOException("Failed to rename backup tmp to final")
            }

            if (writeMeta) {
                // compute checksum
                val checksum = sha256OfFile(dest)
                // write simple meta file
                val meta = File(backupsDir, "$filename.meta.json")
                val now = SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US).format(Date())
                val metaJson = "{" +
                        "\"filename\":\"${dest.name}\"," +
                        "\"createdAt\":\"$now\"," +
                        "\"checksum\":\"$checksum\"" +
                        "}"
                meta.writeText(metaJson, Charsets.UTF_8)
            }
            return@withContext dest
        } catch (e: Exception) {
            Timber.e(e, "Error writing atomic backup")
            return@withContext null
        }
    }

    fun listBackups(context: Context): List<File> {
        val dir = getBackupsDir(context) ?: return emptyList()
        return dir.listFiles { f -> f.isFile && !f.name.endsWith(".meta.json") && !f.name.endsWith(".tmp") }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }

    fun readBackup(context: Context, filename: String): String? {
        val dir = getBackupsDir(context) ?: return null
        val f = File(dir, filename)
        return try {
            if (!f.exists()) return null
            f.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error reading backup")
            null
        }
    }

    private fun sha256OfFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } > 0) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
