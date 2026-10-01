package com.example.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "habit_missed_resolutions",
    primaryKeys = ["habitId", "dateTimestamp"],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["dateTimestamp"])
    ]
)
data class HabitMissedResolutionEntity(
    val habitId: String,
    val dateTimestamp: Long, // Start of day timestamp (00:00:00)
    val resolution: String = "BROKEN", // e.g. "BROKEN"
    val resolvedAt: Long = System.currentTimeMillis()
)
