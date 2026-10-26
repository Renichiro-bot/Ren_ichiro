package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Reminder
import com.example.core.model.ReminderTargetType

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["trigger_at_epoch_ms"]),
        Index(value = ["is_enabled"]),
        Index(value = ["target_id"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "target_type")
    val targetType: String,

    @ColumnInfo(name = "target_id")
    val targetId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message", defaultValue = "''")
    val message: String = "",

    @ColumnInfo(name = "trigger_at_epoch_ms")
    val triggerAtEpochMs: Long,

    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    val isEnabled: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Reminder {
        return Reminder(
            id = id,
            targetType = ReminderTargetType.fromId(targetType),
            targetId = targetId,
            title = title,
            message = message,
            triggerAtEpochMs = triggerAtEpochMs,
            isEnabled = isEnabled == 1,
            createdAtEpochMs = createdAt
        )
    }

    companion object {
        fun fromDomainModel(reminder: Reminder): ReminderEntity {
            return ReminderEntity(
                id = reminder.id,
                targetType = reminder.targetType.id,
                targetId = reminder.targetId,
                title = reminder.title,
                message = reminder.message,
                triggerAtEpochMs = reminder.triggerAtEpochMs,
                isEnabled = if (reminder.isEnabled) 1 else 0,
                createdAt = reminder.createdAtEpochMs
            )
        }
    }
}
