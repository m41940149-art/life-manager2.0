package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.HabitEntity
import com.example.data.local.parseDaysOfWeek
import com.example.util.DateTimeUtils
import com.example.util.HabitStreakCalculator
import java.util.Calendar

object HabitNotificationScheduler {

    const val CHANNEL_ID = "habits_reminder_channel"
    private const val CHANNEL_NAME = "Habit Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled daily and weekly habits"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleHabit(context: Context, habit: HabitEntity) {
        if (!habit.notificationEnabled || habit.notificationTime.isNullOrBlank()) {
            cancelHabit(context, habit.id)
            return
        }

        val (hour, minute) = DateTimeUtils.parseHourMinute(habit.notificationTime) ?: return
        val daysOfWeek = habit.parseDaysOfWeek()

        // Find next trigger time
        val triggerMillis = getNextTriggerTimestamp(hour, minute, habit.isEveryDay, daysOfWeek)
            ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
            putExtra("habit_desc", habit.description)
        }

        val requestCode = habit.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // In case exact alarm permission is restricted
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelHabit(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val requestCode = habitId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getNextTriggerTimestamp(
        hour: Int,
        minute: Int,
        isEveryDay: Boolean,
        daysOfWeek: Set<Int>
    ): Long? {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Search for the next scheduled occurrence within the next 8 days
        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            if (candidate.after(now)) {
                val candidateMillis = DateTimeUtils.getStartOfDay(candidate.timeInMillis)
                if (HabitStreakCalculator.isDayScheduled(candidateMillis, isEveryDay, daysOfWeek)) {
                    return candidate.timeInMillis
                }
            }
        }
        return null
    }
}
