package com.example.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoalPlan
import com.example.model.GoalWeek
import com.example.model.WeekStatus
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError

@Composable
fun GoalPlanScreen(
    plan: GoalPlan,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenWeek: (GoalWeek) -> Unit,
    onStartNewPlan: () -> Unit,
    onDeletePlan: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentWeek = plan.currentWeek
    val subtitle = when {
        plan.isFinished -> if (isArabic) "انتهت الخطة" else "Plan finished"
        currentWeek != null -> if (isArabic) "الأسبوع ${currentWeek.number} من ${plan.totalWeeks}" else "Week ${currentWeek.number} of ${plan.totalWeeks}"
        else -> if (isArabic) "تبدأ ${formatDayMonth(plan.startDate, true)}" else "Starts ${formatDayMonth(plan.startDate, false)}"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GoalsHeader(
            title = plan.title,
            subtitle = subtitle,
            onBack = onBack,
            actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = if (isArabic) "حذف الخطة" else "Delete plan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Goal title + overall progress, shown above the squares
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plan.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${plan.overallPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { plan.overallPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "الإنجاز الكلي · ${plan.totalWeeks} أسابيع" else "Overall progress · ${plan.totalWeeks} weeks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (plan.isFinished) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "انتهت جميع الأسابيع" else "All weeks are done",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = onStartNewPlan) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(if (isArabic) "ابدأ خطة جديدة" else "Start a new plan")
                            }
                        }
                    }
                }
            }

            items(plan.weeks, key = { it.number }) { week ->
                WeekSquare(
                    week = week,
                    isArabic = isArabic,
                    onClick = { onOpenWeek(week) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (isArabic) {
                        "يُفتح كل أسبوع بعد انتهاء الأسبوع الذي قبله، وتختار مهامه عندها. اضغط على أي أسبوع لرؤية تفاصيله."
                    } else {
                        "Each week opens when the previous one ends, and you pick its goals then. Tap any week for details."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isArabic) "حذف الخطة؟" else "Delete plan?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف \"${plan.title}\" وكل أسابيعها وإنجازاتها نهائياً."
                    else "\"${plan.title}\" and all its weeks and progress will be deleted permanently."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeletePlan()
                }) {
                    Text(if (isArabic) "حذف" else "Delete", color = RoseError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

/** One square per week: shows the week's completion %, a red X once the week has ended. */
@Composable
private fun WeekSquare(
    week: GoalWeek,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val ended = week.status == WeekStatus.ENDED
    val active = week.status == WeekStatus.ACTIVE
    val locked = week.status == WeekStatus.LOCKED

    val background = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        ended -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (active) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = if (isArabic) "الأسبوع ${week.number}" else "Week ${week.number}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            when {
                locked -> {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDayMonth(week.startDate, isArabic),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                active && !week.hasTasks -> {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isArabic) "اختر المهام" else "Pick goals",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Text(
                        text = "${week.percent}%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ended) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { week.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (ended) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        if (ended) {
            // Big red X over a finished week
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                val stroke = 5.dp.toPx()
                val color = RoseError.copy(alpha = 0.85f)
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
