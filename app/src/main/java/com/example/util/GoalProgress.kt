package com.example.util

import com.example.model.WeekStatus
import java.util.Calendar

/** Pure date helpers for the weekly goals feature. Weeks start on the plan's chosen start day. */
object GoalProgress {

    /** First day (start-of-day) of week [weekNumber] (1-based). */
    fun weekStart(planStart: Long, weekNumber: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = planStart
        cal.add(Calendar.DAY_OF_YEAR, (weekNumber - 1) * 7)
        return DateTimeUtils.getStartOfDay(cal.timeInMillis)
    }

    /** Date (start-of-day) of the day at [offset] (0..6) inside a week starting at [weekStart]. */
    fun dayDate(weekStart: Long, offset: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = weekStart
        cal.add(Calendar.DAY_OF_YEAR, offset)
        return DateTimeUtils.getStartOfDay(cal.timeInMillis)
    }

    fun statusOf(planStart: Long, weekNumber: Int, today: Long): WeekStatus {
        val start = weekStart(planStart, weekNumber)
        val nextStart = weekStart(planStart, weekNumber + 1)
        return when {
            today >= nextStart -> WeekStatus.ENDED
            today >= start -> WeekStatus.ACTIVE
            else -> WeekStatus.LOCKED
        }
    }

    /** Calendar.DAY_OF_WEEK (1=Sunday..7=Saturday) of the day at [offset] from [weekStart]. */
    fun calendarDayOfWeek(weekStart: Long, offset: Int): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayDate(weekStart, offset)
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    fun weekdayName(calendarDayOfWeek: Int, isArabic: Boolean): String {
        val ar = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val en = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val i = (calendarDayOfWeek - 1).coerceIn(0, 6)
        return if (isArabic) ar[i] else en[i]
    }
}
