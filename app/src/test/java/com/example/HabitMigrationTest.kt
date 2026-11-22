package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.LifeManagerDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitMigrationTest {
    @Test fun `upgrade from version 11 preserves habits and completion history`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "habit-migration-test.db"
        context.deleteDatabase(name)
        val initial = Room.databaseBuilder(context, LifeManagerDatabase::class.java, name).build()
        initial.habitDao().insertHabit(HabitEntity(id = "legacy", name = "Read", createdAt = 1234, sortOrder = 7))
        initial.habitDao().insertCompletion(HabitCompletionEntity(habitId = "legacy", dateTimestamp = 5678, completedAt = 6789))
        initial.close()
        // Reconstruct the v11 schema: scheduledTime is the only v12 schema addition.
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("ALTER TABLE habits DROP COLUMN scheduledTime")
            it.version = 11
        }
        val upgraded = Room.databaseBuilder(context, LifeManagerDatabase::class.java, name)
            .addMigrations(LifeManagerDatabase.MIGRATION_11_12).build()
        try {
            val habit = upgraded.habitDao().getHabitById("legacy")!!
            assertEquals("Read", habit.name)
            assertEquals(1234L, habit.createdAt)
            assertEquals(7, habit.sortOrder)
            assertNull(habit.scheduledTime)
            assertEquals(1, upgraded.habitDao().getAllCompletionsSync().size)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
