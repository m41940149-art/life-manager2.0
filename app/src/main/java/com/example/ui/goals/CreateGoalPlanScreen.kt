package com.example.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.NewGoalTask
import com.example.util.DateTimeUtils
import com.example.util.GoalProgress
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGoalPlanScreen(
    isArabic: Boolean,
    onBack: () -> Unit,
    onCreate: (title: String, totalWeeks: Int, startDate: Long, tasks: List<NewGoalTask>) -> Unit
) {
    BackHandler(onBack = onBack)

    var title by remember { mutableStateOf("") }
    var weeksInput by remember { mutableStateOf("8") }
    var startOffset by remember { mutableIntStateOf(0) }
    val drafts = remember { mutableStateListOf(DraftGoalTask(key = 1)) }
    var showErrors by remember { mutableStateOf(false) }

    val weeks = weeksInput.toIntOrNull()
    val weeksValid = weeks != null && weeks in 1..52
    val titleValid = title.isNotBlank()
    val validTasks = drafts.toNewTasks()
    val tasksValid = validTasks.isNotEmpty()

    val startDate = DateTimeUtils.getRelativeDayStart(startOffset)

    Column(modifier = Modifier.fillMaxSize()) {
        GoalsHeader(
            title = if (isArabic) "خطة أهداف جديدة" else "New Goals Plan",
            subtitle = if (isArabic) "حدد المدة وابدأ بأهداف الأسبوع الأول" else "Set the duration and week 1 goals",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Goal title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isArabic) "عنوان الهدف *" else "Goal title *") },
                placeholder = { Text(if (isArabic) "مثال: لياقة بدنية" else "e.g. Get fit") },
                singleLine = true,
                isError = showErrors && !titleValid,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Number of weeks
            SectionLabel(if (isArabic) "عدد الأسابيع" else "Number of weeks")
            OutlinedTextField(
                value = weeksInput,
                onValueChange = { weeksInput = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(if (isArabic) "الأسابيع (1 - 52)" else "Weeks (1 - 52)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showErrors && !weeksValid,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(4, 8, 12, 24).forEach { n ->
                    GoalChip(
                        text = if (isArabic) "$n أسابيع" else "$n weeks",
                        selected = weeks == n,
                        onClick = { weeksInput = n.toString() }
                    )
                }
            }

            // 3. Start day (today or one of the next 6 days)
            SectionLabel(if (isArabic) "اليوم الذي تبدأ منه" else "Start day")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (offset in 0..6) {
                    val date = DateTimeUtils.getRelativeDayStart(offset)
                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    val dayName = GoalProgress.weekdayName(cal.get(Calendar.DAY_OF_WEEK), isArabic)
                    val label = when (offset) {
                        0 -> if (isArabic) "اليوم" else "Today"
                        1 -> if (isArabic) "غداً" else "Tomorrow"
                        else -> dayName
                    }
                    GoalChip(
                        text = "$label · ${formatDayMonth(date, isArabic)}",
                        selected = startOffset == offset,
                        onClick = { startOffset = offset }
                    )
                }
            }

            // 4. First week's goals
            SectionLabel(if (isArabic) "أهداف الأسبوع الأول" else "Week 1 goals")
            Text(
                text = if (isArabic) {
                    "اكتب أهدافك واختر الأيام التي ستنفذها فيها. أهداف الأسابيع التالية تختارها عند فتح كل أسبوع."
                } else {
                    "Write your goals and pick the days for each. You'll choose the next weeks' goals when each week opens."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WeekTasksEditor(
                drafts = drafts,
                weekStart = startDate,
                isArabic = isArabic
            )

            if (showErrors && !tasksValid) {
                Text(
                    text = if (isArabic) "أضف هدفاً واحداً على الأقل بعنوان ويوم واحد على الأقل" else "Add at least one goal with a title and a day",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (titleValid && weeksValid && tasksValid) {
                        onCreate(title.trim(), weeks!!, startDate, validTasks)
                    } else {
                        showErrors = true
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isArabic) "ابدأ الخطة" else "Start plan",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp)
    )
}
