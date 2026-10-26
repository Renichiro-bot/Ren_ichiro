package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY trigger_at_epoch_ms ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY trigger_at_epoch_ms ASC")
    suspend fun getAllRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE is_enabled = 1 AND trigger_at_epoch_ms > :nowEpochMs ORDER BY trigger_at_epoch_ms ASC")
    fun getUpcomingActiveReminders(nowEpochMs: Long = System.currentTimeMillis()): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE is_enabled = 1 AND trigger_at_epoch_ms > :nowEpochMs ORDER BY trigger_at_epoch_ms ASC")
    suspend fun getUpcomingActiveRemindersList(nowEpochMs: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE target_id = :targetId")
    fun getRemindersForTarget(targetId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET is_enabled = :isEnabled WHERE id = :id")
    suspend fun setReminderEnabled(id: String, isEnabled: Int)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("DELETE FROM reminders WHERE target_id = :targetId")
    suspend fun deleteRemindersForTarget(targetId: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
