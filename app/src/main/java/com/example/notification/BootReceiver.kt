package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LifeManagerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            HabitNotificationScheduler.createNotificationChannel(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = LifeManagerDatabase.getDatabase(context)
                    val habits = db.habitDao().getAllHabits().first()
                    for (habit in habits) {
                        if (habit.notificationEnabled) {
                            HabitNotificationScheduler.scheduleHabit(context, habit)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
