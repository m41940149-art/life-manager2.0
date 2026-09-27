package com.example.data

import com.example.model.Folder
import com.example.model.TextCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface DataRepository {
    fun getFolders(): Flow<List<Folder>>
    fun getTextCards(): Flow<List<TextCard>>
    suspend fun addFolder(folder: Folder)
    suspend fun deleteFolder(folderId: String)
    suspend fun addTextCard(card: TextCard)
    suspend fun updateTextCard(card: TextCard)
    suspend fun deleteTextCard(cardId: String)
}

class InMemoryDataRepository : DataRepository {
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    private val _cards = MutableStateFlow<List<TextCard>>(emptyList())

    override fun getFolders(): Flow<List<Folder>> = _folders.asStateFlow()

    override fun getTextCards(): Flow<List<TextCard>> = _cards.asStateFlow()

    override suspend fun addFolder(folder: Folder) {
        _folders.update { listOf(folder) + it }
    }

    override suspend fun deleteFolder(folderId: String) {
        _folders.update { list -> list.filterNot { it.id == folderId } }
        _cards.update { list -> list.filterNot { it.folderId == folderId } }
    }

    override suspend fun addTextCard(card: TextCard) {
        _cards.update { listOf(card) + it }
    }

    override suspend fun updateTextCard(card: TextCard) {
        _cards.update { list ->
            list.map { if (it.id == card.id) card else it }
        }
    }

    override suspend fun deleteTextCard(cardId: String) {
        _cards.update { list -> list.filterNot { it.id == cardId } }
    }
}
