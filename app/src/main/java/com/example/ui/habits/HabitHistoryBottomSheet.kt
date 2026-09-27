package com.example.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Habit
import com.example.model.HabitCompletion
import com.example.util.DateTimeUtils
import com.example.util.HabitStreakCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitHistoryBottomSheet(
    habit: Habit,
    completions: List<HabitCompletion>,
    isArabic: Boolean,
    onForgotToRecord: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current displayed calendar month offset (0 = current month, -1 = last month, etc.)
    var monthOffset by remember { mutableStateOf(0) }

    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }

    val completionTimestamps = remember(completions) {
        completions.map { it.dateTimestamp }.toSet()
    }

    val habitColor = Color(habit.colorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (habit.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Statistics Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (isArabic) "السلسلة الحالية" else "Current Streak",
                    value = "${habit.currentStreak}",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = if (isArabic) "أطول سلسلة" else "Longest Streak",
                    value = "${habit.longestStreak}",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = Color(0xFFD4AF37),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = if (isArabic) "إجمالي الإنجاز" else "Total Done",
                    value = "${habit.totalCompletions}",
                    icon = Icons.Default.TaskAlt,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Calendar History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "سجل الإنجاز الفعلي" else "Actual Completion History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Month Navigator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { monthOffset-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val monthLocale = if (isArabic) Locale("ar") else Locale.ENGLISH
                    val monthFormat = SimpleDateFormat("MMMM yyyy", monthLocale)
                    Text(
                        text = monthFormat.format(displayCal.time),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = { if (monthOffset < 0) monthOffset++ },
                        enabled = monthOffset < 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            modifier = Modifier.size(18.dp),
                            tint = if (monthOffset < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Grid Container
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Weekday headers
                    val weekdayLabels = if (isArabic) {
                        listOf("ح", "ن", "ث", "ر", "خ", "ج", "س") // أحد إلى سبت
                    } else {
                        listOf("S", "M", "T", "W", "T", "F", "S")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdayLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compute Calendar days in month
                    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = (displayCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday

                    // 6 rows x 7 columns
                    var dayCounter = 1
                    for (week in 0..5) {
                        if (dayCounter > daysInMonth) break

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 1..7) {
                                if (week == 0 && col < firstDayOfWeek || dayCounter > daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val cellCal = (displayCal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayCounter)
                                    }
                                    val cellMillis = DateTimeUtils.getStartOfDay(cellCal.timeInMillis)
                                    val isCompleted = completionTimestamps.contains(cellMillis)
                                    val isScheduled = HabitStreakCalculator.isDayScheduled(
                                        cellMillis,
                                        habit.isEveryDay,
                                        habit.daysOfWeek
                                    )
                                    val isToday = DateTimeUtils.isToday(cellMillis)

                                    CalendarDayCell(
                                        dayNumber = dayCounter,
                                        isCompleted = isCompleted,
                                        isScheduled = isScheduled,
                                        isToday = isToday,
                                        habitColor = habitColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    dayCounter++
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action: "I completed it but forgot to record it"
            OutlinedButton(
                onClick = onForgotToRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("forgot_record_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "أنجزتها لكن نسيت تسجيلها" else "I completed it but forgot to record it",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isCompleted: Boolean,
    isScheduled: Boolean,
    isToday: Boolean,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isCompleted -> habitColor.copy(alpha = 0.2f)
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    isScheduled -> MaterialTheme.colorScheme.surface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 1.5.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCompleted -> habitColor
                    isScheduled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }
            )
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = habitColor,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
