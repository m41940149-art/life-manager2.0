package com.example.data

import com.example.data.local.FolderDao
import com.example.data.local.SyncTombstoneDao
import com.example.data.local.SyncTombstoneEntity
import com.example.data.local.TextCardDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.Folder
import com.example.model.TextCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDataRepository(
    private val folderDao: FolderDao,
    private val textCardDao: TextCardDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : DataRepository {

    override fun getFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTextCards(): Flow<List<TextCard>> {
        return textCardDao.getAllCards().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addFolder(folder: Folder) {
        folderDao.insertFolder(folder.toEntity())
    }

    override suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.toEntity())
    }

    override suspend fun renameFolder(folderId: String, newName: String) {
        folderDao.renameFolder(folderId, newName)
    }

    override suspend fun deleteFolder(folderId: String) {
        textCardDao.deleteCardsByFolder(folderId)
        folderDao.deleteFolder(folderId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "FOLDER",
                entityId = folderId
            )
        )
    }

    override suspend fun addTextCard(card: TextCard) {
        textCardDao.insertCard(card.toEntity())
    }

    override suspend fun updateTextCard(card: TextCard) {
        textCardDao.updateCard(card.toEntity(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteTextCard(cardId: String) {
        textCardDao.deleteCard(cardId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "TEXT_CARD",
                entityId = cardId
            )
        )
    }

    override suspend fun toggleFavorite(cardId: String) {
        val existing = textCardDao.getCardById(cardId) ?: return
        textCardDao.setFavorite(cardId, !existing.isFavorite)
    }

    override suspend fun moveCardToFolder(cardId: String, newFolderId: String?) {
        textCardDao.moveCardToFolder(cardId, newFolderId)
    }
}
