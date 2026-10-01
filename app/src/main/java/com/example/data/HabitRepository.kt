package com.example.data

import com.example.model.Habit
import com.example.model.HabitCompletion
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    fun getHabitCompletions(habitId: String): Flow<List<HabitCompletion>>
    suspend fun addHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: String)
    suspend fun toggleHabitCheckIn(habitId: String, dateTimestamp: Long = DateTimeUtils.getTodayStartOfDay())
    suspend fun completeMissedOccurrence(habitId: String, missedDateTimestamp: Long)
    suspend fun breakMissedOccurrence(habitId: String, missedDateTimestamp: Long)
}
