package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.TaskRepository
import com.example.model.RecurrenceType
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.util.DateTimeUtils
import com.example.util.TaskRecurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val titleAr: String, val titleEn: String) {
    ALL("الكل", "All"),
    TODAY("اليوم", "Today"),
    TOMORROW("غداً", "Tomorrow"),
    UPCOMING("القادمة", "Upcoming"),
    COMPLETED("المكتملة", "Completed")
}

data class TaskGroup(
    val titleAr: String,
    val titleEn: String,
    val tasks: List<Task>,
    val isCollapsible: Boolean = false,
    val isInitiallyCollapsed: Boolean = false
)

data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val displayedTasks: List<Task> = emptyList(),
    val groupedTasks: List<TaskGroup> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val overdueCount: Int = 0
)

class TasksViewModel(
    private val repository: TaskRepository = AppContainer.taskRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _taskToDelete = MutableStateFlow<Task?>(null)
    val taskToDelete: StateFlow<Task?> = _taskToDelete.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getTasks(),
        _selectedFilter
    ) { allTasks, filter ->
        val todayTasks = allTasks.filter { DateTimeUtils.isToday(it.dueDate) }
        val todayCompleted = todayTasks.count { it.isCompleted }
        val overdueTasks = allTasks.filter { !it.isCompleted && DateTimeUtils.isPast(it.dueDate) }

        // Filtered list
        val filtered = when (filter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.TODAY -> todayTasks
            TaskFilter.TOMORROW -> allTasks.filter { DateTimeUtils.isTomorrow(it.dueDate) }
            TaskFilter.UPCOMING -> allTasks.filter { DateTimeUtils.isUpcoming(it.dueDate) }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }

        // Build logical date groups
        val groups = mutableListOf<TaskGroup>()

        if (filter == TaskFilter.ALL) {
            // Overdue group if any
            if (overdueTasks.isNotEmpty()) {
                groups.add(
                    TaskGroup(
                        titleAr = "متأخرة (${overdueTasks.size})",
                        titleEn = "Overdue (${overdueTasks.size})",
                        tasks = overdueTasks
                    )
                )
            }

            // Today group
            val todayPending = todayTasks.filter { !it.isCompleted }
            if (todayPending.isNotEmpty() || todayCompleted == 0) {
                groups.add(
                    TaskGroup(
                        titleAr = "اليوم (${todayPending.size})",
                        titleEn = "Today (${todayPending.size})",
                        tasks = todayPending
                    )
                )
            }

            // Tomorrow group
            val tomorrowTasks = allTasks.filter { !it.isCompleted && DateTimeUtils.isTomorrow(it.dueDate) }
            if (tomorrowTasks.isNotEmpty()) {
                groups.add(
                    TaskGroup(
                        titleAr = "غداً (${tomorrowTasks.size})",
                        titleEn = "Tomorrow (${tomorrowTasks.size})",
                        tasks = tomorrowTasks
                    )
                )
            }

            // Upcoming group (later dates)
            val upcomingTasks = allTasks.filter { !it.isCompleted && DateTimeUtils.isUpcoming(it.dueDate) }
            if (upcomingTasks.isNotEmpty()) {
                groups.add(
                    TaskGroup(
                        titleAr = "الأيام القادمة (${upcomingTasks.size})",
                        titleEn = "Upcoming (${upcomingTasks.size})",
                        tasks = upcomingTasks
                    )
                )
            }

            // Completed group (all completed tasks)
            val completedTasks = allTasks.filter { it.isCompleted }
            if (completedTasks.isNotEmpty()) {
                groups.add(
                    TaskGroup(
                        titleAr = "المكتملة (${completedTasks.size})",
                        titleEn = "Completed (${completedTasks.size})",
                        tasks = completedTasks,
                        isCollapsible = true
                    )
                )
            }
        }

        TasksUiState(
            allTasks = allTasks,
            displayedTasks = filtered,
            groupedTasks = groups,
            selectedFilter = filter,
            todayCompletedCount = todayCompleted,
            todayTotalCount = todayTasks.size,
            overdueCount = overdueTasks.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
        }
    }

    fun setTaskToDelete(task: Task?) {
        _taskToDelete.value = task
    }

    fun confirmDeleteTask() {
        val task = _taskToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteTask(task.id)
            _taskToDelete.value = null
        }
    }

    fun setTaskToEdit(task: Task?) {
        _taskToEdit.value = task
    }

    fun addTask(
        title: String,
        description: String,
        dueDate: Long,
        dueTime: String?,
        priority: TaskPriority,
        category: String,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceDays: Set<Int> = emptySet()
    ) {
        viewModelScope.launch {
            val days = TaskRecurrence.effectiveDays(recurrenceType, recurrenceDays, dueDate)
            // For "specific days", the first occurrence is the first matching day on/after the chosen date.
            val firstDate = if (recurrenceType == RecurrenceType.CUSTOM) {
                TaskRecurrence.firstOccurrenceOnOrAfter(dueDate, days)
            } else dueDate
            val newTask = Task(
                title = title.trim(),
                description = description.trim(),
                dueDate = firstDate,
                dueTime = dueTime?.trim().takeIf { !it.isNullOrBlank() },
                priority = priority,
                category = category.trim().ifBlank { "عام" },
                recurrenceType = recurrenceType,
                recurrenceDays = days
            ).let { if (recurrenceType != RecurrenceType.NONE) it.copy(seriesId = it.id) else it }
            repository.addTask(newTask)
        }
    }

    fun updateTask(
        id: String,
        title: String,
        description: String,
        dueDate: Long,
        dueTime: String?,
        priority: TaskPriority,
        category: String,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceDays: Set<Int> = emptySet()
    ) {
        viewModelScope.launch {
            val current = uiState.value.allTasks.find { it.id == id } ?: return@launch
            val days = TaskRecurrence.effectiveDays(recurrenceType, recurrenceDays, dueDate)
            val updated = current.copy(
                recurrenceType = recurrenceType,
                recurrenceDays = days,
                seriesId = if (recurrenceType == RecurrenceType.NONE) null else (current.seriesId ?: current.id),
                title = title.trim(),
                description = description.trim(),
                dueDate = dueDate,
                dueTime = dueTime?.trim().takeIf { !it.isNullOrBlank() },
                priority = priority,
                category = category.trim().ifBlank { "عام" }
            )
            repository.updateTask(updated)
            _taskToEdit.value = null
        }
    }
}
