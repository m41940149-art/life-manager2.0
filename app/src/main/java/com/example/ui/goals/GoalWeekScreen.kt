package com.example.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoalTask
import com.example.model.GoalWeek
import com.example.model.WeekStatus
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.util.DateTimeUtils
import com.example.util.GoalProgress

private enum class DayState { DONE, MISSED, TODAY, FUTURE }

/** Details of one week: its goals, the days of each, and what was achieved. */
@Composable
fun GoalWeekScreen(
    week: GoalWeek,
    isArabic: Boolean,
    onBack: () -> Unit,
    onToggle: (taskId: String, dayOffset: Int) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onAddTasks: () -> Unit
) {
    BackHandler(onBack = onBack)

    var taskToDelete by remember { mutableStateOf<GoalTask?>(null) }

    val today = DateTimeUtils.getTodayStartOfDay()
    val ended = week.status == WeekStatus.ENDED
    val canEdit = week.status == WeekStatus.ACTIVE || (week.status == WeekStatus.LOCKED && week.number == 1)

    Column(modifier = Modifier.fillMaxSize()) {
        GoalsHeader(
            title = if (isArabic) "الأسبوع ${week.number}" else "Week ${week.number}",
            subtitle = formatWeekRange(week, isArabic),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WeekSummaryCard(week = week, isArabic = isArabic)
            }

            if (!week.hasTasks) {
                item {
                    Text(
                        text = when {
                            ended -> if (isArabic) "لم تُضَف مهام لهذا الأسبوع." else "No goals were added for this week."
                            else -> if (isArabic) "لم تختر مهام هذا الأسبوع بعد." else "You haven't picked this week's goals yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }

            items(week.tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    week = week,
                    today = today,
                    isArabic = isArabic,
                    canDelete = canEdit,
                    onToggle = { offset -> onToggle(task.id, offset) },
                    onDelete = { taskToDelete = task }
                )
            }

            if (canEdit) {
                item {
                    Button(
                        onClick = onAddTasks,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (week.hasTasks) {
                                if (isArabic) "إضافة مهام أخرى" else "Add more goals"
                            } else {
                                if (isArabic) "اختر مهام هذا الأسبوع" else "Pick this week's goals"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(if (isArabic) "حذف الهدف؟" else "Delete goal?") },
            text = { Text(task.title) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTask(task.id)
                    taskToDelete = null
                }) {
                    Text(if (isArabic) "حذف" else "Delete", color = RoseError)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun WeekSummaryCard(week: GoalWeek, isArabic: Boolean) {
    val statusText = when (week.status) {
        WeekStatus.ENDED -> if (isArabic) "أسبوع منتهي · النسبة النهائية" else "Finished week · final result"
        WeekStatus.ACTIVE -> if (isArabic) "الأسبوع الحالي" else "Current week"
        WeekStatus.LOCKED -> if (isArabic) "لم يبدأ بعد" else "Not started yet"
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (week.status == WeekStatus.ENDED) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) "${week.doneSlots} من ${week.totalSlots} إنجاز" else "${week.doneSlots} of ${week.totalSlots} done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (week.status == WeekStatus.ENDED) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text(
                    text = "${week.percent}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { week.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: GoalTask,
    week: GoalWeek,
    today: Long,
    isArabic: Boolean,
    canDelete: Boolean,
    onToggle: (dayOffset: Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${task.doneSlots}/${task.totalSlots}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                task.days.forEach { offset ->
                    val date = GoalProgress.dayDate(week.startDate, offset)
                    val done = offset in task.doneDays
                    val state = when {
                        done -> DayState.DONE
                        week.status == WeekStatus.ENDED || date < today -> DayState.MISSED
                        date == today -> DayState.TODAY
                        else -> DayState.FUTURE
                    }
                    // Today and earlier days of the running week can be ticked; ended weeks are read-only.
                    val canTick = week.status == WeekStatus.ACTIVE && date <= today
                    val dayName = GoalProgress.weekdayName(GoalProgress.calendarDayOfWeek(week.startDate, offset), isArabic)
                    DayCell(
                        label = dayName,
                        state = state,
                        enabled = canTick,
                        onClick = { onToggle(offset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    label: String,
    state: DayState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val circleBg: Color
    val circleBorder: Color
    when (state) {
        DayState.DONE -> {
            circleBg = EmeraldSuccess
            circleBorder = EmeraldSuccess
        }
        DayState.MISSED -> {
            circleBg = RoseError.copy(alpha = 0.12f)
            circleBorder = RoseError.copy(alpha = 0.5f)
        }
        DayState.TODAY -> {
            circleBg = Color.Transparent
            circleBorder = MaterialTheme.colorScheme.primary
        }
        DayState.FUTURE -> {
            circleBg = Color.Transparent
            circleBorder = MaterialTheme.colorScheme.outlineVariant
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circleBg)
                .border(1.5.dp, circleBorder, CircleShape)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            when (state) {
                DayState.DONE -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                DayState.MISSED -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(20.dp)
                )
                else -> {}
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
