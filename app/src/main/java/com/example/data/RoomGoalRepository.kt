package com.example.data

import com.example.data.local.GoalCheckEntity
import com.example.data.local.GoalDao
import com.example.data.local.GoalPlanEntity
import com.example.data.local.GoalTaskEntity
import com.example.data.local.parseDays
import com.example.model.GoalPlan
import com.example.model.GoalTask
import com.example.model.GoalWeek
import com.example.model.NewGoalTask
import com.example.util.DateTimeUtils
import com.example.util.GoalProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.UUID

class RoomGoalRepository(
    private val goalDao: GoalDao
) : GoalRepository {

    /** Emits start-of-today and re-emits when the day changes while the app stays open. */
    private fun todayFlow(): Flow<Long> = flow {
        while (true) {
            emit(DateTimeUtils.getTodayStartOfDay())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    override fun getPlans(): Flow<List<GoalPlan>> {
        return combine(
            goalDao.getPlans(),
            goalDao.getTasks(),
            goalDao.getChecks(),
            todayFlow()
        ) { plans, tasks, checks, today ->
            val tasksByPlan = tasks.groupBy { it.planId }
            val checksByTask = checks.groupBy { it.taskId }

            plans.map { plan ->
                val planTasks = tasksByPlan[plan.id].orEmpty()
                val weeks = (1..plan.totalWeeks).map { number ->
                    val weekTasks = planTasks
                        .filter { it.weekNumber == number }
                        .map { t ->
                            GoalTask(
                                id = t.id,
                                planId = plan.id,
                                weekNumber = number,
                                title = t.title,
                                days = t.parseDays(),
                                doneDays = checksByTask[t.id].orEmpty().map { it.dayOffset }.toSet()
                            )
                        }
                    GoalWeek(
                        planId = plan.id,
                        number = number,
                        startDate = GoalProgress.weekStart(plan.startDate, number),
                        status = GoalProgress.statusOf(plan.startDate, number, today),
                        tasks = weekTasks
                    )
                }
                GoalPlan(
                    id = plan.id,
                    title = plan.title,
                    totalWeeks = plan.totalWeeks,
                    startDate = plan.startDate,
                    createdAt = plan.createdAt,
                    weeks = weeks
                )
            }
        }
    }

    override suspend fun createPlan(
        title: String,
        totalWeeks: Int,
        startDate: Long,
        firstWeekTasks: List<NewGoalTask>
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        goalDao.insertTasks(firstWeekTasks.map { it.toEntity(id, 1, now) })
        goalDao.insertPlan(
            GoalPlanEntity(
                id = id,
                title = title.trim(),
                totalWeeks = totalWeeks.coerceIn(1, 52),
                startDate = DateTimeUtils.getStartOfDay(startDate),
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    override suspend fun addTasks(planId: String, weekNumber: Int, tasks: List<NewGoalTask>) {
        if (tasks.isEmpty()) return
        val now = System.currentTimeMillis()
        goalDao.insertTasks(tasks.map { it.toEntity(planId, weekNumber, now) })
    }

    override suspend fun deleteTask(taskId: String) {
        goalDao.deleteChecksForTask(taskId)
        goalDao.deleteTask(taskId)
    }

    override suspend fun toggleCheck(taskId: String, dayOffset: Int) {
        if (goalDao.isChecked(taskId, dayOffset)) {
            goalDao.deleteCheck(taskId, dayOffset)
        } else {
            goalDao.insertCheck(GoalCheckEntity(taskId, dayOffset, System.currentTimeMillis()))
        }
    }

    override suspend fun deletePlan(planId: String) {
        goalDao.deleteChecksForPlan(planId)
        goalDao.deleteTasksForPlan(planId)
        goalDao.deletePlan(planId)
    }

    private fun NewGoalTask.toEntity(planId: String, weekNumber: Int, createdAt: Long) = GoalTaskEntity(
        id = UUID.randomUUID().toString(),
        planId = planId,
        weekNumber = weekNumber,
        title = title.trim(),
        days = days.filter { it in 0..6 }.sorted().joinToString(","),
        createdAt = createdAt
    )
}
