package com.example.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GoalWeek
import com.example.model.NewGoalTask

/** Pick the goals (and their days) of a week that just opened — or add more to the current one. */
@Composable
fun AddWeekTasksScreen(
    week: GoalWeek,
    isArabic: Boolean,
    onBack: () -> Unit,
    onSave: (List<NewGoalTask>) -> Unit
) {
    BackHandler(onBack = onBack)

    val drafts = remember { mutableStateListOf(DraftGoalTask(key = 1)) }
    var showError by remember { mutableStateOf(false) }
    val validTasks = drafts.toNewTasks()

    Column(modifier = Modifier.fillMaxSize()) {
        GoalsHeader(
            title = if (isArabic) "مهام الأسبوع ${week.number}" else "Week ${week.number} goals",
            subtitle = formatWeekRange(week, isArabic),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isArabic) "اختر أهداف هذا الأسبوع والأيام التي ستنفذها فيها." else "Choose this week's goals and the days for each.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WeekTasksEditor(
                drafts = drafts,
                weekStart = week.startDate,
                isArabic = isArabic
            )

            if (showError && validTasks.isEmpty()) {
                Text(
                    text = if (isArabic) "أضف هدفاً واحداً على الأقل بعنوان ويوم واحد على الأقل" else "Add at least one goal with a title and a day",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (validTasks.isNotEmpty()) onSave(validTasks) else showError = true
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isArabic) "حفظ المهام" else "Save goals",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
