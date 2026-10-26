package com.example.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.example.core.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class AttachmentStorage(private val context: Context) {

    private val attachmentsBaseDir: File
        get() = File(context.filesDir, "attachments").also { if (!it.exists()) it.mkdirs() }

    /**
     * Copies content from selected Uri into private internal storage.
     * Never stores raw paths or allows path traversal.
     */
    suspend fun saveAttachmentFromUri(
        noteId: String,
        sourceUri: Uri
    ): Result<Attachment> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val originalName = queryFileName(sourceUri) ?: "attachment_${System.currentTimeMillis()}"
            val sanitizedName = sanitizeFileName(originalName)
            val mimeType = contentResolver.getType(sourceUri) ?: getMimeTypeFromExtension(sanitizedName)
            val attachmentId = UUID.randomUUID().toString()

            val noteDir = File(attachmentsBaseDir, sanitizeFileName(noteId)).also {
                if (!it.exists()) it.mkdirs()
            }

            val targetFile = File(noteDir, "${attachmentId}_$sanitizedName")
            var bytesCopied = 0L

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for URI: $sourceUri"))

            val relativePath = "attachments/${sanitizeFileName(noteId)}/${targetFile.name}"
            val attachment = Attachment(
                id = attachmentId,
                noteId = noteId,
                fileName = originalName,
                mimeType = mimeType,
                fileSizeBytes = bytesCopied,
                localRelativePath = relativePath,
                createdAtEpochMs = System.currentTimeMillis()
            )
            Result.success(attachment)
        } catch (e: Exception) {
            Log.e("AttachmentStorage", "Failed to save attachment", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves the actual File object in internal storage.
     */
    fun getFile(attachment: Attachment): File {
        return File(context.filesDir, attachment.localRelativePath)
    }

    /**
     * Checks if the attachment file exists on disk.
     */
    fun exists(attachment: Attachment): Boolean {
        return getFile(attachment).exists()
    }

    /**
     * Generates a secure content URI using FileProvider to share or open with external viewer apps.
     */
    fun getShareableUri(attachment: Attachment): Uri? {
        val file = getFile(attachment)
        if (!file.exists()) return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("AttachmentStorage", "Failed to get FileProvider URI for ${file.path}", e)
            null
        }
    }

    /**
     * Creates an ACTION_VIEW Intent with read permissions to open the attachment.
     */
    fun createViewIntent(attachment: Attachment): Intent? {
        val uri = getShareableUri(attachment) ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Safely deletes an attachment file from internal storage.
     */
    suspend fun deleteFile(attachment: Attachment): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getFile(attachment)
            if (file.exists()) {
                val deleted = file.delete()
                val parentDir = file.parentFile
                if (parentDir != null && parentDir.isDirectory && parentDir.listFiles()?.isEmpty() == true) {
                    parentDir.delete()
                }
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("AttachmentStorage", "Failed to delete file for attachment: ${attachment.id}", e)
            false
        }
    }

    /**
     * Deletes all attachment files for a specified note.
     */
    suspend fun deleteFilesForNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            val noteDir = File(attachmentsBaseDir, sanitizeFileName(noteId))
            if (noteDir.exists()) {
                noteDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("AttachmentStorage", "Failed to delete attachment directory for note: $noteId", e)
        }
    }

    /**
     * Queries the original display name from the ContentResolver if possible.
     */
    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AttachmentStorage", "Could not query display name for URI: $uri", e)
            }
        }
        return uri.lastPathSegment
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trimStart('.')
            .ifEmpty { "file" }
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
