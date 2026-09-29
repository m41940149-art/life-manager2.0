package com.example.ui.data

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.TextCard
import com.example.ui.components.BadgeChip

private const val MAX_VISIBLE_TAGS = 3

/**
 * Google Keep style note card.
 * - Tap: open the card.
 * - Long press: quick actions (edit / move / favorite / delete).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TextDataCard(
    card: TextCard,
    isArabic: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("text_card_${card.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val hasTitle = card.title.isNotBlank()

            if (hasTitle || card.isFavorite) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (card.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = if (isArabic) "مفضلة" else "Favorite",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            if (card.isPasswordProtected) {
                if (hasTitle) Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "محتوى محمي" else "Protected content",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else if (card.content.isNotBlank()) {
                if (hasTitle) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = card.content.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (card.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    card.tags.take(MAX_VISIBLE_TAGS).forEach { tag ->
                        BadgeChip(
                            text = "#$tag",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val extra = card.tags.size - MAX_VISIBLE_TAGS
                    if (extra > 0) {
                        BadgeChip(
                            text = "+$extra",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isArabic) "تعديل البطاقة" else "Edit card") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isArabic) "نقل إلى مجلد" else "Move to folder") },
                leadingIcon = {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    menuExpanded = false
                    onMove()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (card.isFavorite) {
                            if (isArabic) "إزالة من المفضلة" else "Remove from favorites"
                        } else {
                            if (isArabic) "إضافة إلى المفضلة" else "Add to favorites"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        if (card.isFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (isArabic) "حذف البطاقة" else "Delete card",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}
