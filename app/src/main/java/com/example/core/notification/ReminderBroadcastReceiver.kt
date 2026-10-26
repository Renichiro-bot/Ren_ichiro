package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_REMINDER_TRIGGER) {
            val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
            val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Reminder"
            val message = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE) ?: ""
            val targetId = intent.getStringExtra(ReminderScheduler.EXTRA_TARGET_ID)
            val targetType = intent.getStringExtra(ReminderScheduler.EXTRA_TARGET_TYPE)

            Log.d("ReminderReceiver", "Received reminder alarm: id=$reminderId")
            ReminderScheduler.showNotification(
                context = context,
                reminderId = reminderId,
                title = title,
                message = message,
                targetId = targetId,
                targetType = targetType
            )
        }
    }
}
