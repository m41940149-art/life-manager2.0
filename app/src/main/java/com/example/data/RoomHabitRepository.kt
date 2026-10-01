package com.example.data

import android.content.Context
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitDao
import com.example.data.local.HabitMissedResolutionEntity
import com.example.data.local.SyncTombstoneDao
import com.example.data.local.SyncTombstoneEntity
import com.example.data.local.parseDaysOfWeek
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.Habit
import com.example.model.HabitCompletion
import com.example.notification.HabitNotificationScheduler
import com.example.util.DateTimeUtils
import com.example.util.HabitStreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomHabitRepository(
    private val habitDao: HabitDao,
    private val context: Context,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : HabitRepository {

    override fun getHabits(): Flow<List<Habit>> {
        return combine(
            habitDao.getAllHabits(),
            habitDao.getAllCompletions(),
            habitDao.getAllMissedResolutions()
        ) { habits, allCompletions, allResolutions ->
            val completionsByHabit = allCompletions.groupBy { it.habitId }
            val resolutionsByHabit = allResolutions.groupBy { it.habitId }

            habits.map { entity ->
                val completionTimestamps = completionsByHabit[entity.id]
                    ?.map { it.dateTimestamp }
                    ?.toSet() ?: emptySet()

                val brokenTimestamps = resolutionsByHabit[entity.id]
                    ?.filter { it.resolution == "BROKEN" }
                    ?.map { it.dateTimestamp }
                    ?.toSet() ?: emptySet()

                val daysOfWeek = entity.parseDaysOfWeek()

                val streak = HabitStreakCalculator.calculate(
                    createdAt = entity.createdAt,
                    isEveryDay = entity.isEveryDay,
                    daysOfWeek = daysOfWeek,
                    completionTimestamps = completionTimestamps,
                    brokenTimestamps = brokenTimestamps
                )

                Habit(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    isEveryDay = entity.isEveryDay,
                    daysOfWeek = daysOfWeek,
                    notificationEnabled = entity.notificationEnabled,
                    notificationTime = entity.notificationTime,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    currentStreak = streak.currentStreak,
                    longestStreak = streak.longestStreak,
                    totalCompletions = streak.totalCompletions,
                    isCompletedToday = streak.isCompletedToday,
                    isScheduledToday = streak.isScheduledToday,
                    pendingMissedDate = streak.earliestPendingMissedDate,
                    pendingMissedCount = streak.pendingMissedOccurrences.size,
                    colorHex = entity.colorHex,
                    categoryIcon = entity.categoryIcon
                )
            }
        }
    }

    override fun getHabitCompletions(habitId: String): Flow<List<HabitCompletion>> {
        return habitDao.getCompletionsForHabit(habitId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addHabit(habit: Habit) {
        val entity = habit.toEntity()
        habitDao.insertHabit(entity)
        if (entity.notificationEnabled) {
            HabitNotificationScheduler.scheduleHabit(context, entity)
        }
    }

    override suspend fun updateHabit(habit: Habit) {
        val entity = habit.toEntity()
        habitDao.updateHabit(entity)
        if (entity.notificationEnabled) {
            HabitNotificationScheduler.scheduleHabit(context, entity)
        } else {
            HabitNotificationScheduler.cancelHabit(context, entity.id)
        }
    }

    override suspend fun deleteHabit(habitId: String) {
        HabitNotificationScheduler.cancelHabit(context, habitId)
        habitDao.deleteCompletionsForHabit(habitId)
        habitDao.deleteMissedResolutionsForHabit(habitId)
        habitDao.deleteHabitById(habitId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "HABIT",
                entityId = habitId
            )
        )
    }

    override suspend fun toggleHabitCheckIn(habitId: String, dateTimestamp: Long) {
        val normalizedDate = DateTimeUtils.getStartOfDay(dateTimestamp)
        val isCompleted = habitDao.isCompletedOnDate(habitId, normalizedDate)
        if (isCompleted) {
            habitDao.deleteCompletion(habitId, normalizedDate)
            syncTombstoneDao?.insertTombstone(
                SyncTombstoneEntity(
                    entityType = "HABIT_COMPLETION",
                    entityId = habitId,
                    extraId = normalizedDate.toString()
                )
            )
        } else {
            // If checking in, remove any broken resolution for this date as well
            habitDao.deleteMissedResolution(habitId, normalizedDate)
            habitDao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    dateTimestamp = normalizedDate,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun completeMissedOccurrence(habitId: String, missedDateTimestamp: Long) {
        val normalizedDate = DateTimeUtils.getStartOfDay(missedDateTimestamp)
        habitDao.deleteMissedResolution(habitId, normalizedDate)
        habitDao.insertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                dateTimestamp = normalizedDate,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun breakMissedOccurrence(habitId: String, missedDateTimestamp: Long) {
        val normalizedDate = DateTimeUtils.getStartOfDay(missedDateTimestamp)
        // Record as broken so we don't prompt again and streak calculation knows it's broken
        habitDao.insertMissedResolution(
            HabitMissedResolutionEntity(
                habitId = habitId,
                dateTimestamp = normalizedDate,
                resolution = "BROKEN",
                resolvedAt = System.currentTimeMillis()
            )
        )
    }
}
