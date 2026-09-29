package com.example.ui.data

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
    var cardToMove by remember { mutableStateOf<TextCard?>(null) }
    var cardToDelete by remember { mutableStateOf<TextCard?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    // Android Back leaves the open folder first
    BackHandler(enabled = uiState.currentFolder != null) {
        viewModel.navigateBackToRoot()
    }
    // Registered last, so it wins: Back first collapses the "+" menu
    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExpandableAddFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                isArabic = isArabic,
                onAddCard = {
                    fabExpanded = false
                    editorRequest = EditorRequest(card = null)
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
            // Keep-style rounded search pill
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        if (isArabic) "ابحث في بطاقاتك" else "Search your cards",
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
                if (uiState.displayedCards.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        val emptyTitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "لا توجد نتائج بحث" else "No matching cards"
                            uiState.showOnlyFavorites -> if (isArabic) "لا توجد بطاقات مفضلة بعد" else "No favorite cards yet"
                            uiState.currentFolder != null -> if (isArabic) "لا توجد بطاقات في هذا المجلد" else "No cards in this folder"
                            uiState.folders.isEmpty() -> if (isArabic) "لا توجد مجلدات أو بطاقات بعد" else "No folders or cards yet"
                            else -> if (isArabic) "لا توجد بطاقات عامة بعد" else "No unfiled cards yet"
                        }

                        val emptySubtitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "جرب كلمة بحث أخرى." else "Try a different search term."
                            uiState.showOnlyFavorites -> if (isArabic) "اضغط مطولاً على أي بطاقة لإضافتها إلى المفضلة." else "Long-press any card to add it to favorites."
                            uiState.currentFolder != null -> if (isArabic) "ابدأ بإضافة بطاقة نصية داخل هذا المجلد." else "Start adding text cards to this folder."
                            else -> if (isArabic) "أنشئ بطاقات نصية لحفظ الأفكار، الملاحظات الهامة، أو البيانات المحمية." else "Create text cards to save personal notes, ideas, or protected data."
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
                    items(uiState.displayedCards, key = { it.id }) { card ->
                        TextDataCard(
                            card = card,
                            isArabic = isArabic,
                            onClick = { editorRequest = EditorRequest(card = card) },
                            onToggleFavorite = { viewModel.toggleFavorite(card.id) },
                            onEdit = { editorRequest = EditorRequest(card = card) },
                            onMove = { cardToMove = card },
                            onDelete = { cardToDelete = card }
                        )
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

        // Dialog: Delete Card Confirmation
        cardToDelete?.let { card ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = {
                    Text(
                        text = if (isArabic) "حذف البطاقة" else "Delete Card",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isArabic) {
                            "هل أنت متأكد من حذف بطاقة \"${card.title}\"؟ لا يمكن التراجع عن هذا الإجراء."
                        } else {
                            "Are you sure you want to delete card \"${card.title}\"? This action cannot be undone."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTextCard(card.id)
                            cardToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_delete_card_btn")
                    ) {
                        Text(if (isArabic) "حذف" else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Dialog: Move Card to another folder
        cardToMove?.let { card ->
            MoveCardDialog(
                card = card,
                folders = uiState.folders,
                isArabic = isArabic,
                onDismiss = { cardToMove = null },
                onMove = { newFolderId ->
                    viewModel.moveCard(card.id, newFolderId)
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
                    onClose = { editorRequest = null }
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
