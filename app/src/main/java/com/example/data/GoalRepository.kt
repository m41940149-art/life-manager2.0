package com.example.data

import com.example.model.GoalPlan
import com.example.model.NewGoalTask
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getPlans(): Flow<List<GoalPlan>>
    suspend fun createPlan(title: String, totalWeeks: Int, startDate: Long, firstWeekTasks: List<NewGoalTask>): String
    suspend fun addTasks(planId: String, weekNumber: Int, tasks: List<NewGoalTask>)
    suspend fun deleteTask(taskId: String)
    suspend fun toggleCheck(taskId: String, dayOffset: Int)
    suspend fun deletePlan(planId: String)
}
