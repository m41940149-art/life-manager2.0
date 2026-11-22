package com.example

import com.example.data.StudyPlanSeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StudyPlanSeedTest {

    private fun ymd(timestamp: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    @Test
    fun seed_builds48Tasks_withUniqueIds() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        // Day 1 has no review; days 2-21 have new + review; days 22-28 have no new material.
        assertEquals(48, tasks.size)
        assertEquals(48, tasks.map { it.id }.distinct().size)
    }

    @Test
    fun seed_startsNov23_endsDec20() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        assertEquals(Triple(2026, 11, 23), ymd(tasks.first().dueDate))
        assertEquals(Triple(2026, 12, 20), ymd(tasks.last().dueDate))
    }

    @Test
    fun seed_datesAre28ConsecutiveDays() {
        val days = StudyPlanSeeder.buildStudyPlanTasks().map { ymd(it.dueDate) }.distinct()
        val expected = (23..30).map { Triple(2026, 11, it) } + (1..20).map { Triple(2026, 12, it) }
        assertEquals(expected, days)
    }

    @Test
    fun seed_days2to21_haveTwoSeparateTasks() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        for (day in 2..21) {
            val dayTasks = tasks.filter { it.title.startsWith("ي$day:") }
            assertEquals("day $day", 2, dayTasks.size)
            assertTrue("day $day new", dayTasks.any { it.description.startsWith("جديد") })
            assertTrue("day $day review", dayTasks.any { it.description.startsWith("مراجعة:") })
        }
    }

    @Test
    fun seed_day1_andDays22to28_haveOneTask() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        assertEquals(1, tasks.count { it.title.startsWith("ي1:") })
        for (day in 22..28) {
            assertEquals("day $day", 1, tasks.count { it.title.startsWith("ي$day:") })
        }
    }

    @Test
    fun seed_hasNoClockTimesAndNoRecurrence() {
        StudyPlanSeeder.buildStudyPlanTasks().forEach { task ->
            assertTrue(task.dueTime == null)
            assertFalse(task.isRecurring)
            assertFalse(task.isCompleted)
        }
    }

    @Test
    fun seed_islamicAdministrationIsRemoved() {
        val all = StudyPlanSeeder.buildStudyPlanTasks()
            .joinToString("\n") { it.title + "\n" + it.description }
        assertFalse(all.contains("إدارة الإسلام"))
    }

    @Test
    fun seed_day25ReviewsPublicAdministrationOnly() {
        val day25 = StudyPlanSeeder.buildStudyPlanTasks().single { it.title.startsWith("ي25:") }
        assertTrue(day25.title.contains("الإدارة العامة"))
        assertEquals(Triple(2026, 12, 17), ymd(day25.dueDate))
    }

    @Test
    fun seed_moneyAndBankingPagesMatchChapterCounts() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        val moneyPages = (6..11).map { day ->
            val task = tasks.single { it.id == "study-plan-2026-y$day-new" }
            Regex("جديد (\\d+)ص").find(task.description)!!.groupValues[1].toInt()
        }
        // Chapter sizes supplied by the user: P1 17+15+28, 14+24+30, 30+23 / P2 13+25+20, 22+14+37, 19+17
        assertEquals(listOf(60, 68, 53, 58, 73, 36), moneyPages)
        assertEquals(348, moneyPages.sum())
    }

    @Test
    fun seed_reviewArithmeticMatchesNewTaskPages() {
        val tasks = StudyPlanSeeder.buildStudyPlanTasks()
        val newPagesByDay = (1..21).associateWith { day ->
            val task = tasks.singleOrNull { it.id == "study-plan-2026-y$day-new" }
                ?: tasks.single { it.id == "study-plan-2026-y$day" }
            Regex("جديد (\\d+)ص").find(task.description)!!.groupValues[1].toInt()
        }
        for (day in 2..21) {
            val review = tasks.single { it.id == "study-plan-2026-y$day-review" }.description
            val referenced = Regex("ي(\\d+) \\((\\d+)ص\\)").findAll(review).toList()
            assertTrue("day $day references something", referenced.isNotEmpty())
            referenced.forEach { m ->
                val refDay = m.groupValues[1].toInt()
                val refPages = m.groupValues[2].toInt()
                assertEquals("day $day review of ي$refDay", newPagesByDay[refDay], refPages)
            }
            Regex("= (\\d+)ص").find(review)?.let { total ->
                assertEquals("day $day review total", referenced.sumOf { it.groupValues[2].toInt() }, total.groupValues[1].toInt())
            }
        }
    }
}
