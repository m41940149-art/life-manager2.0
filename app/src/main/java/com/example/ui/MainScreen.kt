package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.ui.auth.LoginScreen
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
    var showLogin by remember { mutableStateOf(false) }

    val tasksViewModel: TasksViewModel = viewModel()
    val habitsViewModel: HabitsViewModel = viewModel()
    val personalDataViewModel: PersonalDataViewModel = viewModel()
    val surveysViewModel: SurveysViewModel = viewModel()

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BackHandler(enabled = showLogin) { showLogin = false }

        if (showLogin) {
            LoginScreen(
                isArabic = isArabic,
                onNavigateBack = { showLogin = false }
            )
            return@CompositionLocalProvider
        }

        // The app bar (language / theme / account) lives on the Home tab only.
        val isHome = currentTab == AppTab.TASKS

        Scaffold(
            topBar = {
                if (isHome) {
                    AppTopBar(
                        title = "Life Manager",
                        subtitle = if (isArabic) "إدارة المهام والإنتاجية اليومية" else "Daily Tasks & Productivity",
                        isArabic = isArabic,
                        isDarkTheme = isDarkTheme,
                        onToggleLanguage = { isArabic = !isArabic },
                        onToggleTheme = onToggleTheme,
                        onAccountClick = { showLogin = true }
                    )
                }
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
                    // Without the app bar, keep content clear of the status bar
                    .then(if (isHome) Modifier else Modifier.statusBarsPadding())
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
