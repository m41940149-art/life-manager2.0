package com.example.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.DataRepository
import com.example.model.Folder
import com.example.model.TextCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonalDataUiState(
    val folders: List<Folder> = emptyList(),
    val allCards: List<TextCard> = emptyList(),
    val currentFolder: Folder? = null,
    val displayedCards: List<TextCard> = emptyList(),
    val searchQuery: String = ""
)

class PersonalDataViewModel(
    private val repository: DataRepository = AppContainer.dataRepository
) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PersonalDataUiState> = combine(
        repository.getFolders(),
        repository.getTextCards(),
        _currentFolderId,
        _searchQuery
    ) { folders, cards, currentFolderId, query ->
        val currentFolder = folders.find { it.id == currentFolderId }

        val cardsInScope = if (currentFolderId == null) {
            cards // At root, show all or unfiled cards
        } else {
            cards.filter { it.folderId == currentFolderId }
        }

        val filteredCards = if (query.isBlank()) {
            cardsInScope
        } else {
            cardsInScope.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        PersonalDataUiState(
            folders = folders,
            allCards = cards,
            currentFolder = currentFolder,
            displayedCards = filteredCards,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonalDataUiState()
    )

    fun openFolder(folderId: String) {
        _currentFolderId.value = folderId
    }

    fun navigateBackToRoot(): Boolean {
        if (_currentFolderId.value != null) {
            _currentFolderId.value = null
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addFolder(name: String, description: String, colorHex: Long, iconName: String) {
        viewModelScope.launch {
            val folder = Folder(
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                iconName = iconName
            )
            repository.addFolder(folder)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if (_currentFolderId.value == folderId) {
                _currentFolderId.value = null
            }
            repository.deleteFolder(folderId)
        }
    }

    fun addTextCard(
        title: String,
        content: String,
        folderId: String?,
        isPasswordProtected: Boolean,
        tags: List<String>
    ) {
        viewModelScope.launch {
            val card = TextCard(
                title = title.trim(),
                content = content.trim(),
                folderId = folderId,
                isPasswordProtected = isPasswordProtected,
                tags = tags
            )
            repository.addTextCard(card)
        }
    }

    fun deleteTextCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteTextCard(cardId)
        }
    }
}
