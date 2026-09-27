package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppTab(
    val titleAr: String,
    val titleEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    TASKS("المهام", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "tab_tasks"),
    HABITS("العادات", "Habits", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "tab_habits"),
    DATA("البيانات", "Data", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_data"),
    SURVEYS("الاستبيانات", "Surveys", Icons.Filled.Assignment, Icons.Outlined.Assignment, "tab_surveys")
}

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        AppTab.values().forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = if (isArabic) tab.titleAr else tab.titleEn,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = if (isArabic) tab.titleAr else tab.titleEn,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(tab.tag)
            )
        }
    }
}
