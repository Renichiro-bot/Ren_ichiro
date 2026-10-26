package com.example.core.data.repository

import com.example.core.database.dao.CalendarEventDao
import com.example.core.database.entity.CalendarEventEntity
import com.example.core.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface CalendarRepository {
    fun getAllEvents(): Flow<List<CalendarEvent>>
    fun getEventsInRange(rangeStartMs: Long, rangeEndMs: Long): Flow<List<CalendarEvent>>
    suspend fun getEventById(id: String): CalendarEvent?
    suspend fun createEvent(
        title: String,
        description: String = "",
        startEpochMs: Long,
        endEpochMs: Long = startEpochMs + 3600_000L,
        isAllDay: Boolean = false,
        associatedTaskId: String? = null,
        associatedNoteId: String? = null
    ): CalendarEvent
    suspend fun updateEvent(event: CalendarEvent)
    suspend fun deleteEvent(id: String)
}

class CalendarRepositoryImpl(
    private val calendarEventDao: CalendarEventDao
) : CalendarRepository {

    override fun getAllEvents(): Flow<List<CalendarEvent>> {
        return calendarEventDao.getAllEvents().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getEventsInRange(rangeStartMs: Long, rangeEndMs: Long): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsInRange(rangeStartMs, rangeEndMs).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getEventById(id: String): CalendarEvent? {
        return calendarEventDao.getEventById(id)?.toDomainModel()
    }

    override suspend fun createEvent(
        title: String,
        description: String,
        startEpochMs: Long,
        endEpochMs: Long,
        isAllDay: Boolean,
        associatedTaskId: String?,
        associatedNoteId: String?
    ): CalendarEvent {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Event title cannot be blank" }

        val event = CalendarEvent(
            id = UUID.randomUUID().toString(),
            title = trimmed,
            description = description.trim(),
            startEpochMs = startEpochMs,
            endEpochMs = if (endEpochMs >= startEpochMs) endEpochMs else startEpochMs + 3600_000L,
            isAllDay = isAllDay,
            associatedTaskId = associatedTaskId,
            associatedNoteId = associatedNoteId,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        calendarEventDao.insertEvent(CalendarEventEntity.fromDomainModel(event))
        return event
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        val trimmed = event.title.trim()
        require(trimmed.isNotBlank()) { "Event title cannot be blank" }
        calendarEventDao.updateEvent(CalendarEventEntity.fromDomainModel(event.copy(title = trimmed, updatedAtEpochMs = System.currentTimeMillis())))
    }

    override suspend fun deleteEvent(id: String) {
        calendarEventDao.deleteEvent(id)
    }
}
