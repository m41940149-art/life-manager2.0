package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.TaskRepository
import com.example.model.RecurrenceType
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.ui.celebration.CelebrationCenter
import com.example.ui.celebration.CelebrationKind
import com.example.util.DateTimeUtils
import com.example.util.TaskRecurrence
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val titleAr: String, val titleEn: String) {
    TODAY("اليوم", "Today"),
    ALL("الكل", "All")
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
    val selectedFilter: TaskFilter = TaskFilter.TODAY,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val overdueCount: Int = 0
)

class TasksViewModel(
    private val repository: TaskRepository = AppContainer.taskRepository,
    private val startOfToday: () -> Long = { DateTimeUtils.getTodayStartOfDay() },
    private val dayCheckIntervalMs: Long = 60_000L
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilter.TODAY)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _taskToDelete = MutableStateFlow<Task?>(null)
    val taskToDelete: StateFlow<Task?> = _taskToDelete.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    // Bumped at midnight so the list switches to the new day while the app stays open.
    private val _dayTick = MutableStateFlow(startOfToday())

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getTasks(),
        _selectedFilter,
        _dayTick
    ) { storedTasks, filter, today ->
        // "Today" shows only today's tasks; "All" shows every saved task grouped by date.
        // Tasks of other dates stay saved either way; nothing is deleted here.
        val allTasks = storedTasks
        val todayTasks = allTasks.filter { DateTimeUtils.getStartOfDay(it.dueDate) == today }
        val todayCompleted = todayTasks.count { it.isCompleted }
        val overdueTasks = allTasks.filter { !it.isCompleted && DateTimeUtils.isPast(it.dueDate) }

        // Filtered list
        val filtered = when (filter) {
            TaskFilter.TODAY -> todayTasks
            TaskFilter.ALL -> allTasks
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
            val upcomingTasks = allTasks.filter { !it.isCompleted && !DateTimeUtils.isToday(it.dueDate) && !DateTimeUtils.isTomorrow(it.dueDate) && !DateTimeUtils.isPast(it.dueDate) }
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

    init {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(dayCheckIntervalMs)
                val today = startOfToday()
                if (_dayTick.value != today) _dayTick.value = today
            }
        }
        // A repeating task that was not done on its day moves on to its next scheduled day.
        viewModelScope.launch { repository.rollOverMissedRecurringTasks() }
    }

    fun refreshCurrentDay() {
        _dayTick.value = startOfToday()
    }

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun toggleTask(taskId: String) {
        val state = uiState.value
        val task = state.allTasks.find { it.id == taskId }
        val completing = task != null && !task.isCompleted
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
            if (completing) celebrateCompletion(state)
        }
    }

    // Surprise encouragement: picks the rarest milestone this completion unlocked,
    // falling back to an ordinary pat on the back.
    private fun celebrateCompletion(state: TasksUiState) {
        val newTodayCount = state.todayCompletedCount + 1
        when {
            state.todayTotalCount > 0 && newTodayCount >= state.todayTotalCount ->
                CelebrationCenter.celebrate(CelebrationKind.ALL_TASKS_DONE)
            taskDayStreak(state) in dayStreakMilestones ->
                CelebrationCenter.celebrate(CelebrationKind.TASK_DAY_STREAK, taskDayStreak(state))
            totalCompleted(state) in totalMilestones ->
                CelebrationCenter.celebrate(CelebrationKind.TASK_TOTAL_MILESTONE, totalCompleted(state))
            newTodayCount in todayCountMilestones ->
                CelebrationCenter.celebrate(CelebrationKind.TASK_COUNT_MILESTONE, newTodayCount)
            else ->
                CelebrationCenter.celebrate(CelebrationKind.TASK_DONE)
        }
    }

    // Consecutive days ending today with at least one completed task, counting the
    // completion that just happened (its completedAt is written asynchronously).
    private fun taskDayStreak(state: TasksUiState): Int {
        val days = state.allTasks.mapNotNull { it.completedAt }
            .map { DateTimeUtils.getStartOfDay(it) }.toMutableSet()
        days.add(DateTimeUtils.getTodayStartOfDay())
        val cal = Calendar.getInstance().apply { timeInMillis = DateTimeUtils.getTodayStartOfDay() }
        var streak = 0
        while (cal.timeInMillis in days) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun totalCompleted(state: TasksUiState) =
        state.allTasks.count { it.isCompleted } + 1

    private companion object {
        val dayStreakMilestones = setOf(3, 7, 15, 21, 30, 50, 100, 200, 365)
        val totalMilestones = setOf(10, 25, 50, 100, 250, 500, 1000)
        val todayCountMilestones = setOf(5, 10, 20)
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

    /** Saves imported tasks, then reports their ids so the caller can offer Undo. */
    fun importTasks(tasks: List<Task>, onDone: (List<String>) -> Unit) {
        viewModelScope.launch {
            tasks.forEach { repository.addTask(it) }
            onDone(tasks.map { it.id })
        }
    }

    /** Undo of an import: deletes exactly the tasks that import added. */
    fun undoImport(ids: List<String>) {
        viewModelScope.launch { ids.forEach { repository.deleteTask(it) } }
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
