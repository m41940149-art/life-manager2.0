package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists ORDER BY updatedAt DESC")
    fun getAllChecklists(): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists WHERE id = :id LIMIT 1")
    suspend fun getChecklistById(id: String): ChecklistEntity?

    @Query("SELECT * FROM checklists WHERE folderId = :folderId")
    suspend fun getChecklistsByFolderSync(folderId: String): List<ChecklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Query("UPDATE checklists SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE checklists SET folderId = :newFolderId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToFolder(id: String, newFolderId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklist(id: String)

    @Query("DELETE FROM checklists WHERE folderId = :folderId")
    suspend fun deleteChecklistsByFolder(folderId: String)

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklistsSync(): List<ChecklistEntity>
}
