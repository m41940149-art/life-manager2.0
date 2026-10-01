package com.example.model

import kotlin.math.roundToInt

/** Where a week stands relative to today. */
enum class WeekStatus { LOCKED, ACTIVE, ENDED }

/** A goal inside a week, scheduled on some days (offsets 0..6 from the week's first day). */
data class GoalTask(
    val id: String,
    val planId: String,
    val weekNumber: Int,
    val title: String,
    val days: List<Int>,
    val doneDays: Set<Int>
) {
    val totalSlots: Int get() = days.size
    val doneSlots: Int get() = days.count { it in doneDays }
}

data class GoalWeek(
    val planId: String,
    val number: Int,
    val startDate: Long,
    val status: WeekStatus,
    val tasks: List<GoalTask>
) {
    val hasTasks: Boolean get() = tasks.isNotEmpty()
    val totalSlots: Int get() = tasks.sumOf { it.totalSlots }
    val doneSlots: Int get() = tasks.sumOf { it.doneSlots }
    val percent: Int
        get() = if (totalSlots == 0) 0 else (doneSlots * 100f / totalSlots).roundToInt()
}

data class GoalPlan(
    val id: String,
    val title: String,
    val totalWeeks: Int,
    val startDate: Long,
    val createdAt: Long,
    val weeks: List<GoalWeek>
) {
    val isFinished: Boolean get() = weeks.isNotEmpty() && weeks.all { it.status == WeekStatus.ENDED }
    val currentWeek: GoalWeek? get() = weeks.firstOrNull { it.status == WeekStatus.ACTIVE }
    val overallPercent: Int
        get() {
            val total = weeks.sumOf { it.totalSlots }
            return if (total == 0) 0 else (weeks.sumOf { it.doneSlots } * 100f / total).roundToInt()
        }
}

/** Input used when creating tasks for a week. */
data class NewGoalTask(val title: String, val days: Set<Int>)
