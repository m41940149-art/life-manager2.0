package com.example.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.ui.components.BadgeChip
import com.example.util.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    task: Task,
    isArabic: Boolean,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Subtle scale & check animation on completion toggle
    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "check_scale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.65f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "card_alpha"
    )

    val checkColor by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "check_color"
    )

    val isOverdue = !task.isCompleted && DateTimeUtils.isPast(task.dueDate)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else if (isOverdue) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkmark Circle Toggle with restrained animation
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleCompletion)
                    .testTag("toggle_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = checkColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else if (isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    if (task.isCompleted) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (isArabic) "مكتملة (انقر لإلغاء الإكمال)" else "Completed (tap to uncomplete)",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (task.isCompleted) 0.5f else 0.85f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges (Date, Time, Priority, Category)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Real Date Badge
                    val dateFormatted = DateTimeUtils.formatTaskDate(task.dueDate, isArabic)
                    val dateContainerColor = when {
                        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                        isOverdue -> MaterialTheme.colorScheme.errorContainer
                        DateTimeUtils.isToday(task.dueDate) -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    }
                    val dateContentColor = when {
                        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                        DateTimeUtils.isToday(task.dueDate) -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }

                    BadgeChip(
                        icon = Icons.Default.CalendarToday,
                        text = dateFormatted,
                        containerColor = dateContainerColor,
                        contentColor = dateContentColor
                    )

                    // Real Time Badge
                    if (!task.dueTime.isNullOrBlank()) {
                        BadgeChip(
                            icon = Icons.Default.AccessTime,
                            text = task.dueTime,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Priority Badge
                    val priorityColor = when (task.priority) {
                        TaskPriority.HIGH -> Color(0xFFEF4444)
                        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                        TaskPriority.LOW -> Color(0xFF10B981)
                    }
                    val priorityText = if (isArabic) task.priority.titleAr else task.priority.titleEn

                    BadgeChip(
                        text = priorityText,
                        containerColor = priorityColor.copy(alpha = 0.15f),
                        contentColor = priorityColor
                    )

                    // Category Badge
                    if (task.category.isNotBlank()) {
                        BadgeChip(
                            text = task.category,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Options menu (Edit / Delete)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("task_menu_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "تعديل المهمة" else "Edit Task") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        modifier = Modifier.testTag("menu_edit_${task.id}")
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isArabic) "حذف المهمة" else "Delete Task",
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
                            onDelete()
                        },
                        modifier = Modifier.testTag("menu_delete_${task.id}")
                    )
                }
            }
        }
    }
}
