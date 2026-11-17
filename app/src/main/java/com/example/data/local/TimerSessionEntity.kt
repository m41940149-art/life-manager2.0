package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timer_sessions",
    indices = [Index("startedAt")]
)
data class TimerSessionEntity(
    @PrimaryKey
    val id: String,
    /** SessionType enum name: STUDY / LEARNING / WORK. */
    val type: String,
    val goalMinutes: Int,
    val durationMs: Long,
    val startedAt: Long,
    val endedAt: Long
)
