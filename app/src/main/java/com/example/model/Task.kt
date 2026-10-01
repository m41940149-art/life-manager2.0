package com.example.model

import java.util.UUID

enum class TaskPriority(val titleAr: String, val titleEn: String) {
    LOW("منخفضة", "Low"),
    MEDIUM("متوسطة", "Medium"),
    HIGH("عالية", "High")
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueDate: Long, // Real epoch milliseconds (start of day)
    val dueTime: String? = null, // Optional time string e.g. "09:30 AM" or "09:30 ص"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "عام"
)
