package com.example

import com.example.model.GoalTask
import com.example.model.GoalWeek
import com.example.model.WeekStatus
import com.example.util.DateTimeUtils
import com.example.util.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTest {

    private val start = DateTimeUtils.getRelativeDayStart(0)

    @Test
    fun `week status follows the calendar`() {
        // Week 1 runs today..today+6, week 2 opens on today+7
        assertEquals(WeekStatus.ACTIVE, GoalProgress.statusOf(start, 1, start))
        assertEquals(WeekStatus.ACTIVE, GoalProgress.statusOf(start, 1, GoalProgress.dayDate(start, 6)))
        assertEquals(WeekStatus.LOCKED, GoalProgress.statusOf(start, 2, GoalProgress.dayDate(start, 6)))

        val day7 = GoalProgress.dayDate(start, 7)
        assertEquals(WeekStatus.ENDED, GoalProgress.statusOf(start, 1, day7))
        assertEquals(WeekStatus.ACTIVE, GoalProgress.statusOf(start, 2, day7))
    }

    @Test
    fun `week starting in the future is locked`() {
        val tomorrow = DateTimeUtils.getRelativeDayStart(1)
        assertEquals(WeekStatus.LOCKED, GoalProgress.statusOf(tomorrow, 1, start))
    }

    @Test
    fun `week percent is done slots over scheduled slots`() {
        val tasks = listOf(
            GoalTask("a", "p", 1, "قراءة", days = listOf(0, 1, 2, 3), doneDays = setOf(0, 1)),
            GoalTask("b", "p", 1, "رياضة", days = listOf(0, 2), doneDays = setOf(0, 2))
        )
        val week = GoalWeek("p", 1, start, WeekStatus.ACTIVE, tasks)
        assertEquals(6, week.totalSlots)
        assertEquals(4, week.doneSlots)
        assertEquals(67, week.percent)
    }

    @Test
    fun `week without goals is zero percent`() {
        val week = GoalWeek("p", 2, start, WeekStatus.ACTIVE, emptyList())
        assertEquals(0, week.percent)
    }
}
