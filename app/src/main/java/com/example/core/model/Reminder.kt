package com.example.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ReminderTargetType(val id: String) {
    TASK("task"),
    NOTE("note"),
    CALENDAR("calendar");

    companion object {
        fun fromId(id: String): ReminderTargetType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NOTE
        }
    }
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val targetType: ReminderTargetType = ReminderTargetType.TASK,
    val targetId: String,
    val title: String,
    val message: String = "",
    val triggerAtEpochMs: Long,
    val isEnabled: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val formattedDateTime: String
        get() {
            val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            return formatter.format(Date(triggerAtEpochMs))
        }

    val isPast: Boolean
        get() = triggerAtEpochMs < System.currentTimeMillis()
}
