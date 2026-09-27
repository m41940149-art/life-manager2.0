package com.example.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LifeManagerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val habitName = intent.getStringExtra("habit_name") ?: "تذكير بالعادة"
        val habitDesc = intent.getStringExtra("habit_desc") ?: "حان وقت إنجاز عادتك اليومية"

        HabitNotificationScheduler.createNotificationChannel(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(habitName)
            .setContentText(habitDesc.ifBlank { "حان وقت إنجاز عادتك المجدولة اليوم" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(habitDesc.ifBlank { "حان وقت إنجاز عادتك المجدولة اليوم" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(habitId.hashCode(), notification)

        // Reschedule for the next occurrence
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = LifeManagerDatabase.getDatabase(context)
                val habit = db.habitDao().getHabitById(habitId)
                if (habit != null && habit.notificationEnabled) {
                    HabitNotificationScheduler.scheduleHabit(context, habit)
                }
            } catch (_: Exception) {
            }
        }
    }
}
