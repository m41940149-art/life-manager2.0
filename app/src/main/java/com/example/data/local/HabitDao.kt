package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE syncStatus != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("SELECT * FROM habit_completions ORDER BY dateTimestamp DESC")
    fun getAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateTimestamp DESC")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateTimestamp ASC")
    suspend fun getCompletionsListForHabit(habitId: String): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateTimestamp = :dateTimestamp")
    suspend fun deleteCompletion(habitId: String, dateTimestamp: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: String)

    @Query("SELECT COUNT(*) > 0 FROM habit_completions WHERE habitId = :habitId AND dateTimestamp = :dateTimestamp")
    suspend fun isCompletedOnDate(habitId: String, dateTimestamp: Long): Boolean

    @Query("SELECT COUNT(*) FROM habits WHERE syncStatus != 'PENDING_DELETE'")
    suspend fun getHabitCount(): Int

    // Missed resolutions (e.g. user selected "Break the streak" for a missed day)
    @Query("SELECT * FROM habit_missed_resolutions ORDER BY dateTimestamp DESC")
    fun getAllMissedResolutions(): Flow<List<HabitMissedResolutionEntity>>

    @Query("SELECT * FROM habit_missed_resolutions WHERE habitId = :habitId ORDER BY dateTimestamp DESC")
    fun getMissedResolutionsForHabit(habitId: String): Flow<List<HabitMissedResolutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissedResolution(resolution: HabitMissedResolutionEntity)

    @Query("DELETE FROM habit_missed_resolutions WHERE habitId = :habitId AND dateTimestamp = :dateTimestamp")
    suspend fun deleteMissedResolution(habitId: String, dateTimestamp: Long)

    @Query("DELETE FROM habit_missed_resolutions WHERE habitId = :habitId")
    suspend fun deleteMissedResolutionsForHabit(habitId: String)
}
