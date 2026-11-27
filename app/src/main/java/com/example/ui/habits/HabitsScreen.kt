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
import androidx.compose.material3.FilterChip
import com.example.model.HabitTimeGroup
import com.example.model.habitTimeGroup
import com.example.model.isHabitVisibleInPeriod
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
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
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showAllHabits by remember { mutableStateOf(false) }
    var showAllPeriods by remember { mutableStateOf(false) }
    var localHour by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = nowMillis() }.get(Calendar.HOUR_OF_DAY)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                localHour = Calendar.getInstance().apply { timeInMillis = nowMillis() }.get(Calendar.HOUR_OF_DAY)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            localHour = Calendar.getInstance().apply { timeInMillis = nowMillis() }.get(Calendar.HOUR_OF_DAY)
            // Re-evaluate at the next minute, including 16:00, using the current local timezone.
            delay(60_000L - (nowMillis() % 60_000L))
        }
    }
    val habitsForDay = if (showAllHabits) uiState.allHabits else uiState.habits
    val visibleHabits = habitsForDay.filter { isHabitVisibleInPeriod(it.scheduledTime, localHour, showAllPeriods) }


    // --- Drag & drop reordering state ---
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var orderOverride by remember { mutableStateOf<List<String>?>(null) }
    var draggingHabitId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showAllHabits, showAllPeriods, localHour) { orderOverride = null }

    // Persisted list, re-ordered by the in-progress / not-yet-persisted drag order.
    val displayedHabits: List<Habit> = run {
        val custom = orderOverride
        if (custom == null) {
            visibleHabits
        } else {
            val byId = visibleHabits.associateBy { it.id }
            visibleHabits.filter { it.id !in custom } + custom.mapNotNull { byId[it] }
        }
    }

    // Drop the temporary override once the database order has caught up with it.
    LaunchedEffect(visibleHabits) {
        if (draggingHabitId == null && orderOverride == visibleHabits.map { it.id }) {
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
                    .size(46.dp)
                    .testTag("add_habit_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isArabic) "إضافة عادة" else "Add Habit",
                    modifier = Modifier.size(22.dp)
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
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "العادات اليومية" else "Today's Habits",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            if (uiState.hasAnyHabits) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showAllHabits,
                        onClick = { showAllHabits = false },
                        label = { Text(if (isArabic) "اليوم" else "Today") }
                    )
                    FilterChip(
                        selected = showAllHabits,
                        onClick = { showAllHabits = true },
                        label = { Text(if (isArabic) "كل العادات" else "All habits") },
                        modifier = Modifier.testTag("all_habits_filter")
                    )
                }
            }
            if (uiState.hasAnyHabits) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showAllPeriods,
                        onClick = { showAllPeriods = false },
                        label = { Text(if (localHour < 16) {
                            if (isArabic) "الصباح الآن" else "Morning now"
                        } else {
                            if (isArabic) "المساء الآن" else "Evening now"
                        }) },
                        modifier = Modifier.testTag("current_period_filter")
                    )
                    FilterChip(
                        selected = showAllPeriods,
                        onClick = { showAllPeriods = true },
                        label = { Text(if (isArabic) "إظهار كل الفترات" else "Show all periods") },
                        modifier = Modifier.testTag("all_periods_filter")
                    )
                }
            }
            // Habits List or Empty State
            if (visibleHabits.isEmpty() && habitsForDay.isNotEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Loop,
                    title = if (isArabic) "لا توجد عادات لهذه الفترة" else "No habits for this period",
                    subtitle = if (isArabic) "يمكنك إظهار كل الفترات للوصول إلى العادات الأخرى." else "Show all periods to see your other habits.",
                    actionButtonText = if (isArabic) "إظهار كل الفترات" else "Show all periods",
                    onActionClick = { showAllPeriods = true },
                    modifier = Modifier.weight(1f)
                )
            } else if (visibleHabits.isEmpty() && uiState.hasAnyHabits) {
                EmptyStateView(
                    icon = Icons.Default.Loop,
                    title = if (isArabic) "لا توجد عادات لهذا اليوم" else "No habits scheduled today",
                    subtitle = if (isArabic) {
                        "عاداتك ستظهر هنا في الأيام المحددة لها فقط."
                    } else {
                        "Your habits will show up here on their scheduled days only."
                    },
                    modifier = Modifier.weight(1f)
                )
            } else if (visibleHabits.isEmpty()) {
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
                    HabitTimeGroup.entries.forEach { group ->
                        val groupHabits = displayedHabits.filter { habitTimeGroup(it.scheduledTime) == group }
                        if (groupHabits.isNotEmpty()) {
                            item(key = "header_$group") {
                                Text(
                                    text = when (group) {
                                        HabitTimeGroup.MORNING -> if (isArabic) "صباحًا · قبل ٤:٠٠ مساءً" else "Morning · before 4:00 PM"
                                        HabitTimeGroup.AFTERNOON_EVENING -> if (isArabic) "بعد الظهر / مساءً · من ٤:٠٠ مساءً" else "Afternoon / evening · from 4:00 PM"
                                        HabitTimeGroup.UNSCHEDULED -> if (isArabic) "بدون وقت · عدّل العادة لتحديده" else "No time set · edit to choose a time"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp).testTag("habit_group_$group")
                                )
                            }
                        }
                    items(groupHabits, key = { it.id }) { habit ->
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
                                            displayedHabits.any { candidate ->
                                                candidate.id == it.key && habitTimeGroup(candidate.scheduledTime) == group
                                            } &&
                                            centerY >= it.offset &&
                                            centerY <= it.offset + it.size
                                    }

                                    if (target != null) {
                                        val ids = orderOverride ?: visibleHabits.map { it.id }
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
                                    if (ordered != null && ordered != visibleHabits.map { it.id }) {
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
                            onEdit = { editingHabit = habit },
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
        }

        // Add Habit Bottom Sheet
        if (showAddSheet || editingHabit != null) {
            val initial = editingHabit
            AddHabitBottomSheet(
                isArabic = isArabic,
                initialHabit = initial,
                onDismiss = { showAddSheet = false; editingHabit = null },
                onAddHabit = { name, description, isEveryDay, daysOfWeek, notificationEnabled, notificationTime, icon, color, scheduledTime ->
                    if (initial != null) {
                        viewModel.updateHabit(
                            initial.id, name, description, isEveryDay, daysOfWeek,
                            notificationEnabled, notificationTime, icon, color, scheduledTime
                        )
                    } else viewModel.addHabit(
                        name = name,
                        description = description,
                        isEveryDay = isEveryDay,
                        daysOfWeek = daysOfWeek,
                        notificationEnabled = notificationEnabled,
                        notificationTime = notificationTime,
                        categoryIcon = icon,
                        colorHex = color,
                        scheduledTime = scheduledTime
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
