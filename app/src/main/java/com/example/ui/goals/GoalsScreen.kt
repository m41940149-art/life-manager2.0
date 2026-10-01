package com.example.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GoalPlan
import com.example.model.WeekStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldSuccess

/** Entry point of the Goals tab. Owns the small internal navigation (list → plan → week → add goals). */
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val route = uiState.route

    BackHandler(enabled = route !is GoalsRoute.Plans) { viewModel.back() }

    val planId = when (route) {
        is GoalsRoute.Plan -> route.planId
        is GoalsRoute.Week -> route.planId
        is GoalsRoute.AddTasks -> route.planId
        else -> null
    }
    val plan = planId?.let { id -> uiState.plans.firstOrNull { it.id == id } }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            route is GoalsRoute.Create -> {
                CreateGoalPlanScreen(
                    isArabic = isArabic,
                    onBack = { viewModel.back() },
                    onCreate = { title, weeks, startDate, tasks ->
                        viewModel.createPlan(title, weeks, startDate, tasks)
                    }
                )
            }

            route is GoalsRoute.Plan && plan != null -> {
                GoalPlanScreen(
                    plan = plan,
                    isArabic = isArabic,
                    onBack = { viewModel.back() },
                    onOpenWeek = { week ->
                        when {
                            // A week that just opened and has no goals yet goes straight to goal picking
                            week.status == WeekStatus.ACTIVE && !week.hasTasks ->
                                viewModel.navigate(GoalsRoute.AddTasks(plan.id, week.number))
                            // Later weeks stay locked until the previous one ends
                            week.status == WeekStatus.LOCKED && !week.hasTasks -> Unit
                            else -> viewModel.navigate(GoalsRoute.Week(plan.id, week.number))
                        }
                    },
                    onStartNewPlan = { viewModel.navigate(GoalsRoute.Create) },
                    onDeletePlan = { viewModel.deletePlan(plan.id) }
                )
            }

            route is GoalsRoute.Week && plan != null -> {
                val week = plan.weeks.firstOrNull { it.number == route.week }
                if (week != null) {
                    GoalWeekScreen(
                        week = week,
                        isArabic = isArabic,
                        onBack = { viewModel.back() },
                        onToggle = { taskId, offset -> viewModel.toggleCheck(taskId, offset) },
                        onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
                        onAddTasks = { viewModel.navigate(GoalsRoute.AddTasks(plan.id, week.number)) }
                    )
                } else {
                    PlansList(uiState.plans, isArabic, viewModel)
                }
            }

            route is GoalsRoute.AddTasks && plan != null -> {
                val week = plan.weeks.firstOrNull { it.number == route.week }
                if (week != null) {
                    AddWeekTasksScreen(
                        week = week,
                        isArabic = isArabic,
                        onBack = { viewModel.back() },
                        onSave = { tasks -> viewModel.addTasks(plan.id, week.number, tasks) }
                    )
                } else {
                    PlansList(uiState.plans, isArabic, viewModel)
                }
            }

            else -> PlansList(uiState.plans, isArabic, viewModel)
        }
    }
}

@Composable
private fun PlansList(
    plans: List<GoalPlan>,
    isArabic: Boolean,
    viewModel: GoalsViewModel
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigate(GoalsRoute.Create) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_goal_plan_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (isArabic) "خطة أهداف جديدة" else "New goals plan",
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
            GoalsHeader(
                title = if (isArabic) "الأهداف" else "Goals",
                subtitle = if (isArabic) "خطط أسبوعية لتحقيق أهدافك" else "Weekly plans to reach your goals"
            )

            if (plans.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Flag,
                    title = if (isArabic) "لا توجد خطط أهداف بعد" else "No goals plans yet",
                    subtitle = if (isArabic) {
                        "اختر عدد الأسابيع واليوم الذي تبدأ منه، وحدد أهداف الأسبوع الأول وأيامها، ثم تابع تقدمك أسبوعاً بأسبوع."
                    } else {
                        "Pick the number of weeks and a start day, set your week 1 goals and their days, then track progress week by week."
                    },
                    actionButtonText = if (isArabic) "+ خطة جديدة" else "+ New plan",
                    onActionClick = { viewModel.navigate(GoalsRoute.Create) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            isArabic = isArabic,
                            onClick = { viewModel.navigate(GoalsRoute.Plan(plan.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: GoalPlan,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val current = plan.currentWeek
    val statusText = when {
        plan.isFinished -> if (isArabic) "منتهية" else "Finished"
        current != null -> if (isArabic) "الأسبوع ${current.number} من ${plan.totalWeeks}" else "Week ${current.number} of ${plan.totalWeeks}"
        else -> if (isArabic) "تبدأ ${formatDayMonth(plan.startDate, true)}" else "Starts ${formatDayMonth(plan.startDate, false)}"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${plan.overallPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (plan.isFinished) EmeraldSuccess else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isArabic) "$statusText · ${plan.totalWeeks} أسابيع" else "$statusText · ${plan.totalWeeks} weeks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { plan.overallPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (plan.isFinished) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
