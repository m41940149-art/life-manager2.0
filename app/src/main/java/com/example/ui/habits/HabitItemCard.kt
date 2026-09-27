package com.example.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Habit
import com.example.ui.components.BadgeChip
import com.example.util.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitItemCard(
    habit: Habit,
    isArabic: Boolean,
    onToggleCheckIn: () -> Unit,
    onViewHistory: () -> Unit,
    onForgotToRecord: () -> Unit,
    onResolveMissed: (habitId: String, dateTimestamp: Long, completed: Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val habitColor = Color(habit.colorHex)

    // Subtle restrained completion animation
    val checkScale by animateFloatAsState(
        targetValue = if (habit.isCompletedToday) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "check_scale"
    )

    val checkColor by animateColorAsState(
        targetValue = if (habit.isCompletedToday) habitColor else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "check_color"
    )

    val streakFlameColor = when {
        habit.currentStreak > 0 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habit.isCompletedToday) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (habit.isCompletedToday) 1.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .clickable(onClick = onViewHistory)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Check-in Button for current scheduled occurrence
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleCheckIn)
                        .testTag("checkin_habit_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = checkColor,
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (habit.isCompletedToday) habitColor else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        if (habit.isCompletedToday) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed Today",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name and Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (habit.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("habit_menu_${habit.id}")
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
                            text = { Text(if (isArabic) "عرض السجل والإحصائيات" else "View History & Stats") },
                            leadingIcon = {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                onViewHistory()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (isArabic) "أنجزتها لكن نسيت تسجيلها" else "Completed but forgot to record") },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                onForgotToRecord()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isArabic) "حذف العادة" else "Delete Habit",
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
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Row: Streak Badge, Schedule, Milestone
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Streak Badge (with visual flame indicator)
                val streakText = if (isArabic) {
                    "${habit.currentStreak} ${if (habit.currentStreak == 1) "يوم" else "أيام"}"
                } else {
                    "${habit.currentStreak} ${if (habit.currentStreak == 1) "day" else "days"}"
                }

                BadgeChip(
                    icon = Icons.Default.LocalFireDepartment,
                    text = streakText,
                    containerColor = streakFlameColor.copy(alpha = if (habit.currentStreak > 0) 0.15f else 0.08f),
                    contentColor = streakFlameColor
                )

                // Schedule Badge
                val scheduleText = if (habit.isEveryDay) {
                    if (isArabic) "كل يوم" else "Daily"
                } else {
                    val count = habit.daysOfWeek.size
                    if (isArabic) "$count أيام/أسبوع" else "$count days/wk"
                }

                BadgeChip(
                    icon = Icons.Default.Schedule,
                    text = scheduleText,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )

                // Today Status Badge
                if (habit.isCompletedToday) {
                    BadgeChip(
                        icon = Icons.Default.Check,
                        text = if (isArabic) "مكتملة لليوم" else "Completed Today",
                        containerColor = habitColor.copy(alpha = 0.15f),
                        contentColor = habitColor
                    )
                } else if (!habit.isScheduledToday) {
                    BadgeChip(
                        text = if (isArabic) "غير مجدولة اليوم" else "Not scheduled today",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Milestone Badge (if streak >= 7)
                if (habit.currentStreak >= 7) {
                    val milestoneText = when {
                        habit.currentStreak >= 30 -> if (isArabic) "إنجاز شهر متواصل! 🌟" else "30-Day Milestone! 🌟"
                        else -> if (isArabic) "إنجاز أسبوع متواصل! ⭐" else "7-Day Milestone! ⭐"
                    }
                    BadgeChip(
                        icon = Icons.Default.Star,
                        text = milestoneText,
                        containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f),
                        contentColor = Color(0xFFB58400)
                    )
                }

                // Notification Badge
                if (habit.notificationEnabled && !habit.notificationTime.isNullOrBlank()) {
                    BadgeChip(
                        icon = Icons.Default.Notifications,
                        text = habit.notificationTime,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Missed Habit Prompt Section (Shown if there is a pending missed occurrence)
            if (habit.pendingMissedDate != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missed_habit_prompt_${habit.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateLabel = if (DateTimeUtils.isYesterday(habit.pendingMissedDate)) {
                                if (isArabic) "فاتك أمس — ${DateTimeUtils.formatShortDate(habit.pendingMissedDate, true)}"
                                else "Missed yesterday — ${DateTimeUtils.formatShortDate(habit.pendingMissedDate, false)}"
                            } else {
                                if (isArabic) "فاتك يوم ${DateTimeUtils.formatShortDate(habit.pendingMissedDate, true)}"
                                else "Missed on ${DateTimeUtils.formatShortDate(habit.pendingMissedDate, false)}"
                            }
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (habit.pendingMissedCount > 1) {
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isArabic) "1 من ${habit.pendingMissedCount}" else "1 of ${habit.pendingMissedCount}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) "هل أنجزت هذه العادة؟" else "Did you complete it?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onResolveMissed(habit.id, habit.pendingMissedDate, true) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("yes_completed_btn_${habit.id}")
                            ) {
                                Text(
                                    text = if (isArabic) "نعم، أنجزتها" else "Yes, I completed it",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onResolveMissed(habit.id, habit.pendingMissedDate, false) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("break_streak_btn_${habit.id}")
                            ) {
                                Text(
                                    text = if (isArabic) "قطع السلسلة" else "Break the streak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
