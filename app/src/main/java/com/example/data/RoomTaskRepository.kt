package com.example.data

import com.example.data.local.SyncStatus
import com.example.data.local.SyncTombstoneDao
import com.example.data.local.SyncTombstoneEntity
import com.example.data.local.TaskDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.Task
import com.example.util.DateTimeUtils
import com.example.util.TaskRecurrence
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val taskDao: TaskDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(
            task.toEntity(
                syncStatus = SyncStatus.PENDING_SYNC,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateTask(task: Task) {
        val existing = taskDao.getTaskById(task.id)
        val createdAt = existing?.createdAt ?: task.createdAt
        val entity = task.copy(createdAt = createdAt).toEntity(
            syncStatus = SyncStatus.PENDING_SYNC,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(entity)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTaskById(taskId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "TASK",
                entityId = taskId
            )
        )
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val willBeCompleted = !entity.isCompleted
        val completedAt = if (willBeCompleted) System.currentTimeMillis() else null
        val updatedAt = System.currentTimeMillis()

        taskDao.updateTaskCompletion(
            id = taskId,
            isCompleted = willBeCompleted,
            completedAt = completedAt,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.PENDING_SYNC.name
        )

        val task = entity.toDomain()
        val seriesId = task.seriesId
        if (!task.isRecurring || seriesId == null) return

        if (willBeCompleted) {
            spawnNextOccurrence(task, seriesId)
        } else {
            // Un-completing: remove the occurrence that was auto-created after it.
            taskDao.getPendingInSeriesAfter(seriesId, task.dueDate).forEach { deleteTask(it.id) }
        }
    }

    /**
     * Creates the next occurrence of a repeating task after it is completed.
     * Based on the later of (due date, today) so completing late never creates a past task.
     */
    private suspend fun spawnNextOccurrence(task: Task, seriesId: String) {
        if (taskDao.getPendingInSeriesAfter(seriesId, task.dueDate).isNotEmpty()) return
        val base = maxOf(task.dueDate, DateTimeUtils.getTodayStartOfDay())
        val nextDate = TaskRecurrence.nextOccurrenceAfter(base, task.recurrenceDays) ?: return
        addTask(
            task.copy(
                id = UUID.randomUUID().toString(),
                dueDate = nextDate,
                isCompleted = false,
                completedAt = null,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
