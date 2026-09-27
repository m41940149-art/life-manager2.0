package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import com.example.model.HabitCompletion

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dateTimestamp"],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["dateTimestamp"])
    ]
)
data class HabitCompletionEntity(
    val habitId: String,
    val dateTimestamp: Long, // Start of day timestamp (00:00:00)
    val completedAt: Long = System.currentTimeMillis()
)

fun HabitCompletionEntity.toDomain() = HabitCompletion(
    habitId = habitId,
    dateTimestamp = dateTimestamp,
    completedAt = completedAt
)

fun HabitCompletion.toEntity() = HabitCompletionEntity(
    habitId = habitId,
    dateTimestamp = dateTimestamp,
    completedAt = completedAt
)
