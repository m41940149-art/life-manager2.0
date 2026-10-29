package com.example.model

import java.util.Calendar
import java.util.UUID

enum class TaskPriority(val titleAr: String, val titleEn: String) {
    LOW("منخفضة", "Low"),
    MEDIUM("متوسطة", "Medium"),
    HIGH("عالية", "High")
}

enum class RecurrenceType(val titleAr: String, val titleEn: String) {
    NONE("بدون تكرار", "No repeat"),
    DAILY("يومياً", "Daily"),
    WEEKLY("أسبوعياً", "Weekly"),
    CUSTOM("أيام محددة", "Specific days");

    companion object {
        fun fromName(name: String?): RecurrenceType =
            values().firstOrNull { it.name == name } ?: NONE
    }
}

val ALL_WEEK_DAYS: Set<Int> = setOf(
    Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
)

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
    val category: String = "عام",
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    // Calendar.SUNDAY(1)..Calendar.SATURDAY(7). Always filled when recurrenceType != NONE.
    val recurrenceDays: Set<Int> = emptySet(),
    // All occurrences of one repeating task share the same seriesId.
    val seriesId: String? = null
) {
    val isRecurring: Boolean get() = recurrenceType != RecurrenceType.NONE
}
