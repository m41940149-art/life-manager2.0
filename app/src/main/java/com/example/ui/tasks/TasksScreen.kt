package com.example.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.ui.components.EmptyStateView
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    // Track collapsed state for groups
    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.setTaskToEdit(null)
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isArabic) "إضافة مهمة" else "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Daily Progress Banner Card (Prominent Today's Tasks Summary)
            val progress = if (uiState.todayTotalCount > 0) {
                uiState.todayCompletedCount.toFloat() / uiState.todayTotalCount
            } else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "إنجاز مهام اليوم" else "Today's Task Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) {
                                    "${uiState.todayCompletedCount} من ${uiState.todayTotalCount} مهام مكتملة (${DateTimeUtils.formatShortDate(DateTimeUtils.getTodayStartOfDay(), true)})"
                                } else {
                                    "${uiState.todayCompletedCount} of ${uiState.todayTotalCount} completed (${DateTimeUtils.formatShortDate(DateTimeUtils.getTodayStartOfDay(), false)})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Percentage badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TaskFilter.values()) { filter ->
                    val selected = uiState.selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = if (isArabic) filter.titleAr else filter.titleEn,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Task Content: Grouped List or Filtered List
            if (uiState.displayedTasks.isEmpty()) {
                // Empty state
                val emptyTitle = when (uiState.selectedFilter) {
                    TaskFilter.TODAY -> if (isArabic) "لا توجد مهام مجدولة لليوم" else "No tasks for today"
                    TaskFilter.TOMORROW -> if (isArabic) "لا توجد مهام مجدولة لغداً" else "No tasks for tomorrow"
                    TaskFilter.UPCOMING -> if (isArabic) "لا توجد مهام قادمة" else "No upcoming tasks"
                    TaskFilter.COMPLETED -> if (isArabic) "لا توجد مهام مكتملة بعد" else "No completed tasks yet"
                    TaskFilter.ALL -> if (isArabic) "قائمتك خالية من المهام" else "Your task list is empty"
                }

                val emptySubtitle = if (isArabic) {
                    "أضف مهامك الجديدة لتنظيم يومك وزيادة إنتاجيتك بسهولة."
                } else {
                    "Add tasks to keep your schedule organized and stay on track."
                }

                EmptyStateView(
                    icon = Icons.Default.AssignmentTurnedIn,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    actionButtonText = if (isArabic) "+ إضافة مهمة جديدة" else "+ Add New Task",
                    onActionClick = {
                        viewModel.setTaskToEdit(null)
                        showAddSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.selectedFilter == TaskFilter.ALL) {
                        // Display by Groups (Overdue, Today, Tomorrow, Upcoming, Completed)
                        uiState.groupedTasks.forEach { group ->
                            val groupKey = group.titleEn
                            val isCollapsed = collapsedGroups[groupKey] ?: false

                            item(key = "header_$groupKey") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp)
                                        .clickable(enabled = group.isCollapsible) {
                                            collapsedGroups[groupKey] = !isCollapsed
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) group.titleAr else group.titleEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (group.titleEn.startsWith("Overdue")) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (group.isCollapsible) {
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            if (!isCollapsed) {
                                items(group.tasks, key = { it.id }) { task ->
                                    TaskItemCard(
                                        task = task,
                                        isArabic = isArabic,
                                        onToggleCompletion = { viewModel.toggleTask(task.id) },
                                        onEdit = {
                                            viewModel.setTaskToEdit(task)
                                            showAddSheet = true
                                        },
                                        onDelete = { viewModel.setTaskToDelete(task) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Display flat list for the selected filter
                        items(uiState.displayedTasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                isArabic = isArabic,
                                onToggleCompletion = { viewModel.toggleTask(task.id) },
                                onEdit = {
                                    viewModel.setTaskToEdit(task)
                                    showAddSheet = true
                                },
                                onDelete = { viewModel.setTaskToDelete(task) }
                            )
                        }
                    }
                }
            }
        }

        // Add / Edit Task Bottom Sheet
        if (showAddSheet) {
            TaskFormBottomSheet(
                taskToEdit = taskToEdit,
                isArabic = isArabic,
                onDismiss = {
                    showAddSheet = false
                    viewModel.setTaskToEdit(null)
                },
                onSubmit = { title, description, dueDate, dueTime, priority, category ->
                    if (taskToEdit != null) {
                        viewModel.updateTask(
                            id = taskToEdit!!.id,
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            priority = priority,
                            category = category
                        )
                    } else {
                        viewModel.addTask(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            priority = priority,
                            category = category
                        )
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        taskToDelete?.let { task ->
            DeleteTaskConfirmationDialog(
                task = task,
                isArabic = isArabic,
                onConfirm = { viewModel.confirmDeleteTask() },
                onDismiss = { viewModel.setTaskToDelete(null) }
            )
        }
    }
}
