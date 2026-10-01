package com.example.util

import java.util.Calendar

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val isCompletedToday: Boolean,
    val isScheduledToday: Boolean,
    val pendingMissedOccurrences: List<Long> = emptyList(),
    val earliestPendingMissedDate: Long? = null
)

object HabitStreakCalculator {

    /**
     * Checks if a specific day is a scheduled occurrence for the habit.
     */
    fun isDayScheduled(
        timestamp: Long,
        isEveryDay: Boolean,
        daysOfWeek: Set<Int>
    ): Boolean {
        if (isEveryDay) return true
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return daysOfWeek.contains(dayOfWeek)
    }

    /**
     * Computes streak, completion statistics, and pending missed occurrences.
     *
     * Rules:
     * 1. Habit creation date (start of day) is the absolute start of its history.
     *    Never consider any date before creation date as scheduled or missed.
     * 2. Missed occurrences are past scheduled days between creation date and today (exclusive)
     *    that have no completion record and have not been resolved as broken.
     * 3. A pending missed occurrence does NOT break the streak automatically until
     *    the user explicitly selects "Break the streak".
     */
    fun calculate(
        createdAt: Long,
        isEveryDay: Boolean,
        daysOfWeek: Set<Int>,
        completionTimestamps: Set<Long>,
        brokenTimestamps: Set<Long> = emptySet(),
        currentDayTimestamp: Long = DateTimeUtils.getTodayStartOfDay()
    ): StreakResult {
        val creationStartOfDay = DateTimeUtils.getStartOfDay(createdAt)

        // Only completions on or after creation date are valid
        val validCompletions = completionTimestamps.filter { it >= creationStartOfDay }.toSet()
        val totalCompletions = validCompletions.size

        val isScheduledToday = isDayScheduled(currentDayTimestamp, isEveryDay, daysOfWeek)
        val isCompletedToday = validCompletions.contains(currentDayTimestamp)

        // Collect all past scheduled days strictly between creationStartOfDay and today
        val pastScheduledDays = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = creationStartOfDay }

        while (cal.timeInMillis < currentDayTimestamp) {
            val dayMillis = cal.timeInMillis
            if (isDayScheduled(dayMillis, isEveryDay, daysOfWeek)) {
                pastScheduledDays.add(dayMillis)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Unresolved missed occurrences: past scheduled days with NO completion and NOT broken
        val unresolvedMissed = pastScheduledDays.filter { day ->
            !validCompletions.contains(day) && !brokenTimestamps.contains(day)
        }.sorted()

        // 1. Calculate Current Streak
        var currentStreak = 0

        // If today is scheduled and completed, start with 1
        if (isScheduledToday && isCompletedToday) {
            currentStreak = 1
        }

        // Walk backwards through past scheduled days from most recent
        for (day in pastScheduledDays.reversed()) {
            if (validCompletions.contains(day)) {
                currentStreak++
            } else if (brokenTimestamps.contains(day)) {
                // The streak is officially broken at this day
                break
            } else {
                // Unresolved missed occurrence (pending decision):
                // Do NOT automatically break the streak before the user makes this decision.
                // We keep checking past completed days that form the streak pending resolution.
                // Note: the pending day itself is not counted until confirmed "Yes".
            }
        }

        // 2. Calculate Longest Streak
        var longestStreak = 0
        var runningStreak = 0

        val allScheduledUpToToday = mutableListOf<Long>().apply {
            addAll(pastScheduledDays)
            if (isScheduledToday) {
                add(currentDayTimestamp)
            }
        }

        for (day in allScheduledUpToToday) {
            if (validCompletions.contains(day)) {
                runningStreak++
                if (runningStreak > longestStreak) {
                    longestStreak = runningStreak
                }
            } else if (brokenTimestamps.contains(day)) {
                runningStreak = 0
            } else if (day == currentDayTimestamp) {
                // Today not completed yet - ongoing, don't wipe previous running streak
            } else {
                // Pending missed occurrence
                // Does not increment until confirmed
                runningStreak = 0
            }
        }

        longestStreak = maxOf(longestStreak, currentStreak)

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletions = totalCompletions,
            isCompletedToday = isCompletedToday,
            isScheduledToday = isScheduledToday,
            pendingMissedOccurrences = unresolvedMissed,
            earliestPendingMissedDate = unresolvedMissed.firstOrNull()
        )
    }
}
