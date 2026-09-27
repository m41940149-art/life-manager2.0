package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Habit
import java.util.Calendar

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val isEveryDay: Boolean = true,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // Comma-separated calendar days
    val notificationEnabled: Boolean = false,
    val notificationTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val colorHex: Long = 0xFF006C5F,
    val categoryIcon: String = "check_circle",
    val syncStatus: String = SyncStatus.PENDING_SYNC.name
)

fun HabitEntity.parseDaysOfWeek(): Set<Int> {
    if (isEveryDay || daysOfWeek.isBlank()) {
        return setOf(1, 2, 3, 4, 5, 6, 7)
    }
    return daysOfWeek.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
}

fun Habit.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    updatedAt: Long = System.currentTimeMillis()
): HabitEntity = HabitEntity(
    id = id,
    name = name,
    description = description,
    isEveryDay = isEveryDay,
    daysOfWeek = daysOfWeek.joinToString(","),
    notificationEnabled = notificationEnabled,
    notificationTime = notificationTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    colorHex = colorHex,
    categoryIcon = categoryIcon,
    syncStatus = syncStatus.name
)
