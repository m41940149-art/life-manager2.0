package com.example.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NewGoalTask
import com.example.util.GoalProgress

/** Top header used by all Goals sub-screens. */
@Composable
fun GoalsHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            actions()
        }
    }
}

/** Small selectable pill built from stable foundation APIs only. */
@Composable
fun GoalChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** A task being typed by the user before it is saved. */
data class DraftGoalTask(
    val key: Int,
    val title: String = "",
    val days: Set<Int> = emptySet()
)

fun List<DraftGoalTask>.toNewTasks(): List<NewGoalTask> =
    filter { it.title.isNotBlank() && it.days.isNotEmpty() }
        .map { NewGoalTask(title = it.title.trim(), days = it.days) }

/**
 * Editor for the goals of one week: a title and the days (offsets 0..6 from [weekStart]) for each goal.
 * Must be placed inside a scrollable parent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekTasksEditor(
    drafts: SnapshotStateList<DraftGoalTask>,
    weekStart: Long,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        drafts.forEachIndexed { index, draft ->
            androidx.compose.runtime.key(draft.key) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = { drafts[index] = draft.copy(title = it) },
                                label = { Text(if (isArabic) "الهدف ${index + 1}" else "Goal ${index + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (drafts.size > 1) {
                                IconButton(onClick = { drafts.removeAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = if (isArabic) "حذف" else "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "أيام الهدف" else "Days",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            val allSelected = draft.days.size == 7
                            Text(
                                text = if (allSelected) {
                                    if (isArabic) "إلغاء الكل" else "Clear all"
                                } else {
                                    if (isArabic) "كل الأيام" else "All days"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        drafts[index] = draft.copy(
                                            days = if (allSelected) emptySet() else (0..6).toSet()
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (offset in 0..6) {
                                val label = GoalProgress.weekdayName(
                                    GoalProgress.calendarDayOfWeek(weekStart, offset),
                                    isArabic
                                )
                                GoalChip(
                                    text = label,
                                    selected = offset in draft.days,
                                    onClick = {
                                        val newDays = if (offset in draft.days) draft.days - offset else draft.days + offset
                                        drafts[index] = draft.copy(days = newDays)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val nextKey = (drafts.maxOfOrNull { it.key } ?: 0) + 1
                drafts.add(DraftGoalTask(key = nextKey))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isArabic) "إضافة هدف آخر" else "Add another goal")
        }
    }
}
