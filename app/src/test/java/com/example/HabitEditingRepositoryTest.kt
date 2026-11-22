package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoomHabitRepository
import com.example.data.local.LifeManagerDatabase
import com.example.model.Habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitEditingRepositoryTest {
    private lateinit var db: LifeManagerDatabase
    private lateinit var repo: RoomHabitRepository
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = LifeManagerDatabase.getInMemoryDatabase(context)
        repo = RoomHabitRepository(db.habitDao(), context)
    }
    @After fun tearDown() = db.close()

    @Test fun `editing persists time and keeps identity history and order`() = runBlocking {
        repo.addHabit(Habit(id = "habit", name = "Read", createdAt = 1000, scheduledTime = "15:59"))
        repo.toggleHabitCheckIn("habit")
        val before = repo.getHabits().first().single()
        repo.updateHabit(before.copy(name = "Read a chapter", scheduledTime = "16:00", description = "Daily reading"))
        val after = repo.getHabits().first().single()
        assertEquals("habit", after.id)
        assertEquals("16:00", after.scheduledTime)
        assertEquals("Read a chapter", after.name)
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(before.sortOrder, after.sortOrder)
        assertEquals(before.totalCompletions, after.totalCompletions)
        assertEquals(1, repo.getHabitCompletions("habit").first().size)
        assertTrue(after.isCompletedToday)
        assertFalse(after.notificationEnabled)
    }

    @Test fun `legacy habits remain editable without inventing performance time`() = runBlocking {
        repo.addHabit(Habit(id = "legacy", name = "Old habit", notificationTime = "20:00"))
        val old = repo.getHabits().first().single()
        assertNull(old.scheduledTime)
        repo.updateHabit(old.copy(scheduledTime = "08:00"))
        assertEquals("08:00", repo.getHabits().first().single().scheduledTime)
    }
}
