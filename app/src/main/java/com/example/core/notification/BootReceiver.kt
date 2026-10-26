package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.database.VellumDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Device rebooted or package replaced; reconciling pending reminders")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = VellumDatabase.getInstance(context)
                    val upcoming = db.reminderDao().getUpcomingActiveRemindersList(System.currentTimeMillis())
                    Log.d("BootReceiver", "Found ${upcoming.size} active upcoming reminders to reschedule")
                    upcoming.forEach { entity ->
                        ReminderScheduler.scheduleReminder(context, entity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule reminders on reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
