package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goal_plans ORDER BY createdAt DESC")
    fun getPlans(): Flow<List<GoalPlanEntity>>

    @Query("SELECT * FROM goal_tasks ORDER BY createdAt ASC")
    fun getTasks(): Flow<List<GoalTaskEntity>>

    @Query("SELECT * FROM goal_checks")
    fun getChecks(): Flow<List<GoalCheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: GoalPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<GoalTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: GoalCheckEntity): Long

    @Query("DELETE FROM goal_checks WHERE taskId = :taskId AND dayOffset = :dayOffset")
    suspend fun deleteCheck(taskId: String, dayOffset: Int)

    @Query("SELECT COUNT(*) > 0 FROM goal_checks WHERE taskId = :taskId AND dayOffset = :dayOffset")
    suspend fun isChecked(taskId: String, dayOffset: Int): Boolean

    @Query("DELETE FROM goal_checks WHERE taskId = :taskId")
    suspend fun deleteChecksForTask(taskId: String)

    @Query("DELETE FROM goal_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM goal_checks WHERE taskId IN (SELECT id FROM goal_tasks WHERE planId = :planId)")
    suspend fun deleteChecksForPlan(planId: String)

    @Query("DELETE FROM goal_tasks WHERE planId = :planId")
    suspend fun deleteTasksForPlan(planId: String)

    @Query("DELETE FROM goal_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String)

    // ---- Sync helpers (one-shot reads used by FirestoreSyncManager and the repository) ----

    @Query("SELECT * FROM goal_plans")
    suspend fun getAllPlansSync(): List<GoalPlanEntity>

    @Query("SELECT * FROM goal_tasks")
    suspend fun getAllTasksSync(): List<GoalTaskEntity>

    @Query("SELECT * FROM goal_checks")
    suspend fun getAllChecksSync(): List<GoalCheckEntity>

    @Query("SELECT * FROM goal_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): GoalPlanEntity?

    @Query("SELECT * FROM goal_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): GoalTaskEntity?

    @Query("SELECT * FROM goal_tasks WHERE planId = :planId")
    suspend fun getTasksForPlanSync(planId: String): List<GoalTaskEntity>

    @Query("SELECT * FROM goal_checks WHERE taskId = :taskId")
    suspend fun getChecksForTaskSync(taskId: String): List<GoalCheckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(checks: List<GoalCheckEntity>)
}
