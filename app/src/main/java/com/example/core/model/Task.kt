package com.example.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TaskPriority(val id: String, val label: String) {
    NONE("none", "None"),
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");

    companion object {
        fun fromId(id: String): TaskPriority {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NONE
        }
    }
}

enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

enum class TaskSortOrder(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    TITLE("Title"),
    DATE_CREATED("Date Created")
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.NONE,
    val dueDateEpochMs: Long? = null,
    val associatedNoteId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val formattedDueDate: String?
        get() {
            val due = dueDateEpochMs ?: return null
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return formatter.format(Date(due))
        }

    val isOverdue: Boolean
        get() {
            val due = dueDateEpochMs ?: return false
            return !isCompleted && due < System.currentTimeMillis()
        }
}
