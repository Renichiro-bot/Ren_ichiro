package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.CalendarEvent

@Entity(
    tableName = "calendar_events",
    indices = [
        Index(value = ["start_epoch_ms"]),
        Index(value = ["end_epoch_ms"])
    ]
)
data class CalendarEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description", defaultValue = "''")
    val description: String = "",

    @ColumnInfo(name = "start_epoch_ms")
    val startEpochMs: Long,

    @ColumnInfo(name = "end_epoch_ms")
    val endEpochMs: Long,

    @ColumnInfo(name = "is_all_day", defaultValue = "0")
    val isAllDay: Int = 0,

    @ColumnInfo(name = "associated_task_id")
    val associatedTaskId: String? = null,

    @ColumnInfo(name = "associated_note_id")
    val associatedNoteId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): CalendarEvent {
        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            isAllDay = isAllDay == 1,
            associatedTaskId = associatedTaskId,
            associatedNoteId = associatedNoteId,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = updatedAt
        )
    }

    companion object {
        fun fromDomainModel(event: CalendarEvent): CalendarEventEntity {
            return CalendarEventEntity(
                id = event.id,
                title = event.title,
                description = event.description,
                startEpochMs = event.startEpochMs,
                endEpochMs = event.endEpochMs,
                isAllDay = if (event.isAllDay) 1 else 0,
                associatedTaskId = event.associatedTaskId,
                associatedNoteId = event.associatedNoteId,
                createdAt = event.createdAtEpochMs,
                updatedAt = event.updatedAtEpochMs
            )
        }
    }
}
