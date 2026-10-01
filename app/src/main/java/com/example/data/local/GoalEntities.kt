package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goal_plans")
data class GoalPlanEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val totalWeeks: Int,
    val startDate: Long, // start-of-day of the chosen first day
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "goal_tasks",
    indices = [Index(value = ["planId"])]
)
data class GoalTaskEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val weekNumber: Int,
    val title: String,
    val days: String, // comma-separated day offsets 0..6 from the first day of the week
    val createdAt: Long
)

fun GoalTaskEntity.parseDays(): List<Int> =
    days.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.distinct().sorted()

@Entity(
    tableName = "goal_checks",
    primaryKeys = ["taskId", "dayOffset"],
    indices = [Index(value = ["taskId"])]
)
data class GoalCheckEntity(
    val taskId: String,
    val dayOffset: Int,
    val completedAt: Long
)
