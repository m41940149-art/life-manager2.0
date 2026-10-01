package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones ORDER BY deletedAt ASC")
    suspend fun getAllTombstones(): List<SyncTombstoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: SyncTombstoneEntity)

    @Query("DELETE FROM sync_tombstones WHERE id = :id")
    suspend fun deleteTombstone(id: String)

    @Query("DELETE FROM sync_tombstones WHERE id IN (:ids)")
    suspend fun deleteTombstones(ids: List<String>)

    @Query("SELECT * FROM sync_tombstones WHERE entityType = :type AND entityId = :entityId LIMIT 1")
    suspend fun getTombstone(type: String, entityId: String): SyncTombstoneEntity?

    @Query("SELECT COUNT(*) FROM sync_tombstones")
    suspend fun getTombstoneCount(): Int
}
