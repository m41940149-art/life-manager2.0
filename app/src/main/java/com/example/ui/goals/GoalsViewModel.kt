package com.example.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.GoalRepository
import com.example.model.GoalPlan
import com.example.model.NewGoalTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which screen of the Goals tab is showing. */
sealed class GoalsRoute {
    object Plans : GoalsRoute()
    object Create : GoalsRoute()
    data class Plan(val planId: String) : GoalsRoute()
    data class Week(val planId: String, val week: Int) : GoalsRoute()
    data class AddTasks(val planId: String, val week: Int) : GoalsRoute()
}

data class GoalsUiState(
    val plans: List<GoalPlan> = emptyList(),
    val route: GoalsRoute = GoalsRoute.Plans
)

class GoalsViewModel(
    private val repository: GoalRepository = AppContainer.goalRepository
) : ViewModel() {

    private val _route = MutableStateFlow<GoalsRoute>(GoalsRoute.Plans)

    val uiState: StateFlow<GoalsUiState> = combine(
        repository.getPlans(),
        _route
    ) { plans, route ->
        GoalsUiState(plans = plans, route = route)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState()
    )

    fun navigate(route: GoalsRoute) {
        _route.value = route
    }

    /** One step back in the Goals tab hierarchy. */
    fun back() {
        _route.value = when (val r = _route.value) {
            is GoalsRoute.Plans -> GoalsRoute.Plans
            is GoalsRoute.Create -> GoalsRoute.Plans
            is GoalsRoute.Plan -> GoalsRoute.Plans
            is GoalsRoute.Week -> GoalsRoute.Plan(r.planId)
            is GoalsRoute.AddTasks -> GoalsRoute.Week(r.planId, r.week)
        }
    }

    fun createPlan(title: String, totalWeeks: Int, startDate: Long, firstWeekTasks: List<NewGoalTask>) {
        viewModelScope.launch {
            val id = repository.createPlan(title, totalWeeks, startDate, firstWeekTasks)
            _route.value = GoalsRoute.Plan(id)
        }
    }

    fun addTasks(planId: String, week: Int, tasks: List<NewGoalTask>) {
        viewModelScope.launch {
            repository.addTasks(planId, week, tasks)
            _route.value = GoalsRoute.Week(planId, week)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    fun toggleCheck(taskId: String, dayOffset: Int) {
        viewModelScope.launch { repository.toggleCheck(taskId, dayOffset) }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            _route.value = GoalsRoute.Plans
            repository.deletePlan(planId)
        }
    }
}
