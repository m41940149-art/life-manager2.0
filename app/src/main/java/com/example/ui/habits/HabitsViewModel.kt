package com.example.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.HabitRepository
import com.example.model.Habit
import com.example.model.HabitCompletion
import com.example.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    // True when the user owns at least one habit, even if none is scheduled today.
    val hasAnyHabits: Boolean = false,
    val completedTodayCount: Int = 0,
    val totalCount: Int = 0,
    val maxStreakAcrossHabits: Int = 0,
    val selectedHabitForHistory: Habit? = null,
    val historyCompletions: List<HabitCompletion> = emptyList(),
    val forgotRecordHabit: Habit? = null,
    val missedOccurrenceDate: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(
    private val repository: HabitRepository = AppContainer.habitRepository
) : ViewModel() {

    private val _selectedHabitForHistory = MutableStateFlow<Habit?>(null)
    private val _forgotRecordHabit = MutableStateFlow<Habit?>(null)
    private val _missedOccurrenceDate = MutableStateFlow<Long?>(null)

    private val completionsForSelectedHabit = _selectedHabitForHistory.flatMapLatest { habit ->
        if (habit == null) flowOf(emptyList())
        else repository.getHabitCompletions(habit.id)
    }

    val uiState: StateFlow<HabitsUiState> = combine(
        repository.getHabits(),
        _selectedHabitForHistory,
        completionsForSelectedHabit,
        _forgotRecordHabit,
        _missedOccurrenceDate
    ) { allHabits, selectedHabit, completions, forgotHabit, missedDate ->
        // A habit is only listed on the days it is scheduled for.
        val habits = allHabits.filter { it.isScheduledToday }
        val completedToday = habits.count { it.isCompletedToday }
        val maxStreak = allHabits.maxOfOrNull { it.currentStreak } ?: 0

        val currentSelectedHabit = allHabits.find { it.id == selectedHabit?.id } ?: selectedHabit

        HabitsUiState(
            habits = habits,
            hasAnyHabits = allHabits.isNotEmpty(),
            completedTodayCount = completedToday,
            totalCount = habits.size,
            maxStreakAcrossHabits = maxStreak,
            selectedHabitForHistory = currentSelectedHabit,
            historyCompletions = completions,
            forgotRecordHabit = forgotHabit,
            missedOccurrenceDate = missedDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun toggleCheckIn(habitId: String) {
        viewModelScope.launch {
            repository.toggleHabitCheckIn(habitId)
        }
    }

    fun addHabit(
        name: String,
        description: String,
        isEveryDay: Boolean,
        daysOfWeek: Set<Int>,
        notificationEnabled: Boolean,
        notificationTime: String?,
        categoryIcon: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val newHabit = Habit(
                name = name.trim(),
                description = description.trim(),
                isEveryDay = isEveryDay,
                daysOfWeek = daysOfWeek,
                notificationEnabled = notificationEnabled,
                notificationTime = notificationTime?.trim().takeIf { !it.isNullOrBlank() },
                categoryIcon = categoryIcon,
                colorHex = colorHex
            )
            repository.addHabit(newHabit)
        }
    }

    fun reorderHabits(orderedVisibleIds: List<String>) {
        viewModelScope.launch {
            // Only today's habits are on screen: put them back into the slots they occupy
            // in the full list so the hidden habits keep their relative positions.
            val allIds = repository.getHabits().first().map { it.id }
            val visible = orderedVisibleIds.toSet()
            val next = orderedVisibleIds.iterator()
            val merged = allIds.map { if (it in visible && next.hasNext()) next.next() else it }
            repository.reorderHabits(merged)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            if (_selectedHabitForHistory.value?.id == habitId) {
                _selectedHabitForHistory.value = null
            }
            if (_forgotRecordHabit.value?.id == habitId) {
                _forgotRecordHabit.value = null
                _missedOccurrenceDate.value = null
            }
            repository.deleteHabit(habitId)
        }
    }

    fun openHistory(habit: Habit) {
        _selectedHabitForHistory.value = habit
    }

    fun closeHistory() {
        _selectedHabitForHistory.value = null
    }

    fun openForgotToRecord(habit: Habit) {
        val missedDate = habit.pendingMissedDate ?: DateTimeUtils.getYesterdayStartOfDay()
        _forgotRecordHabit.value = habit
        _missedOccurrenceDate.value = missedDate
    }

    fun closeForgotToRecord() {
        _forgotRecordHabit.value = null
        _missedOccurrenceDate.value = null
    }

    fun continueStreak() {
        val habit = _forgotRecordHabit.value ?: return
        val missedDate = _missedOccurrenceDate.value ?: return
        resolveMissedOccurrence(habit.id, missedDate, completed = true)
        closeForgotToRecord()
    }

    fun breakStreak() {
        val habit = _forgotRecordHabit.value ?: return
        val missedDate = _missedOccurrenceDate.value ?: return
        resolveMissedOccurrence(habit.id, missedDate, completed = false)
        closeForgotToRecord()
    }

    fun resolveMissedOccurrence(habitId: String, missedDateTimestamp: Long, completed: Boolean) {
        viewModelScope.launch {
            if (completed) {
                repository.completeMissedOccurrence(habitId, missedDateTimestamp)
            } else {
                repository.breakMissedOccurrence(habitId, missedDateTimestamp)
            }
        }
    }
}
