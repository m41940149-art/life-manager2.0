package com.example

import com.example.model.HabitTimeGroup
import com.example.model.habitTimeGroup
import com.example.model.isHabitVisibleInPeriod
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTimeGroupTest {
    @Test fun `automatic visibility follows local 4 PM boundary and override`() {
        assertTrue(isHabitVisibleInPeriod("08:00", 15, false))
        assertFalse(isHabitVisibleInPeriod("16:00", 15, false))
        assertFalse(isHabitVisibleInPeriod("08:00", 16, false))
        assertTrue(isHabitVisibleInPeriod("16:00", 16, false))
        assertTrue(isHabitVisibleInPeriod(null, 16, false))
        assertTrue(isHabitVisibleInPeriod("08:00", 16, true))
        assertTrue(isHabitVisibleInPeriod("16:00", 8, true))
    }

    @Test fun `4 PM starts afternoon evening`() {
        assertEquals(HabitTimeGroup.MORNING, habitTimeGroup("00:00"))
        assertEquals(HabitTimeGroup.MORNING, habitTimeGroup("15:59"))
        assertEquals(HabitTimeGroup.AFTERNOON_EVENING, habitTimeGroup("16:00"))
        assertEquals(HabitTimeGroup.AFTERNOON_EVENING, habitTimeGroup("23:59"))
    }

    @Test fun `missing or invalid times are not guessed`() {
        listOf(null, "", "24:00", "16:60", "-1:00", "8 PM").forEach {
            assertEquals(HabitTimeGroup.UNSCHEDULED, habitTimeGroup(it))
        }
    }
}
