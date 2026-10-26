package com.example.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startEpochMs: Long,
    val endEpochMs: Long = startEpochMs + 3600_000L,
    val isAllDay: Boolean = false,
    val associatedTaskId: String? = null,
    val associatedNoteId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val formattedTimeRange: String
        get() {
            if (isAllDay) return "All Day"
            val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            return "${timeFormatter.format(Date(startEpochMs))} – ${timeFormatter.format(Date(endEpochMs))}"
        }
}
