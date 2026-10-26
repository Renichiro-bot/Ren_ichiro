package com.example.core.model

data class Attachment(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val localRelativePath: String,
    val createdAtEpochMs: Long
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    val isPdf: Boolean
        get() = mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)

    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "0 B"
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$fileSizeBytes B"
            }
        }
}
