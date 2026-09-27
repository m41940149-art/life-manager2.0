package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppTab
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.data.PersonalDataScreen
import com.example.ui.data.PersonalDataViewModel
import com.example.ui.habits.HabitsScreen
import com.example.ui.habits.HabitsViewModel
import com.example.ui.surveys.SurveysScreen
import com.example.ui.surveys.SurveysViewModel
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isArabic by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(AppTab.TASKS) }

    val tasksViewModel: TasksViewModel = viewModel()
    val habitsViewModel: HabitsViewModel = viewModel()
    val personalDataViewModel: PersonalDataViewModel = viewModel()
    val surveysViewModel: SurveysViewModel = viewModel()

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val screenTitle = "Life Manager"
        val subtitle = when (currentTab) {
            AppTab.TASKS -> if (isArabic) "إدارة المهام والإنتاجية اليومية" else "Daily Tasks & Productivity"
            AppTab.HABITS -> if (isArabic) "بناء العادات ومتابعة السلاسل" else "Habit Tracking & Streaks"
            AppTab.DATA -> if (isArabic) "حفظ البيانات والملاحظات الآمنة" else "Personal Data & Protected Notes"
            AppTab.SURVEYS -> if (isArabic) "الاستبيانات الدورية والتقييم الذاتي" else "Periodic Surveys & Self-Review"
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    title = screenTitle,
                    subtitle = subtitle,
                    isArabic = isArabic,
                    isDarkTheme = isDarkTheme,
                    onToggleLanguage = { isArabic = !isArabic },
                    onToggleTheme = onToggleTheme
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isArabic = isArabic
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentTab,
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppTab.TASKS -> {
                            TasksScreen(
                                viewModel = tasksViewModel,
                                isArabic = isArabic
                            )
                        }
                        AppTab.HABITS -> {
                            HabitsScreen(
                                viewModel = habitsViewModel,
                                isArabic = isArabic
                            )
                        }
                        AppTab.DATA -> {
                            PersonalDataScreen(
                                viewModel = personalDataViewModel,
                                isArabic = isArabic
                            )
                        }
                        AppTab.SURVEYS -> {
                            SurveysScreen(
                                viewModel = surveysViewModel,
                                isArabic = isArabic
                            )
                        }
                    }
                }
            }
        }
    }
}
