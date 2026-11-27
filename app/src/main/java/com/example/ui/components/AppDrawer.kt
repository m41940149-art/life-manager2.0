package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Side drawer content: lists the other pages of the app.
// The current page is left out; Habits, Tasks and Files are also in the bottom bar.
@Composable
fun AppDrawerContent(
    currentTab: AppTab,
    isArabic: Boolean,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) "الصفحات" else "Pages",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        // Tasks sits in the bottom bar now, so the drawer does not repeat it.
        AppTab.values().filter { it != currentTab && it != AppTab.TASKS }.forEach { tab ->
            NavigationDrawerItem(
                selected = false,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.unselectedIcon,
                        contentDescription = if (isArabic) tab.titleAr else tab.titleEn
                    )
                },
                label = {
                    Text(
                        text = if (isArabic) tab.titleAr else tab.titleEn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_" + tab.tag)
            )
        }
    }
}
