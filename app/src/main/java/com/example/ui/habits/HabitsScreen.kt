package com.example.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.example.model.Habit
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    // --- Drag & drop reordering state ---
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var orderOverride by remember { mutableStateOf<List<String>?>(null) }
    var draggingHabitId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Persisted list, re-ordered by the in-progress / not-yet-persisted drag order.
    val displayedHabits: List<Habit> = run {
        val custom = orderOverride
        if (custom == null) {
            uiState.habits
        } else {
            val byId = uiState.habits.associateBy { it.id }
            uiState.habits.filter { it.id !in custom } + custom.mapNotNull { byId[it] }
        }
    }

    // Drop the temporary override once the database order has caught up with it.
    LaunchedEffect(uiState.habits) {
        if (draggingHabitId == null && orderOverride == uiState.habits.map { it.id }) {
            orderOverride = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_habit_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isArabic) "إضافة عادة" else "Add Habit",
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
            // Header Progress Card (only shown when user has habits)
            if (uiState.habits.isNotEmpty()) {
                val scheduledTodayCount = uiState.habits.count { it.isScheduledToday }
                val progress = if (scheduledTodayCount > 0) {
                    uiState.completedTodayCount.toFloat() / scheduledTodayCount
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
                                Text(
                                    text = if (isArabic) "العادات اليومية" else "Today's Habits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isArabic) {
                                        "${uiState.completedTodayCount} من $scheduledTodayCount عادات مكتملة اليوم"
                                    } else {
                                        "${uiState.completedTodayCount} of $scheduledTodayCount completed today"
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

                        Spacer(modifier = Modifier.height(10.dp))

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
            }

            // Habits List or Empty State
            if (uiState.habits.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Loop,
                    title = if (isArabic) "لا توجد عادات مضافة بعد" else "No habits created yet",
                    subtitle = if (isArabic) {
                        "ابدأ ببناء عادات إيجابية صغيرة ومتابعة سلاسل الإنجاز اليومية."
                    } else {
                        "Start building small positive habits and track your daily streaks."
                    },
                    actionButtonText = if (isArabic) "+ إضافة عادة جديدة" else "+ Add New Habit",
                    onActionClick = { showAddSheet = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedHabits, key = { it.id }) { habit ->
                        val isDragging = draggingHabitId == habit.id

                        val dragHandleModifier = Modifier.pointerInput(habit.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingHabitId = habit.id
                                    dragOffsetY = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    val layout = listState.layoutInfo
                                    val current = layout.visibleItemsInfo
                                        .firstOrNull { it.key == habit.id }
                                        ?: return@detectDragGesturesAfterLongPress

                                    val centerY = current.offset + current.size / 2f + dragOffsetY
                                    val target = layout.visibleItemsInfo.firstOrNull {
                                        it.key != habit.id &&
                                            centerY >= it.offset &&
                                            centerY <= it.offset + it.size
                                    }

                                    if (target != null) {
                                        val ids = orderOverride ?: uiState.habits.map { it.id }
                                        val from = ids.indexOf(habit.id)
                                        val to = ids.indexOf(target.key)
                                        if (from != -1 && to != -1 && from != to) {
                                            val newOffset = if (to > from) {
                                                target.offset + target.size - current.size
                                            } else {
                                                target.offset
                                            }
                                            orderOverride = ids.toMutableList().apply {
                                                add(to, removeAt(from))
                                            }
                                            // Keep the dragged card under the finger after the swap.
                                            dragOffsetY += (current.offset - newOffset)
                                        }
                                    }

                                    // Auto-scroll near the list edges.
                                    val viewportTop = layout.viewportStartOffset
                                    val viewportBottom = layout.viewportEndOffset
                                    val edge = 120f
                                    val scrollDelta = when {
                                        centerY < viewportTop + edge -> -24f
                                        centerY > viewportBottom - edge -> 24f
                                        else -> 0f
                                    }
                                    if (scrollDelta != 0f) {
                                        scope.launch {
                                            listState.scrollBy(scrollDelta)
                                        }
                                        dragOffsetY += scrollDelta
                                    }
                                },
                                onDragEnd = {
                                    val ordered = orderOverride
                                    if (ordered != null && ordered != uiState.habits.map { it.id }) {
                                        viewModel.reorderHabits(ordered)
                                    } else {
                                        orderOverride = null
                                    }
                                    draggingHabitId = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingHabitId = null
                                    dragOffsetY = 0f
                                    orderOverride = null
                                }
                            )
                        }

                        HabitItemCard(
                            habit = habit,
                            isArabic = isArabic,
                            onToggleCheckIn = { viewModel.toggleCheckIn(habit.id) },
                            onViewHistory = { viewModel.openHistory(habit) },
                            onForgotToRecord = { viewModel.openForgotToRecord(habit) },
                            onResolveMissed = { habitId, dateTimestamp, completed ->
                                viewModel.resolveMissedOccurrence(habitId, dateTimestamp, completed)
                            },
                            onDelete = { viewModel.deleteHabit(habit.id) },
                            dragHandleModifier = dragHandleModifier,
                            modifier = if (isDragging) {
                                Modifier
                                    .zIndex(1f)
                                    .graphicsLayer {
                                        translationY = dragOffsetY
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        shadowElevation = 16f
                                    }
                            } else {
                                Modifier.animateItem()
                            }
                        )
                    }
                }
            }
        }

        // Add Habit Bottom Sheet
        if (showAddSheet) {
            AddHabitBottomSheet(
                isArabic = isArabic,
                onDismiss = { showAddSheet = false },
                onAddHabit = { name, description, isEveryDay, daysOfWeek, notificationEnabled, notificationTime, icon, color ->
                    viewModel.addHabit(
                        name = name,
                        description = description,
                        isEveryDay = isEveryDay,
                        daysOfWeek = daysOfWeek,
                        notificationEnabled = notificationEnabled,
                        notificationTime = notificationTime,
                        categoryIcon = icon,
                        colorHex = color
                    )
                }
            )
        }

        // Habit History & Statistics Sheet
        uiState.selectedHabitForHistory?.let { habit ->
            HabitHistoryBottomSheet(
                habit = habit,
                completions = uiState.historyCompletions,
                isArabic = isArabic,
                onForgotToRecord = {
                    viewModel.closeHistory()
                    viewModel.openForgotToRecord(habit)
                },
                onDismiss = { viewModel.closeHistory() }
            )
        }

        // "Forgot to record" Dialog
        if (uiState.forgotRecordHabit != null && uiState.missedOccurrenceDate != null) {
            ForgotToRecordDialog(
                habit = uiState.forgotRecordHabit!!,
                missedDateTimestamp = uiState.missedOccurrenceDate!!,
                isArabic = isArabic,
                onContinueStreak = { viewModel.continueStreak() },
                onBreakStreak = { viewModel.breakStreak() },
                onDismiss = { viewModel.closeForgotToRecord() }
            )
        }
    }
}
