package com.example

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.TaskRepository
import com.example.model.Task
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI tests for picking tasks: long press starts it, taps tick and untick,
 * "select all" ticks the whole visible list, and the delete only happens
 * after the confirmation dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TasksSelectionTest {
    @get:Rule val compose = createComposeRule()

    private class Repo(val tasks: MutableStateFlow<List<Task>>) : TaskRepository {
        val deleted = mutableListOf<String>()

        override fun getTasks(): Flow<List<Task>> = tasks
        override suspend fun addTask(task: Task) { tasks.value += task }
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTask(taskId: String) {
            deleted += taskId
            tasks.value = tasks.value.filterNot { it.id == taskId }
        }
        override suspend fun toggleTaskCompletion(taskId: String) {}
    }

    private fun day(offset: Int) = DateTimeUtils.getRelativeDayStart(offset)

    private fun threeTasks() = listOf(
        Task(id = "a", title = "Alpha task", dueDate = day(0)),
        Task(id = "b", title = "Beta task", dueDate = day(0)),
        Task(id = "c", title = "Gamma task", dueDate = day(0))
    )

    @Test fun `long press picks, tap toggles, select all, delete after confirmation`() {
        val repo = Repo(MutableStateFlow(threeTasks()))
        val vm = TasksViewModel(repo, startOfToday = { day(0) }, dayCheckIntervalMs = 60_000L)
        compose.setContent {
            MaterialTheme { TasksScreen(vm, isArabic = false) }
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Alpha task").fetchSemanticsNodes().isNotEmpty()
        }

        // Not picking yet: no bar, and the add button is still there.
        compose.onNodeWithTag("selection_toolbar").assertDoesNotExist()
        compose.onNodeWithTag("add_task_fab").assertExists()

        // Long press enters picking mode with that one task ticked.
        compose.onNodeWithTag("task_item_a").performTouchInput { longClick() }
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("a") }
        compose.onNodeWithTag("selection_toolbar").assertExists()
        compose.onNodeWithText("1 selected").assertExists()
        compose.onNodeWithTag("add_task_fab").assertDoesNotExist()

        // A plain tap now ticks instead of opening the editor.
        compose.onNodeWithTag("task_item_b").performClick()
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("a", "b") }
        compose.onNodeWithText("2 selected").assertExists()

        // Tapping a ticked task unticks it and leaves the others alone.
        compose.onNodeWithTag("task_item_b").performClick()
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("a") }

        // Select all ticks every task in the visible list.
        compose.onNodeWithTag("selection_select_all_btn").performClick()
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("a", "b", "c") }
        compose.onNodeWithText("3 selected").assertExists()

        // Delete asks first; cancelling keeps every task and the picking.
        compose.onNodeWithTag("selection_delete_btn").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("bulk_delete_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("cancel_bulk_delete_btn").performClick()
        compose.waitForIdle()
        assertEquals(3, repo.tasks.value.size)
        assertEquals(emptyList<String>(), repo.deleted)
        compose.onNodeWithTag("selection_toolbar").assertExists()

        // Confirming deletes all three, then leaves picking mode.
        compose.onNodeWithTag("selection_delete_btn").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("bulk_delete_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("confirm_bulk_delete_btn").performClick()
        compose.waitUntil(5000) { repo.tasks.value.isEmpty() }
        assertEquals(setOf("a", "b", "c"), repo.deleted.toSet())
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("selection_toolbar").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("No tasks for today").assertExists()
    }

    @Test fun `close button and unticking the last task both leave picking`() {
        val repo = Repo(MutableStateFlow(threeTasks()))
        val vm = TasksViewModel(repo, startOfToday = { day(0) }, dayCheckIntervalMs = 60_000L)
        compose.setContent {
            MaterialTheme { TasksScreen(vm, isArabic = false) }
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Alpha task").fetchSemanticsNodes().isNotEmpty()
        }

        // Unticking the only ticked task ends picking without deleting anything.
        compose.onNodeWithTag("task_item_a").performTouchInput { longClick() }
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("a") }
        compose.onNodeWithTag("task_item_a").performClick()
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds.isEmpty() }
        compose.onNodeWithTag("selection_toolbar").assertDoesNotExist()
        assertEquals(3, repo.tasks.value.size)

        // The close button ends picking too.
        compose.onNodeWithTag("task_item_b").performTouchInput { longClick() }
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds == setOf("b") }
        compose.onNodeWithTag("selection_close_btn").performClick()
        compose.waitUntil(5000) { vm.uiState.value.selectedTaskIds.isEmpty() }
        compose.onNodeWithTag("selection_toolbar").assertDoesNotExist()
        assertEquals(3, repo.tasks.value.size)
        assertEquals(emptyList<String>(), repo.deleted)
    }
}
