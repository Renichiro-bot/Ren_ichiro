package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events ORDER BY start_epoch_ms ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY start_epoch_ms ASC")
    suspend fun getAllEventsSync(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE (start_epoch_ms <= :rangeEndMs) AND (end_epoch_ms >= :rangeStartMs) ORDER BY start_epoch_ms ASC")
    fun getEventsInRange(rangeStartMs: Long, rangeEndMs: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: String): CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity)

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}
