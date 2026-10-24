package com.example.ui.data

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Checklist
import com.example.model.Folder
import com.example.model.TextCard
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDataScreen(
    viewModel: PersonalDataViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }
    var checklistEditorRequest by remember { mutableStateOf<ChecklistEditorRequest?>(null) }
    var showMoveSelected by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cards / checklists hidden from the list while the "Undo" snackbar is showing.
    // They are really deleted only when the snackbar closes without Undo.
    val pendingDeletes = remember { mutableStateListOf<TextCard>() }
    val pendingChecklistDeletes = remember { mutableStateListOf<Checklist>() }
    val visibleCards = uiState.displayedCards.filter { c -> pendingDeletes.none { it.id == c.id } }
    val visibleChecklists = uiState.displayedChecklists.filter { c -> pendingChecklistDeletes.none { it.id == c.id } }
    // Text cards and checklists share one grid, newest first
    val entries: List<DataEntry> = (
        visibleCards.map { DataEntry.Note(it) } + visibleChecklists.map { DataEntry.Todo(it) }
        ).sortedByDescending { it.updatedAt }
    val selectionMode = selectedIds.isNotEmpty()
    val selectedEntries = entries.filter { it.id in selectedIds }
    val selectedCards = visibleCards.filter { it.id in selectedIds }
    val selectedChecklists = visibleChecklists.filter { it.id in selectedIds }

    val commitDelete: (List<TextCard>, List<Checklist>) -> Unit = { cards, lists ->
        cards.forEach { c ->
            if (pendingDeletes.removeAll { it.id == c.id }) viewModel.deleteTextCard(c.id)
        }
        lists.forEach { l ->
            if (pendingChecklistDeletes.removeAll { it.id == l.id }) viewModel.deleteChecklist(l.id)
        }
    }
    val requestDelete: (List<TextCard>, List<Checklist>) -> Unit = { cards, lists ->
        val count = cards.size + lists.size
        if (count > 0) {
            snackbarHostState.currentSnackbarData?.dismiss()
            pendingDeletes.addAll(cards)
            pendingChecklistDeletes.addAll(lists)
            selectedIds = emptySet()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = if (count == 1) {
                        if (lists.isNotEmpty()) {
                            if (isArabic) "تم حذف القائمة" else "List deleted"
                        } else {
                            if (isArabic) "تم حذف البطاقة" else "Card deleted"
                        }
                    } else {
                        if (isArabic) "تم حذف $count عناصر" else "$count items deleted"
                    },
                    actionLabel = if (isArabic) "تراجع" else "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    pendingDeletes.removeAll { c -> cards.any { it.id == c.id } }
                    pendingChecklistDeletes.removeAll { c -> lists.any { it.id == c.id } }
                } else {
                    commitDelete(cards, lists)
                }
            }
        }
    }
    // Leaving the screen while a delete is pending: finish the delete
    DisposableEffect(Unit) {
        onDispose { commitDelete(pendingDeletes.toList(), pendingChecklistDeletes.toList()) }
    }
    // Drop selections that are no longer visible (folder switched, searched, deleted...)
    LaunchedEffect(entries) {
        val ids = entries.map { it.id }.toSet()
        if (!ids.containsAll(selectedIds)) selectedIds = selectedIds intersect ids
    }
    LaunchedEffect(selectionMode) { if (!selectionMode) showMoveSelected = false }

    // Android Back leaves the open folder first
    BackHandler(enabled = uiState.currentFolder != null) {
        viewModel.navigateBackToRoot()
    }
    // Registered last, so it wins: Back first collapses the "+" menu
    BackHandler(enabled = fabExpanded) { fabExpanded = false }
    BackHandler(enabled = selectionMode) { selectedIds = emptySet() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!selectionMode) ExpandableAddFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                isArabic = isArabic,
                onAddCard = {
                    fabExpanded = false
                    editorRequest = EditorRequest(card = null)
                },
                onAddChecklist = {
                    fabExpanded = false
                    checklistEditorRequest = ChecklistEditorRequest(checklist = null)
                },
                onAddFolder = {
                    fabExpanded = false
                    showAddFolderDialog = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectionMode) {
                SelectionBar(
                    count = selectedIds.size,
                    isArabic = isArabic,
                    allFavorite = selectedEntries.isNotEmpty() && selectedEntries.all { it.isFavorite },
                    onClose = { selectedIds = emptySet() },
                    onToggleFavorite = {
                        val allFav = selectedEntries.all { it.isFavorite }
                        selectedCards.filter { it.isFavorite == allFav }.forEach { viewModel.toggleFavorite(it.id) }
                        selectedChecklists.filter { it.isFavorite == allFav }.forEach { viewModel.toggleChecklistFavorite(it.id) }
                        selectedIds = emptySet()
                    },
                    onMove = { showMoveSelected = true },
                    onDelete = { requestDelete(selectedCards, selectedChecklists) }
                )
            } else {
            // Keep-style rounded search pill
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            if (isArabic) "ابحث في بطاقاتك وقوائمك" else "Search your cards and lists",
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = if (isArabic) "بحث" else "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = if (isArabic) "مسح" else "Clear",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("data_search_input")
                )
            }

            // General | Favorites | folders... | + Folder
            FolderChipsRow(
                folders = uiState.folders,
                currentFolder = uiState.currentFolder,
                showOnlyFavorites = uiState.showOnlyFavorites,
                isArabic = isArabic,
                onSelectGeneral = {
                    viewModel.navigateBackToRoot()
                    viewModel.setShowOnlyFavorites(false)
                },
                onSelectFavorites = {
                    viewModel.navigateBackToRoot()
                    viewModel.setShowOnlyFavorites(true)
                },
                onSelectFolder = { viewModel.openFolder(it.id) },
                onRenameFolder = { folderToRename = it },
                onDeleteFolder = { folderToDelete = it }
            )

            // Masonry grid (2 columns, height follows content)
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entries.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        val emptyTitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "لا توجد نتائج بحث" else "No matching cards"
                            uiState.showOnlyFavorites -> if (isArabic) "لا توجد بطاقات مفضلة بعد" else "No favorite cards yet"
                            uiState.currentFolder != null -> if (isArabic) "لا توجد عناصر في هذا المجلد" else "Nothing in this folder yet"
                            uiState.folders.isEmpty() -> if (isArabic) "لا توجد مجلدات أو بطاقات بعد" else "No folders or cards yet"
                            else -> if (isArabic) "لا توجد بطاقات عامة بعد" else "No unfiled cards yet"
                        }

                        val emptySubtitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "جرب كلمة بحث أخرى." else "Try a different search term."
                            uiState.showOnlyFavorites -> if (isArabic) "افتح أي بطاقة واضغط على النجمة لإضافتها إلى المفضلة." else "Open any card and tap the star to add it to favorites."
                            uiState.currentFolder != null -> if (isArabic) "ابدأ بإضافة بطاقة نصية أو قائمة داخل هذا المجلد." else "Start adding text cards or checklists to this folder."
                            else -> if (isArabic) "أنشئ بطاقات نصية لحفظ الملاحظات، أو قوائم للأفلام والكتب والتسوق." else "Create text cards for notes, or checklists for movies, books and shopping."
                        }

                        EmptyStateView(
                            icon = Icons.Default.NoteAdd,
                            title = emptyTitle,
                            subtitle = emptySubtitle,
                            actionButtonText = if (uiState.searchQuery.isBlank()) (if (isArabic) "+ إضافة بطاقة نصية" else "+ Add Text Card") else null,
                            onActionClick = { editorRequest = EditorRequest(card = null) }
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        val toggleSelected = {
                            selectedIds = if (entry.id in selectedIds) selectedIds - entry.id else selectedIds + entry.id
                        }
                        when (entry) {
                            is DataEntry.Note -> TextDataCard(
                                card = entry.card,
                                isArabic = isArabic,
                                selected = entry.id in selectedIds,
                                onClick = {
                                    if (selectionMode) toggleSelected()
                                    else editorRequest = EditorRequest(card = entry.card)
                                },
                                onLongClick = toggleSelected
                            )
                            is DataEntry.Todo -> ChecklistDataCard(
                                checklist = entry.checklist,
                                isArabic = isArabic,
                                selected = entry.id in selectedIds,
                                onClick = {
                                    if (selectionMode) toggleSelected()
                                    else checklistEditorRequest = ChecklistEditorRequest(checklist = entry.checklist)
                                },
                                onLongClick = toggleSelected
                            )
                        }
                    }
                }
            }
        }

        // Scrim behind the expanded "+" menu: tap anywhere to collapse
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { fabExpanded = false }
            )
        }

        // Dialog: Add Folder
        if (showAddFolderDialog) {
            AddFolderDialog(
                isArabic = isArabic,
                onDismiss = { showAddFolderDialog = false },
                onAddFolder = { name, description, colorHex ->
                    viewModel.addFolder(name, description, colorHex, "folder")
                }
            )
        }

        // Dialog: Rename Folder
        folderToRename?.let { folder ->
            RenameFolderDialog(
                folder = folder,
                isArabic = isArabic,
                onDismiss = { folderToRename = null },
                onRename = { newName ->
                    viewModel.renameFolder(folder.id, newName)
                }
            )
        }

        // Dialog: Delete Folder Confirmation (explains what will happen)
        folderToDelete?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = {
                    Text(
                        text = if (isArabic) "حذف المجلد" else "Delete Folder",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isArabic) {
                            "هل أنت متأكد من حذف مجلد \"${folder.name}\"؟ سيتم حذف جميع البطاقات الموجودة بداخله نهائياً."
                        } else {
                            "Are you sure you want to delete folder \"${folder.name}\"? All cards inside it will be permanently deleted."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFolder(folder.id)
                            folderToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_delete_folder_btn")
                    ) {
                        Text(if (isArabic) "حذف المجلد" else "Delete Folder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Dialog: Move the selected cards / checklists to a folder
        if (showMoveSelected && selectedEntries.isNotEmpty()) {
            MoveCardDialog(
                card = TextCard(
                    title = selectedEntries.first().title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" },
                    content = "",
                    folderId = selectedEntries.map { it.folderId }.distinct().singleOrNull()
                ),
                selectedCount = selectedEntries.size,
                folders = uiState.folders,
                isArabic = isArabic,
                onDismiss = { showMoveSelected = false },
                onMove = { newFolderId ->
                    selectedCards.forEach { viewModel.moveCard(it.id, newFolderId) }
                    selectedChecklists.forEach { viewModel.moveChecklist(it.id, newFolderId) }
                    selectedIds = emptySet()
                    showMoveSelected = false
                }
            )
        }

        // Full-screen editor (create + view + edit, autosaves)
        editorRequest?.let { request ->
            key(request.key) {
                CardEditorDialog(
                    card = request.card,
                    initialFolderId = uiState.currentFolder?.id,
                    folders = uiState.folders,
                    isArabic = isArabic,
                    onSave = { card, isNew -> viewModel.saveTextCard(card, isNew) },
                    onDelete = { id -> viewModel.deleteTextCard(id) },
                    onDeleteWithUndo = { card -> requestDelete(listOf(card), emptyList()) },
                    onClose = { editorRequest = null }
                )
            }
        }

        // Full-screen checklist editor (create + edit, autosaves)
        checklistEditorRequest?.let { request ->
            key(request.key) {
                ChecklistEditorDialog(
                    checklist = request.checklist,
                    initialFolderId = uiState.currentFolder?.id,
                    folders = uiState.folders,
                    isArabic = isArabic,
                    onSave = { list, isNew -> viewModel.saveChecklist(list, isNew) },
                    onDelete = { id -> viewModel.deleteChecklist(id) },
                    onDeleteWithUndo = { list -> requestDelete(emptyList(), listOf(list)) },
                    onClose = { checklistEditorRequest = null }
                )
            }
        }
    }
}

/** Identifies one editor session; [key] makes every opening start from a clean state. */
private data class EditorRequest(
    val card: TextCard?,
    val key: Long = System.nanoTime()
)

/** Same idea as [EditorRequest], for the checklist editor. */
private data class ChecklistEditorRequest(
    val checklist: Checklist?,
    val key: Long = System.nanoTime()
)

/** One tile in the grid: a text card or a checklist. */
private sealed interface DataEntry {
    val id: String
    val title: String
    val folderId: String?
    val isFavorite: Boolean
    val updatedAt: Long

    data class Note(val card: TextCard) : DataEntry {
        override val id get() = card.id
        override val title get() = card.title
        override val folderId get() = card.folderId
        override val isFavorite get() = card.isFavorite
        override val updatedAt get() = card.updatedAt
    }

    data class Todo(val checklist: Checklist) : DataEntry {
        override val id get() = checklist.id
        override val title get() = checklist.title
        override val folderId get() = checklist.folderId
        override val isFavorite get() = checklist.isFavorite
        override val updatedAt get() = checklist.updatedAt
    }
}

/** Contextual bar shown instead of the search pill while cards are selected. */
@Composable
private fun SelectionBar(
    count: Int,
    isArabic: Boolean,
    allFavorite: Boolean,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("selection_close_btn")) {
            Icon(Icons.Default.Close, contentDescription = if (isArabic) "إلغاء التحديد" else "Clear selection")
        }
        Text(
            text = if (isArabic) "$count محددة" else "$count selected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("selection_favorite_btn")) {
            Icon(
                imageVector = if (allFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                contentDescription = if (isArabic) "مفضلة" else "Favorite",
                tint = Color(0xFFEAB308)
            )
        }
        IconButton(onClick = onMove, modifier = Modifier.testTag("selection_move_btn")) {
            Icon(Icons.Default.DriveFileMove, contentDescription = if (isArabic) "نقل" else "Move")
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("selection_delete_btn")) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = if (isArabic) "حذف" else "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
