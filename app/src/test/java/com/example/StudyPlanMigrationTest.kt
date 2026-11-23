package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.StudyPlanSeeder
import com.example.data.RoomTaskRepository
import com.example.data.local.LifeManagerDatabase
import com.example.data.local.toDomain
import com.example.model.TaskPriority
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class StudyPlanMigrationTest {
    @Test fun upgrade_preservesEditsDeletionsCompletion_andMigratesLateSyncedRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("study_plan_seed_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("study_plan_2026_seeded_v1", true).commit()
        val db = Room.inMemoryDatabaseBuilder(context, LifeManagerDatabase::class.java).allowMainThreadQueries().build()
        val repo = RoomTaskRepository(db.taskDao(), db.syncTombstoneDao())
        val old = StudyPlanSeeder.buildOriginalStudyPlanTasks()
        val deleted = old[4]
        val edited = old[6].copy(description = "my notes")
        val completed = old.first().copy(isCompleted = true, completedAt = 123L, createdAt = 42L,
            dueTime = "10:00", priority = TaskPriority.HIGH, category = "custom")
        val late = old[10]
        old.filter { it.id !in setOf(deleted.id, edited.id, completed.id, late.id) }.forEach { repo.addTask(it) }
        repo.addTask(edited)
        repo.addTask(completed)
        val job = launch(Dispatchers.Default) { StudyPlanSeeder.seedIfNeeded(context, repo) }
        try {
            withTimeout(10000) { repo.getTasks().first { tasks -> tasks.any { it.id == completed.id && it.title.startsWith("ي7:") } } }
            val moved = db.taskDao().getTaskById(completed.id)!!.toDomain()
            assertEquals(completed.copy(title = moved.title, description = moved.description, dueDate = moved.dueDate), moved)
            assertNull(db.taskDao().getTaskById(deleted.id))
            assertEquals(edited, db.taskDao().getTaskById(edited.id)!!.toDomain())
            repo.addTask(late)
            val expected = StudyPlanSeeder.buildStudyPlanTasks().single { it.id == late.id }
            withTimeout(10000) { repo.getTasks().first { rows -> rows.any { it.id == late.id && it.title == expected.title } } }
            assertEquals(47, repo.getTasks().first().size)
            job.cancelAndJoin()
            val settle = launch(Dispatchers.Default) { StudyPlanSeeder.seedIfNeeded(context, repo) }
            withTimeout(10000) { repo.getTasks().first { rows -> rows.none { StudyPlanSeeder.reorderedTask(it) != null } } }
            settle.cancelAndJoin()
            val once = repo.getTasks().first()
            val again = launch(Dispatchers.Default) { StudyPlanSeeder.seedIfNeeded(context, repo) }
            delay(150)
            again.cancelAndJoin()
            assertEquals(once.associateBy { it.id }, repo.getTasks().first().associateBy { it.id })

            // A stale migration proposal cannot undo concurrent completion or schedule edits.
            val sample = old[2]
            repo.addTask(sample)
            val proposal = StudyPlanSeeder.reorderedTask(sample)!!
            repo.updateTask(sample.copy(isCompleted = true, completedAt = 456L))
            repo.updateStudyPlanSchedule(sample, proposal)
            assertTrue(db.taskDao().getTaskById(sample.id)!!.isCompleted)
            assertEquals(456L, db.taskDao().getTaskById(sample.id)!!.completedAt)
            repo.updateTask(sample.copy(title = "edited during migration"))
            repo.updateStudyPlanSchedule(sample, proposal)
            assertEquals("edited during migration", db.taskDao().getTaskById(sample.id)!!.title)
            repo.deleteTask(sample.id)
            repo.updateStudyPlanSchedule(sample, proposal)
            assertNull(db.taskDao().getTaskById(sample.id))
        } finally { job.cancelAndJoin(); db.close() }
    }
}
