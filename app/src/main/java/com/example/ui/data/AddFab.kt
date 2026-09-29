package com.example.ui.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Single "+" button. Tapping it reveals two options: new card / new folder.
 * The "+" rotates into an "x" while expanded.
 */
@Composable
fun ExpandableAddFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    isArabic: Boolean,
    onAddCard: () -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fab_rotation")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddFolder,
                    icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                    text = { Text(if (isArabic) "مجلد" else "Folder") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("add_folder_fab")
                )
                ExtendedFloatingActionButton(
                    onClick = onAddCard,
                    icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                    text = { Text(if (isArabic) "بطاقة" else "Card") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("add_card_fab")
                )
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.testTag("add_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isArabic) "إضافة" else "Add",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}
