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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonalDataUiState(
    val folders: List<Folder> = emptyList(),
    val allCards: List<TextCard> = emptyList(),
    val currentFolder: Folder? = null,
    val displayedCards: List<TextCard> = emptyList(),
    val searchQuery: String = "",
    val showOnlyFavorites: Boolean = false
)

class PersonalDataViewModel(
    private val repository: DataRepository = AppContainer.dataRepository
) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _showOnlyFavorites = MutableStateFlow(false)

    val uiState: StateFlow<PersonalDataUiState> = combine(
        repository.getFolders(),
        repository.getTextCards(),
        _currentFolderId,
        _searchQuery,
        _showOnlyFavorites
    ) { folders, cards, currentFolderId, query, onlyFavs ->
        val currentFolder = folders.find { it.id == currentFolderId }

        val cardsInScope = when {
            onlyFavs -> cards.filter { it.isFavorite }
            currentFolderId != null -> cards.filter { it.folderId == currentFolderId }
            else -> cards.filter { it.folderId == null } // Root shows unfiled cards
        }

        val filteredCards = if (query.isBlank()) {
            cardsInScope
        } else {
            // When user searches, search across relevant cards or all cards if at root
            val searchBase = if (currentFolderId != null) cardsInScope else cards
            searchBase.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }

        PersonalDataUiState(
            folders = folders,
            allCards = cards,
            currentFolder = currentFolder,
            displayedCards = filteredCards,
            searchQuery = query,
            showOnlyFavorites = onlyFavs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonalDataUiState()
    )

    fun openFolder(folderId: String) {
        _currentFolderId.value = folderId
        _showOnlyFavorites.value = false
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

    fun setShowOnlyFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
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

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(folderId, newName.trim())
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
        isFavorite: Boolean = false,
        isPasswordProtected: Boolean = false,
        passwordHash: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val card = TextCard(
                title = title.trim(),
                content = content.trim(),
                folderId = folderId,
                isFavorite = isFavorite,
                isPasswordProtected = isPasswordProtected,
                passwordHash = passwordHash,
                tags = tags
            )
            repository.addTextCard(card)
        }
    }

    fun updateTextCard(card: TextCard) {
        viewModelScope.launch {
            repository.updateTextCard(card)
        }
    }

    fun deleteTextCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteTextCard(cardId)
        }
    }

    fun toggleFavorite(cardId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(cardId)
        }
    }

    fun moveCard(cardId: String, newFolderId: String?) {
        viewModelScope.launch {
            repository.moveCardToFolder(cardId, newFolderId)
        }
    }
}
