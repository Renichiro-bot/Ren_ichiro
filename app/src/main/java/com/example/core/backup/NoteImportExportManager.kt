package com.example.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.core.data.repository.NoteRepository
import com.example.core.model.ChecklistItem
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ImportPreview(
    val suggestedTitle: String,
    val body: String,
    val isChecklist: Boolean,
    val checklistItems: List<ChecklistItem>,
    val characterCount: Int,
    val lineCount: Int,
    val rawText: String
)

class NoteImportExportManager(
    private val context: Context,
    private val noteRepository: NoteRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Exports a note as a clean Markdown (.md) document to the chosen URI.
     */
    suspend fun exportNoteAsMarkdown(
        note: Note,
        destinationUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()

            // Header title
            sb.append("# ${note.title.ifBlank { "Untitled Note" }}\n\n")

            // Metadata block
            if (note.tags.isNotEmpty()) {
                val tagStrings = note.tags.joinToString(" ") { "#${it.replace(" ", "_")}" }
                sb.append("> Tags: $tagStrings\n")
            }
            sb.append("> Created: ${dateFormat.format(Date(note.createdAtEpochMs))}\n")
            sb.append("> Last Updated: ${dateFormat.format(Date(note.updatedAtEpochMs))}\n\n")

            // Main body
            if (note.content.isNotBlank()) {
                sb.append(note.content).append("\n\n")
            }

            // Checklist items if present
            if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                sb.append("---\n\n### Checklist\n")
                note.checklistItems.forEach { item ->
                    val checkMark = if (item.isDone) "x" else " "
                    sb.append("- [$checkMark] ${item.text}\n")
                }
                sb.append("\n")
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                outStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                outStream.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Could not open destination output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NoteImportExport", "Failed to export markdown", e)
            Result.failure(e)
        }
    }

    /**
     * Exports a note as a plain text (.txt) document.
     */
    suspend fun exportNoteAsPlainText(
        note: Note,
        destinationUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()

            sb.append(note.title.ifBlank { "Untitled Note" }).append("\n")
            sb.append("=".repeat(40)).append("\n\n")

            if (note.content.isNotBlank()) {
                sb.append(note.content).append("\n\n")
            }

            if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                sb.append("Checklist:\n")
                note.checklistItems.forEach { item ->
                    val status = if (item.isDone) "[X]" else "[ ]"
                    sb.append("$status ${item.text}\n")
                }
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                outStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                outStream.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Could not open destination output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NoteImportExport", "Failed to export plain text", e)
            Result.failure(e)
        }
    }

    /**
     * Creates an ACTION_SEND Intent for instant sharing of the note.
     */
    fun createShareIntent(note: Note): Intent {
        val sb = StringBuilder()
        sb.append(note.title.ifBlank { "Untitled Note" }).append("\n\n")
        if (note.content.isNotBlank()) {
            sb.append(note.content).append("\n\n")
        }
        if (note.isChecklist && note.checklistItems.isNotEmpty()) {
            note.checklistItems.forEach { item ->
                val check = if (item.isDone) "☑" else "☐"
                sb.append("$check ${item.text}\n")
            }
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note from Vellum" })
            putExtra(Intent.EXTRA_TEXT, sb.toString().trim())
        }
    }

    /**
     * Inspects and parses an external text/markdown file for preview before importing.
     * Enforces a 5MB upper bound for safety.
     */
    suspend fun parseImportFile(sourceUri: Uri): Result<ImportPreview> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for URI: $sourceUri"))

            val text = inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                // Guard against gigantic files (>5MB)
                val buffer = CharArray(1024 * 1024 * 5)
                val read = reader.read(buffer)
                if (read == -1) "" else String(buffer, 0, read)
            }

            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Selected file is empty"))
            }

            val lines = text.lines()
            var suggestedTitle = "Imported Note"
            var titleLineIndex = -1

            // Detect title from first non-empty line
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.isNotBlank()) {
                    suggestedTitle = if (line.startsWith("# ")) {
                        line.removePrefix("# ").trim()
                    } else if (line.startsWith("## ")) {
                        line.removePrefix("## ").trim()
                    } else if (line.length <= 80) {
                        line
                    } else {
                        line.take(60) + "..."
                    }
                    titleLineIndex = i
                    break
                }
            }

            // Check for markdown checklist items: - [ ] or - [x]
            val checklistRegex = Regex("""^-\s*\[([ xX])\]\s*(.*)$""")
            val detectedChecklistItems = mutableListOf<ChecklistItem>()
            val bodyLines = mutableListOf<String>()

            for (i in lines.indices) {
                if (i == titleLineIndex && (lines[i].trim().startsWith("# ") || lines[i].trim().length <= 80)) {
                    continue // Skip title line in body
                }
                val line = lines[i]
                val match = checklistRegex.find(line.trim())
                if (match != null) {
                    val isChecked = match.groupValues[1].lowercase() == "x"
                    val itemText = match.groupValues[2].trim()
                    if (itemText.isNotBlank()) {
                        detectedChecklistItems.add(
                            ChecklistItem(
                                id = UUID.randomUUID().toString(),
                                text = itemText,
                                isDone = isChecked
                            )
                        )
                    }
                } else {
                    bodyLines.add(line)
                }
            }

            val isChecklist = detectedChecklistItems.isNotEmpty() && detectedChecklistItems.size >= bodyLines.filter { it.isNotBlank() }.size
            val cleanBody = bodyLines.joinToString("\n").trim()

            val preview = ImportPreview(
                suggestedTitle = suggestedTitle,
                body = cleanBody,
                isChecklist = isChecklist,
                checklistItems = detectedChecklistItems,
                characterCount = text.length,
                lineCount = lines.size,
                rawText = text
            )

            Result.success(preview)
        } catch (e: Exception) {
            Log.e("NoteImportExport", "Failed to parse import file", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new note in Room from an approved import preview.
     */
    suspend fun importNoteFromPreview(
        preview: ImportPreview,
        targetFolderId: String? = null
    ): Result<Note> = withContext(Dispatchers.IO) {
        try {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = preview.suggestedTitle,
                content = preview.body,
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis(),
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                colorTheme = NoteColorTheme.DEFAULT,
                isChecklist = preview.isChecklist,
                checklistItems = preview.checklistItems,
                isFavorite = false,
                folderId = targetFolderId
            )
            noteRepository.saveNote(newNote)
            Result.success(newNote)
        } catch (e: Exception) {
            Log.e("NoteImportExport", "Failed to save imported note", e)
            Result.failure(e)
        }
    }
}
