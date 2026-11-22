package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.TaskRepository
import com.example.model.Task
import com.example.ui.tasks.TaskFilter
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TasksTodayOnlyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var contentView: View

    private class Repo(val tasks: MutableStateFlow<List<Task>>) : TaskRepository {
        override fun getTasks(): Flow<List<Task>> = tasks
        override suspend fun addTask(task: Task) { tasks.value += task }
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTask(taskId: String) {}
        override suspend fun toggleTaskCompletion(taskId: String) {
            tasks.value = tasks.value.map { if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it }
        }
    }

    private fun day(offset: Int) = DateTimeUtils.getRelativeDayStart(offset)

    private fun screenshot(name: String) {
        val target = File("/tmp/tasks-visuals/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = contentView.rootView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `only today tasks show and list follows the day`() {
        val repo = Repo(MutableStateFlow(listOf(
            Task(id = "y", title = "Yesterday study", dueDate = day(-1)),
            Task(id = "t1", title = "Today new study", dueDate = day(0)),
            Task(id = "t2", title = "Today review", dueDate = day(0)),
            Task(id = "t3", title = "Today finished", dueDate = day(0), isCompleted = true),
            Task(id = "m", title = "Tomorrow study", dueDate = day(1)),
            Task(id = "f", title = "Far future study", dueDate = day(5))
        )))
        var currentDay = day(0)
        val vm = TasksViewModel(repo, startOfToday = { currentDay }, dayCheckIntervalMs = 50L)
        compose.setContent {
            contentView = LocalView.current
            MaterialTheme { TasksScreen(vm, isArabic = false) }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Today new study").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Today new study").assertExists()
        compose.onNodeWithText("Today review").assertExists()
        compose.onNodeWithText("Today finished").assertExists()
        compose.onNodeWithText("Yesterday study").assertDoesNotExist()
        compose.onNodeWithText("Tomorrow study").assertDoesNotExist()
        compose.onNodeWithText("Far future study").assertDoesNotExist()
        screenshot("1-today")

        // Switching to "All" shows every saved task grouped by date; "Today" is the default.
        compose.onNodeWithTag("filter_chip_all").performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
        compose.waitForIdle()
        org.junit.Assert.assertEquals(TaskFilter.ALL, vm.selectedFilter.value)
        org.junit.Assert.assertEquals(6, vm.uiState.value.displayedTasks.size)
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasText("Yesterday study"))
        compose.onNodeWithText("Yesterday study").assertExists()
        screenshot("2-all")
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasText("Tomorrow study"))
        compose.onNodeWithText("Tomorrow study").assertExists()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasText("Far future study"))
        compose.onNodeWithText("Far future study").assertExists()

        // Back to "Today".
        compose.onNodeWithTag("filter_chip_today").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Tomorrow study").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Yesterday study").assertDoesNotExist()
        compose.onNodeWithText("Today new study").assertExists()

        // The day changes while the app stays open: tomorrow's tasks appear, today's disappear.
        currentDay = day(1)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1))
        compose.waitUntil(5000) { compose.onAllNodesWithText("Tomorrow study").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Tomorrow study").assertExists()
        compose.onNodeWithText("Today new study").assertDoesNotExist()
        compose.onNodeWithText("Today finished").assertDoesNotExist()
        screenshot("3-tomorrow")

        // Back to the original day: nothing was deleted.
        currentDay = day(0)
        compose.runOnIdle { vm.refreshCurrentDay() }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Today review").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Yesterday study").assertDoesNotExist()
        assert(repo.tasks.value.size == 6)
    }
}
