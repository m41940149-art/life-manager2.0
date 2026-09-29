package com.example.ui.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Folder

/**
 * One horizontal row replacing: the old folder cards, the section headers
 * and the All/Favorites chips. Long list of folders scrolls sideways.
 */
@Composable
fun FolderChipsRow(
    folders: List<Folder>,
    currentFolder: Folder?,
    showOnlyFavorites: Boolean,
    isArabic: Boolean,
    onSelectGeneral: () -> Unit,
    onSelectFavorites: () -> Unit,
    onSelectFolder: (Folder) -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentFolder == null && !showOnlyFavorites,
                    onClick = onSelectGeneral,
                    label = { Text(if (isArabic) "عام" else "General") },
                    colors = chipColors
                )
            }

            item {
                FilterChip(
                    selected = showOnlyFavorites,
                    onClick = onSelectFavorites,
                    label = { Text(if (isArabic) "المفضلة" else "Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    },
                    colors = chipColors,
                    modifier = Modifier.testTag("favorites_filter_chip")
                )
            }

            items(folders, key = { it.id }) { folder ->
                FilterChip(
                    selected = currentFolder?.id == folder.id && !showOnlyFavorites,
                    onClick = { onSelectFolder(folder) },
                    label = {
                        Text(
                            text = folder.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(folder.colorHex),
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    },
                    colors = chipColors,
                    modifier = Modifier.testTag("folder_chip_${folder.id}")
                )
            }
        }

        // Folder options appear only while a folder is open
        if (currentFolder != null && !showOnlyFavorites) {
            var menuExpanded by remember(currentFolder.id) { mutableStateOf(false) }
            Box(modifier = Modifier.padding(end = 4.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("folder_menu_${currentFolder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = if (isArabic) "خيارات المجلد" else "Folder options",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "إعادة تسمية المجلد" else "Rename folder") },
                        leadingIcon = {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onRenameFolder(currentFolder)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isArabic) "حذف المجلد" else "Delete folder",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteFolder(currentFolder)
                        }
                    )
                }
            }
        }
    }
}
