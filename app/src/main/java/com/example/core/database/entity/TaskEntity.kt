package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Task
import com.example.core.model.TaskPriority

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["is_completed"]),
        Index(value = ["due_date_epoch_ms"]),
        Index(value = ["associated_note_id"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description", defaultValue = "''")
    val description: String = "",

    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Int = 0,

    @ColumnInfo(name = "priority", defaultValue = "'none'")
    val priority: String = "none",

    @ColumnInfo(name = "due_date_epoch_ms")
    val dueDateEpochMs: Long? = null,

    @ColumnInfo(name = "associated_note_id")
    val associatedNoteId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted == 1,
            priority = TaskPriority.fromId(priority),
            dueDateEpochMs = dueDateEpochMs,
            associatedNoteId = associatedNoteId,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = updatedAt
        )
    }

    companion object {
        fun fromDomainModel(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                isCompleted = if (task.isCompleted) 1 else 0,
                priority = task.priority.id,
                dueDateEpochMs = task.dueDateEpochMs,
                associatedNoteId = task.associatedNoteId,
                createdAt = task.createdAtEpochMs,
                updatedAt = task.updatedAtEpochMs
            )
        }
    }
}
