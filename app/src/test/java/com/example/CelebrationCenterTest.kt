package com.example

import com.example.ui.celebration.CelebrationCenter
import com.example.ui.celebration.CelebrationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationCenterTest {

    @Test
    fun `every kind produces a non-blank bilingual message`() {
        val cases = listOf(
            CelebrationKind.TASK_DONE to 0,
            CelebrationKind.TASK_COUNT_MILESTONE to 5,
            CelebrationKind.ALL_TASKS_DONE to 0,
            CelebrationKind.TASK_TOTAL_MILESTONE to 50,
            CelebrationKind.TASK_DAY_STREAK to 3,
            CelebrationKind.TASK_DAY_STREAK to 7,
            CelebrationKind.TASK_DAY_STREAK to 15,
            CelebrationKind.TASK_DAY_STREAK to 30,
            CelebrationKind.TASK_DAY_STREAK to 100,
            CelebrationKind.HABIT_DONE to 0,
            CelebrationKind.HABIT_STREAK to 3,
            CelebrationKind.HABIT_STREAK to 7,
            CelebrationKind.HABIT_STREAK to 15,
            CelebrationKind.HABIT_STREAK to 21,
            CelebrationKind.HABIT_STREAK to 30,
            CelebrationKind.HABIT_STREAK to 100,
            CelebrationKind.HABIT_STREAK to 365,
            CelebrationKind.ALL_HABITS_DONE to 0,
            CelebrationKind.TIMER_SESSION to 10,
            CelebrationKind.TIMER_SESSION to 30,
            CelebrationKind.TIMER_TOTAL_MILESTONE to 300
        )
        cases.forEach { (kind, detail) ->
            CelebrationCenter.celebrate(kind, detail)
            val c = CelebrationCenter.current.value
            assertNotNull("no celebration for $kind", c)
            assertEquals(kind, c!!.kind)
            assertTrue("blank Arabic message for $kind/$detail", c.messageAr.isNotBlank())
            assertTrue("blank English message for $kind/$detail", c.messageEn.isNotBlank())
            assertTrue("blank emoji for $kind/$detail", c.emoji.isNotBlank())
        }
    }

    @Test
    fun `milestones are always big celebrations`() {
        listOf(
            CelebrationKind.TASK_COUNT_MILESTONE to 10,
            CelebrationKind.ALL_TASKS_DONE to 0,
            CelebrationKind.TASK_TOTAL_MILESTONE to 100,
            CelebrationKind.TASK_DAY_STREAK to 7,
            CelebrationKind.HABIT_STREAK to 15,
            CelebrationKind.ALL_HABITS_DONE to 0,
            CelebrationKind.TIMER_TOTAL_MILESTONE to 600
        ).forEach { (kind, detail) ->
            CelebrationCenter.celebrate(kind, detail)
            assertTrue("$kind should be big", CelebrationCenter.current.value!!.big)
        }
    }

    @Test
    fun `streak detail appears in the message`() {
        CelebrationCenter.celebrate(CelebrationKind.HABIT_STREAK, 15)
        val c = CelebrationCenter.current.value!!
        assertTrue(c.messageAr.contains("15") || c.messageEn.contains("15"))
    }

    @Test
    fun `dismiss clears the current celebration`() {
        CelebrationCenter.celebrate(CelebrationKind.TASK_DONE)
        assertNotNull(CelebrationCenter.current.value)
        CelebrationCenter.dismiss()
        assertNull(CelebrationCenter.current.value)
    }
}
