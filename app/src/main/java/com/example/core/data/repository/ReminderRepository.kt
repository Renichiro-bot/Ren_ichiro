package com.example.core.data.repository

import android.content.Context
import com.example.core.database.dao.ReminderDao
import com.example.core.database.entity.ReminderEntity
import com.example.core.model.Reminder
import com.example.core.model.ReminderTargetType
import com.example.core.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getUpcomingActiveReminders(): Flow<List<Reminder>>
    fun getRemindersForTarget(targetId: String): Flow<List<Reminder>>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun createReminder(
        targetType: ReminderTargetType,
        targetId: String,
        title: String,
        message: String = "",
        triggerAtEpochMs: Long
    ): Reminder
    suspend fun setReminderEnabled(id: String, isEnabled: Boolean)
    suspend fun deleteReminder(id: String)
    suspend fun deleteRemindersForTarget(targetId: String)
}

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val context: Context
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getUpcomingActiveReminders(): Flow<List<Reminder>> {
        return reminderDao.getUpcomingActiveReminders().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getRemindersForTarget(targetId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForTarget(targetId).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getReminderById(id: String): Reminder? {
        return reminderDao.getReminderById(id)?.toDomainModel()
    }

    override suspend fun createReminder(
        targetType: ReminderTargetType,
        targetId: String,
        title: String,
        message: String,
        triggerAtEpochMs: Long
    ): Reminder {
        val reminder = Reminder(
            id = UUID.randomUUID().toString(),
            targetType = targetType,
            targetId = targetId,
            title = title.trim(),
            message = message.trim(),
            triggerAtEpochMs = triggerAtEpochMs,
            isEnabled = true,
            createdAtEpochMs = System.currentTimeMillis()
        )
        reminderDao.insertReminder(ReminderEntity.fromDomainModel(reminder))
        ReminderScheduler.scheduleReminder(context, reminder)
        return reminder
    }

    override suspend fun setReminderEnabled(id: String, isEnabled: Boolean) {
        val reminder = reminderDao.getReminderById(id)?.toDomainModel() ?: return
        reminderDao.setReminderEnabled(id, if (isEnabled) 1 else 0)
        if (isEnabled) {
            ReminderScheduler.scheduleReminder(context, reminder.copy(isEnabled = true))
        } else {
            ReminderScheduler.cancelReminder(context, id)
        }
    }

    override suspend fun deleteReminder(id: String) {
        ReminderScheduler.cancelReminder(context, id)
        reminderDao.deleteReminder(id)
    }

    override suspend fun deleteRemindersForTarget(targetId: String) {
        reminderDao.deleteRemindersForTarget(targetId)
    }
}
