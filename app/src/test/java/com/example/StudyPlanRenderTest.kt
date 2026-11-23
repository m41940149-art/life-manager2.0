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
class StudyPlanRenderTest {
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

    @Test fun reorderedScheduleRendersTodayAndAll() {
        val tasks = com.example.data.StudyPlanSeeder.buildStudyPlanTasks()
        val repo = Repo(MutableStateFlow(tasks))
        val firstDay = tasks.first().dueDate
        val vm = TasksViewModel(repo, startOfToday = { firstDay })
        compose.setContent {
            contentView = LocalView.current
            MaterialTheme { TasksScreen(vm, isArabic = true) }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("ي1: نقود وبنوك ج1 ف1–3").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("ي1: نقود وبنوك ج1 ف1–3").assertExists()
        compose.onNodeWithText("ي7: قانون ف1–4").assertDoesNotExist()
        screenshot("study-money-first")
        compose.onNodeWithTag("filter_chip_all").performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasText("ي7: قانون ف1–4"))
        compose.onNodeWithText("ي7: قانون ف1–4").assertExists()
        screenshot("study-law-after-money")
        assert(repo.tasks.value.size == 48)
    }
}
