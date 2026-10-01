package com.example.model

import java.util.Calendar
import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isEveryDay: Boolean = true,
    val daysOfWeek: Set<Int> = setOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    ),
    val notificationEnabled: Boolean = false,
    val notificationTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val isCompletedToday: Boolean = false,
    val isScheduledToday: Boolean = true,
    val pendingMissedDate: Long? = null,
    val pendingMissedCount: Int = 0,
    val colorHex: Long = 0xFF006C5F,
    val categoryIcon: String = "check_circle"
)

data class HabitCompletion(
    val habitId: String,
    val dateTimestamp: Long, // Start of day timestamp (00:00:00)
    val completedAt: Long = System.currentTimeMillis()
)
