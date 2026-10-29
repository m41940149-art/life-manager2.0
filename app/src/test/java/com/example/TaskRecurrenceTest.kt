package com.example

import com.example.model.RecurrenceType
import com.example.util.TaskRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class TaskRecurrenceTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month, dayOfMonth, 0, 0, 0)
        return cal.timeInMillis
    }

    // 2026-11-01 is a Sunday
    private val sunday = day(2026, Calendar.NOVEMBER, 1)

    @Test
    fun customDays_sunWedThu_followsTheCycle() {
        val days = setOf(Calendar.SUNDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
        assertEquals(day(2026, Calendar.NOVEMBER, 4), TaskRecurrence.nextOccurrenceAfter(sunday, days))
        assertEquals(day(2026, Calendar.NOVEMBER, 5), TaskRecurrence.nextOccurrenceAfter(day(2026, Calendar.NOVEMBER, 4), days))
        assertEquals(day(2026, Calendar.NOVEMBER, 8), TaskRecurrence.nextOccurrenceAfter(day(2026, Calendar.NOVEMBER, 5), days))
    }

    @Test
    fun weekly_repeatsSameWeekdayAfterSevenDays() {
        val days = TaskRecurrence.effectiveDays(RecurrenceType.WEEKLY, emptySet(), sunday)
        assertEquals(day(2026, Calendar.NOVEMBER, 8), TaskRecurrence.nextOccurrenceAfter(sunday, days))
    }

    @Test
    fun daily_nextIsTomorrow() {
        val days = TaskRecurrence.effectiveDays(RecurrenceType.DAILY, emptySet(), sunday)
        assertEquals(day(2026, Calendar.NOVEMBER, 2), TaskRecurrence.nextOccurrenceAfter(sunday, days))
    }

    @Test
    fun firstOccurrence_snapsForwardToMatchingDay() {
        val days = setOf(Calendar.WEDNESDAY)
        assertEquals(day(2026, Calendar.NOVEMBER, 4), TaskRecurrence.firstOccurrenceOnOrAfter(sunday, days))
        assertEquals(day(2026, Calendar.NOVEMBER, 4), TaskRecurrence.firstOccurrenceOnOrAfter(day(2026, Calendar.NOVEMBER, 4), days))
    }

    @Test
    fun emptyDays_hasNoNextOccurrence() {
        assertNull(TaskRecurrence.nextOccurrenceAfter(sunday, emptySet()))
    }
}
