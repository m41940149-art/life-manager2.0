package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Task
import com.example.model.TaskPriority

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    PENDING_DELETE
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val dueDate: Long,
    val dueTime: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val priority: String = "MEDIUM",
    val category: String = "عام",
    val syncStatus: String = SyncStatus.PENDING_SYNC.name
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    isCompleted = isCompleted,
    createdAt = createdAt,
    completedAt = completedAt,
    priority = try {
        TaskPriority.valueOf(priority)
    } catch (_: Exception) {
        TaskPriority.MEDIUM
    },
    category = category
)

fun Task.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    updatedAt: Long = System.currentTimeMillis()
): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    isCompleted = isCompleted,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    priority = priority.name,
    category = category,
    syncStatus = syncStatus.name
)
