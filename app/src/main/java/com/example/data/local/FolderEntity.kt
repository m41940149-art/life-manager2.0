package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val colorHex: Long = 0xFF006C5F,
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING_SYNC.name
)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Folder.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    updatedAt: Long = this.updatedAt
): FolderEntity = FolderEntity(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name
)
