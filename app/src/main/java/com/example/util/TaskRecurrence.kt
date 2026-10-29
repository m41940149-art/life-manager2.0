package com.example.util

import com.example.model.ALL_WEEK_DAYS
import com.example.model.RecurrenceType
import java.util.Calendar

object TaskRecurrence {

    /** Days of week (Calendar.SUNDAY..SATURDAY) a task repeats on, derived from its type. */
    fun effectiveDays(type: RecurrenceType, chosenDays: Set<Int>, anchorDate: Long): Set<Int> =
        when (type) {
            RecurrenceType.NONE -> emptySet()
            RecurrenceType.DAILY -> ALL_WEEK_DAYS
            RecurrenceType.WEEKLY -> setOf(dayOfWeek(anchorDate))
            RecurrenceType.CUSTOM -> chosenDays
        }

    fun dayOfWeek(date: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    /** First day strictly after [fromDate] whose weekday is in [days]; null if [days] is empty. */
    fun nextOccurrenceAfter(fromDate: Long, days: Set<Int>): Long? {
        if (days.isEmpty()) return null
        val cal = Calendar.getInstance()
        cal.timeInMillis = DateTimeUtils.getStartOfDay(fromDate)
        repeat(7) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            if (cal.get(Calendar.DAY_OF_WEEK) in days) return cal.timeInMillis
        }
        return null
    }

    /** [date] itself if its weekday is in [days], otherwise the next matching day. */
    fun firstOccurrenceOnOrAfter(date: Long, days: Set<Int>): Long {
        val start = DateTimeUtils.getStartOfDay(date)
        if (days.isEmpty() || dayOfWeek(start) in days) return start
        return nextOccurrenceAfter(start, days) ?: start
    }
}
