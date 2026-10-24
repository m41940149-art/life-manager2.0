package com.example.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.DataRepository
import com.example.model.Checklist
import com.example.model.Folder
import com.example.model.TextCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PersonalDataUiState(
    val folders: List<Folder> = emptyList(),
    val allCards: List<TextCard> = emptyList(),
    val allChecklists: List<Checklist> = emptyList(),
    val currentFolder: Folder? = null,
    val displayedCards: List<TextCard> = emptyList(),
    val displayedChecklists: List<Checklist> = emptyList(),
    val searchQuery: String = "",
    val showOnlyFavorites: Boolean = false
)

class PersonalDataViewModel(
    private val repository: DataRepository = AppContainer.dataRepository
) : ViewModel() {

    // Keeps autosave writes (insert -> update -> delete) in the order they were requested
    private val writeMutex = Mutex()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _showOnlyFavorites = MutableStateFlow(false)

    // Text cards and checklists travel together so the 5-argument combine below still fits
    private val contentFlow = combine(
        repository.getTextCards(),
        repository.getChecklists()
    ) { cards, checklists -> cards to checklists }

    val uiState: StateFlow<PersonalDataUiState> = combine(
        repository.getFolders(),
        contentFlow,
        _currentFolderId,
        _searchQuery,
        _showOnlyFavorites
    ) { folders, content, currentFolderId, query, onlyFavs ->
        val (cards, checklists) = content
        val currentFolder = folders.find { it.id == currentFolderId }

        val cardsInScope = when {
            onlyFavs -> cards.filter { it.isFavorite }
            currentFolderId != null -> cards.filter { it.folderId == currentFolderId }
            else -> cards.filter { it.folderId == null } // Root shows unfiled cards
        }
        val listsInScope = when {
            onlyFavs -> checklists.filter { it.isFavorite }
            currentFolderId != null -> checklists.filter { it.folderId == currentFolderId }
            else -> checklists.filter { it.folderId == null }
        }

        val filteredCards: List<TextCard>
        val filteredLists: List<Checklist>
        if (query.isBlank()) {
            filteredCards = cardsInScope
            filteredLists = listsInScope
        } else {
            // When user searches, search across relevant cards or all cards if at root
            val cardsBase = if (currentFolderId != null) cardsInScope else cards
            val listsBase = if (currentFolderId != null) listsInScope else checklists
            filteredCards = cardsBase.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
            filteredLists = listsBase.filter { list ->
                list.title.contains(query, ignoreCase = true) ||
                        list.items.any {
                            it.text.contains(query, ignoreCase = true) ||
                                    it.note.contains(query, ignoreCase = true)
                        }
            }
        }

        PersonalDataUiState(
            folders = folders,
            allCards = cards,
            allChecklists = checklists,
            currentFolder = currentFolder,
            displayedCards = filteredCards,
            displayedChecklists = filteredLists,
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

    fun saveTextCard(card: TextCard, isNew: Boolean) {
        viewModelScope.launch {
            writeMutex.withLock {
                if (isNew) repository.addTextCard(card) else repository.updateTextCard(card)
            }
        }
    }

    fun deleteTextCard(cardId: String) {
        viewModelScope.launch {
            writeMutex.withLock {
                repository.deleteTextCard(cardId)
            }
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

    fun saveChecklist(checklist: Checklist, isNew: Boolean) {
        viewModelScope.launch {
            writeMutex.withLock {
                if (isNew) repository.addChecklist(checklist) else repository.updateChecklist(checklist)
            }
        }
    }

    fun deleteChecklist(checklistId: String) {
        viewModelScope.launch {
            writeMutex.withLock {
                repository.deleteChecklist(checklistId)
            }
        }
    }

    fun toggleChecklistFavorite(checklistId: String) {
        viewModelScope.launch {
            repository.toggleChecklistFavorite(checklistId)
        }
    }

    fun moveChecklist(checklistId: String, newFolderId: String?) {
        viewModelScope.launch {
            repository.moveChecklistToFolder(checklistId, newFolderId)
        }
    }
}
