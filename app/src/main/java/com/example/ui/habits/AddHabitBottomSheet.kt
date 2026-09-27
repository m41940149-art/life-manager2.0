package com.example.ui.habits

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitBottomSheet(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAddHabit: (
        name: String,
        description: String,
        isEveryDay: Boolean,
        daysOfWeek: Set<Int>,
        notificationEnabled: Boolean,
        notificationTime: String?,
        categoryIcon: String,
        colorHex: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isEveryDay by remember { mutableStateOf(true) }

    // Calendar.SUNDAY(1) to Calendar.SATURDAY(7)
    var selectedDays by remember {
        mutableStateOf(setOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY))
    }

    var notificationEnabled by remember { mutableStateOf(false) }
    var notificationTime by remember { mutableStateOf("08:00 ص") }
    var selectedColor by remember { mutableStateOf(0xFF006C5F) }
    var nameError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationEnabled = isGranted
    }

    val presetColors = listOf(
        0xFF006C5F,
        0xFF0288D1,
        0xFF7B1FA2,
        0xFFE65100,
        0xFFB58400,
        0xFF43A047
    )

    // Days mapping: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
    val daysInfo = if (isArabic) {
        listOf(
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الإثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة",
            Calendar.SATURDAY to "السبت"
        )
    } else {
        listOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "إضافة عادة جديدة" else "Create New Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text(if (isArabic) "اسم العادة *" else "Habit Name *") },
                placeholder = { Text(if (isArabic) "مثال: القراءة اليومية" else "e.g., Daily Reading") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(if (isArabic) "اسم العادة مطلوب" else "Habit name is required") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Habit Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "الوصف والهدف (اختياري)" else "Goal / Motivation (optional)") },
                placeholder = { Text(if (isArabic) "لماذا تريد الالتزام بهذه العادة؟" else "Why do you want to build this habit?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_desc_input"),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Repeat Schedule
            Text(
                text = if (isArabic) "جدول التكرار" else "Repeat Schedule",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = isEveryDay,
                    onClick = { isEveryDay = true },
                    label = { Text(if (isArabic) "كل يوم" else "Every Day") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = !isEveryDay,
                    onClick = { isEveryDay = false },
                    label = { Text(if (isArabic) "أيام محددة" else "Selected Days") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Days Selection (visible only when !isEveryDay)
            AnimatedVisibility(visible = !isEveryDay) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = if (isArabic) "اختر الأيام المجدولة:" else "Select Scheduled Days:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysInfo.forEach { (dayInt, label) ->
                            val isSelected = selectedDays.contains(dayInt)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays - dayInt else selectedDays
                                        } else {
                                            selectedDays + dayInt
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label.take(2),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Notifications Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = if (isArabic) "تفعيل التنبيهات" else "Enable Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "تذكيرك في الأوقات المحددة" else "Get reminded at chosen time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = notificationEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationEnabled = true
                                }
                            } else {
                                notificationEnabled = true
                            }
                        } else {
                            notificationEnabled = false
                        }
                    },
                    modifier = Modifier.testTag("notification_switch")
                )
            }

            // Notification Time (only if enabled)
            AnimatedVisibility(visible = notificationEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = notificationTime,
                        onValueChange = { notificationTime = it },
                        label = { Text(if (isArabic) "وقت التذكير" else "Reminder Time") },
                        placeholder = { Text(if (isArabic) "مثال: 08:00 ص أو 20:00" else "e.g., 08:00 AM") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick time suggestion chips
                    val timePresets = if (isArabic) {
                        listOf("07:00 ص", "08:30 ص", "01:00 م", "08:00 م", "10:30 م")
                    } else {
                        listOf("07:00 AM", "08:30 AM", "01:00 PM", "08:00 PM", "10:30 PM")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timePresets.forEach { timeStr ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (notificationTime == timeStr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { notificationTime = timeStr }
                            ) {
                                Text(
                                    text = timeStr,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = if (notificationTime == timeStr) FontWeight.Bold else FontWeight.Normal,
                                    color = if (notificationTime == timeStr) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Color Selector
            Text(
                text = if (isArabic) "لون العادة" else "Habit Color",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                presetColors.forEach { col ->
                    val color = Color(col)
                    val isSelected = selectedColor == col
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = col }
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        val effectiveDays = if (isEveryDay) {
                            setOf(1, 2, 3, 4, 5, 6, 7)
                        } else {
                            selectedDays
                        }
                        onAddHabit(
                            name,
                            description,
                            isEveryDay,
                            effectiveDays,
                            notificationEnabled,
                            notificationTime.takeIf { notificationEnabled },
                            "check_circle",
                            selectedColor
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_add_habit_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "إنشاء العادة" else "Create Habit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
