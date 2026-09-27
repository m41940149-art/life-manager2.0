package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE syncStatus != 'PENDING_DELETE' ORDER BY dueDate ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateTaskCompletion(
        id: String,
        isCompleted: Boolean,
        completedAt: Long?,
        updatedAt: Long,
        syncStatus: String = SyncStatus.PENDING_SYNC.name
    )

    @Query("SELECT COUNT(*) FROM tasks WHERE syncStatus != 'PENDING_DELETE'")
    suspend fun getTaskCount(): Int
}
