package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_tombstones")
data class SyncTombstoneEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "TASK", "HABIT", "HABIT_COMPLETION", "FOLDER", "TEXT_CARD", "SURVEY", "SURVEY_RESPONSE"
    val entityId: String,
    val extraId: String? = null,
    val deletedAt: Long = System.currentTimeMillis()
)
