package com.example.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class NoteColorTheme(
    val id: String,
    val label: String
) {
    DEFAULT("default", "Default"),
    SAND("sand", "Warm Sand"),
    SAGE("sage", "Soft Sage"),
    OCEAN("ocean", "Muted Ocean"),
    LAVENDER("lavender", "Dusky Lavender"),
    TERRACOTTA("terracotta", "Terracotta");

    companion object {
        fun fromId(id: String): NoteColorTheme {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String = "",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val colorTheme: NoteColorTheme = NoteColorTheme.DEFAULT,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val wordCount: Int
        get() {
            val bodyWords = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val checkWords = checklistItems.sumOf { if (it.text.isBlank()) 0 else it.text.trim().split("\\s+".toRegex()).size }
            return bodyWords + checkWords
        }

    val previewText: String
        get() = if (content.isNotBlank()) content else checklistItems.filter { it.text.isNotBlank() }.joinToString(" • ") { it.text }

    val completedChecklistCount: Int
        get() = checklistItems.count { it.isDone }

    val formattedTimestamp: String
        get() {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return formatter.format(Date(updatedAtEpochMs))
        }

    // Retain compatibility with Level 2 views that inspect .timestamp
    val timestamp: String
        get() = formattedTimestamp
}

// Backward compatibility typealiases
typealias PrototypeNote = Note
typealias PrototypeChecklistItem = ChecklistItem
