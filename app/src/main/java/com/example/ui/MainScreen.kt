package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.celebration.CelebrationHost
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppTab
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.data.PersonalDataScreen
import com.example.ui.data.PersonalDataViewModel
import com.example.ui.files.FilesScreen
import com.example.ui.goals.GoalsScreen
import com.example.ui.goals.GoalsViewModel
import com.example.ui.habits.HabitsScreen
import com.example.ui.habits.HabitsViewModel
import com.example.ui.surveys.SurveysScreen
import com.example.ui.surveys.SurveysViewModel
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.ui.timer.TimerScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isArabic by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(AppTab.HABITS) }
    var showLogin by remember { mutableStateOf(false) }

    val tasksViewModel: TasksViewModel = viewModel()
    val habitsViewModel: HabitsViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()
    val personalDataViewModel: PersonalDataViewModel = viewModel()
    val surveysViewModel: SurveysViewModel = viewModel()

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
        BackHandler(enabled = showLogin) { showLogin = false }
        BackHandler(enabled = !showLogin && currentTab != AppTab.HABITS) { currentTab = AppTab.HABITS }

        if (showLogin) {
            LoginScreen(
                isArabic = isArabic,
                onNavigateBack = { showLogin = false }
            )
            return@CompositionLocalProvider
        }

        // The app bar stays on every page so the hamburger (side drawer) is
        // always reachable; language / theme / account controls show on Home only.
        val isHome = currentTab == AppTab.HABITS

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawerContent(
                    currentTab = currentTab,
                    isArabic = isArabic,
                    onTabSelected = { tab ->
                        currentTab = tab
                        scope.launch { drawerState.close() }
                    }
                )
            }
        ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "Life Manager",
                    subtitle = if (isArabic) "إدارة المهام والإنتاجية اليومية" else "Daily Tasks & Productivity",
                    isArabic = isArabic,
                    isDarkTheme = isDarkTheme,
                    onToggleLanguage = { isArabic = !isArabic },
                    onToggleTheme = onToggleTheme,
                    onAccountClick = { showLogin = true },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    showControls = isHome
                )
            },
            bottomBar = {
                // Bottom bar stays visible on every page: Habits and Files only.
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
                        AppTab.GOALS -> {
                            GoalsScreen(
                                viewModel = goalsViewModel,
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
                        AppTab.TIMER -> {
                            TimerScreen(isArabic = isArabic)
                        }
                        AppTab.FILES -> {
                            FilesScreen(isArabic = isArabic)
                        }
                    }
                }

                // Joyful surprises overlay every tab when something is achieved.
                CelebrationHost(isArabic = isArabic)
            }
        }
        }
    }
}
