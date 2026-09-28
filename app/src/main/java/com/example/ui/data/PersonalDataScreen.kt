package com.example.ui.data

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var showAddCardSheet by remember { mutableStateOf(false) }
    var selectedCardToView by remember { mutableStateOf<TextCard?>(null) }
    var cardToEdit by remember { mutableStateOf<TextCard?>(null) }
    var cardToMove by remember { mutableStateOf<TextCard?>(null) }
    var cardToDelete by remember { mutableStateOf<TextCard?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    // Support Android Back navigation when inside a folder
    BackHandler(enabled = uiState.currentFolder != null) {
        viewModel.navigateBackToRoot()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Folder Button (only on root)
                if (uiState.currentFolder == null && !uiState.showOnlyFavorites) {
                    FloatingActionButton(
                        onClick = { showAddFolderDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_folder_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = if (isArabic) "إضافة مجلد" else "Add Folder",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Add Card FAB
                FloatingActionButton(
                    onClick = {
                        cardToEdit = null
                        showAddCardSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("add_card_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = if (isArabic) "إضافة بطاقة" else "Add Card",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Folder Breadcrumb / Header
            if (uiState.currentFolder != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBackToRoot() },
                            modifier = Modifier.testTag("folder_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(uiState.currentFolder!!.colorHex).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(uiState.currentFolder!!.colorHex),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = uiState.currentFolder!!.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "محتويات المجلد" else "Folder contents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        if (isArabic) "بحث في العناوين والمحتوى..." else "Search titles & content...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("data_search_input"),
                singleLine = true
            )

            // Filter Chips: All vs Favorites (Only on root view when not searching)
            if (uiState.currentFolder == null && uiState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !uiState.showOnlyFavorites,
                        onClick = { viewModel.setShowOnlyFavorites(false) },
                        label = { Text(if (isArabic) "الكل" else "All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = uiState.showOnlyFavorites,
                        onClick = { viewModel.setShowOnlyFavorites(true) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (uiState.showOnlyFavorites) Color(0xFFEAB308) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "المفضلة" else "Favorites")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("favorites_filter_chip")
                    )
                }
            }

            // Content Area
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Folders (only shown on root view when query is blank and not in favorites mode)
                if (uiState.currentFolder == null && uiState.searchQuery.isBlank() && !uiState.showOnlyFavorites) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "المجلدات" else "Folders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { showAddFolderDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "مجلد جديد" else "New Folder",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.folders.isNotEmpty()) {
                        items(uiState.folders, key = { it.id }) { folder ->
                            val cardCount = uiState.allCards.count { it.folderId == folder.id }
                            FolderCard(
                                folder = folder,
                                cardCount = cardCount,
                                isArabic = isArabic,
                                onClick = { viewModel.openFolder(folder.id) },
                                onRename = { folderToRename = folder },
                                onDelete = { folderToDelete = folder }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "البطاقات العامة (بدون مجلد)" else "Unfiled Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Header for Favorites view
                if (uiState.currentFolder == null && uiState.showOnlyFavorites && uiState.searchQuery.isBlank()) {
                    item {
                        Text(
                            text = if (isArabic) "البطاقات المفضلة ⭐" else "Favorite Cards ⭐",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Section: Text Cards
                if (uiState.displayedCards.isEmpty()) {
                    item {
                        val emptyTitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "لا توجد نتائج بحث" else "No matching cards"
                            uiState.showOnlyFavorites -> if (isArabic) "لا توجد بطاقات مفضلة بعد" else "No favorite cards yet"
                            uiState.currentFolder != null -> if (isArabic) "لا توجد بطاقات في هذا المجلد" else "No cards in this folder"
                            uiState.folders.isEmpty() -> if (isArabic) "لا توجد مجلدات أو بطاقات بعد" else "No folders or cards yet"
                            else -> if (isArabic) "لا توجد بطاقات عامة بعد" else "No unfiled cards yet"
                        }

                        val emptySubtitle = when {
                            uiState.searchQuery.isNotBlank() -> if (isArabic) "جرب كلمة بحث أخرى." else "Try a different search term."
                            uiState.showOnlyFavorites -> if (isArabic) "يمكنك تمييز أي بطاقة بنجمة لتظهر هنا." else "Star any card to access it quickly here."
                            uiState.currentFolder != null -> if (isArabic) "ابدأ بإضافة بطاقة نصية داخل هذا المجلد." else "Start adding text cards to this folder."
                            else -> if (isArabic) "أنشئ بطاقات نصية لحفظ الأفكار، الملاحظات الهامة، أو البيانات المحمية." else "Create text cards to save personal notes, ideas, or protected data."
                        }

                        EmptyStateView(
                            icon = Icons.Default.NoteAdd,
                            title = emptyTitle,
                            subtitle = emptySubtitle,
                            actionButtonText = if (uiState.searchQuery.isBlank()) (if (isArabic) "+ إضافة بطاقة نصية" else "+ Add Text Card") else null,
                            onActionClick = {
                                cardToEdit = null
                                showAddCardSheet = true
                            }
                        )
                    }
                } else {
                    items(uiState.displayedCards, key = { it.id }) { card ->
                        TextDataCard(
                            card = card,
                            isArabic = isArabic,
                            onClick = { selectedCardToView = card },
                            onToggleFavorite = { viewModel.toggleFavorite(card.id) },
                            onEdit = {
                                cardToEdit = card
                                showAddCardSheet = true
                            },
                            onMove = { cardToMove = card },
                            onDelete = { cardToDelete = card }
                        )
                    }
                }
            }
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

        // Sheet: Add or Edit Card
        if (showAddCardSheet) {
            AddTextCardBottomSheet(
                folders = uiState.folders,
                initialFolderId = uiState.currentFolder?.id,
                cardToEdit = cardToEdit,
                isArabic = isArabic,
                onDismiss = {
                    showAddCardSheet = false
                    cardToEdit = null
                },
                onSaveCard = { title, content, folderId, isFavorite, isPasswordProtected, passwordHash, tags ->
                    if (cardToEdit != null) {
                        viewModel.updateTextCard(
                            cardToEdit!!.copy(
                                title = title,
                                content = content,
                                folderId = folderId,
                                isFavorite = isFavorite,
                                isPasswordProtected = isPasswordProtected,
                                passwordHash = passwordHash,
                                tags = tags,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        viewModel.addTextCard(
                            title = title,
                            content = content,
                            folderId = folderId,
                            isFavorite = isFavorite,
                            isPasswordProtected = isPasswordProtected,
                            passwordHash = passwordHash,
                            tags = tags
                        )
                    }
                }
            )
        }

        // Dialog: View Card Details
        selectedCardToView?.let { card ->
            val folder = uiState.folders.find { it.id == card.folderId }
            ViewTextCardDialog(
                card = card,
                folder = folder,
                isArabic = isArabic,
                onDismiss = { selectedCardToView = null }
            )
        }
    }
}
