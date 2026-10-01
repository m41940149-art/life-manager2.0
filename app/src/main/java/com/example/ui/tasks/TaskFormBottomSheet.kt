package com.example.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.util.DateTimeUtils

enum class QuickDateOption(val titleAr: String, val titleEn: String) {
    TODAY("اليوم", "Today"),
    TOMORROW("غداً", "Tomorrow"),
    CUSTOM("تاريخ مخصص", "Custom Date")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormBottomSheet(
    taskToEdit: Task? = null,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        dueDate: Long,
        dueTime: String?,
        priority: TaskPriority,
        category: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = taskToEdit != null

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }

    var selectedDueDate by remember {
        mutableLongStateOf(taskToEdit?.dueDate ?: DateTimeUtils.getTodayStartOfDay())
    }

    var selectedDateOption by remember {
        mutableStateOf(
            when {
                taskToEdit == null || DateTimeUtils.isToday(taskToEdit.dueDate) -> QuickDateOption.TODAY
                DateTimeUtils.isTomorrow(taskToEdit.dueDate) -> QuickDateOption.TOMORROW
                else -> QuickDateOption.CUSTOM
            }
        )
    }

    var dueTime by remember { mutableStateOf(taskToEdit?.dueTime ?: "") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(taskToEdit?.category ?: if (isArabic) "عمل" else "Work") }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

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
                    text = if (isEditing) {
                        if (isArabic) "تعديل المهمة" else "Edit Task"
                    } else {
                        if (isArabic) "إضافة مهمة جديدة" else "Add New Task"
                    },
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

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text(if (isArabic) "عنوان المهمة *" else "Task Title *") },
                placeholder = { Text(if (isArabic) "مثال: مراجعة الميزانية السنوية" else "e.g., Review annual budget") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(if (isArabic) "عنوان المهمة مطلوب" else "Title is required") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "الوصف (اختياري)" else "Description (optional)") },
                placeholder = { Text(if (isArabic) "أضف تفاصيل إضافية أو خطوات المهمة..." else "Add extra notes or sub-steps...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_desc_input"),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Date Selection with Quick Options
            Text(
                text = if (isArabic) "تاريخ الإنجاز (الموعد)" else "Due Date",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateOption.values().forEach { option ->
                    val selected = selectedDateOption == option
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedDateOption = option
                            when (option) {
                                QuickDateOption.TODAY -> {
                                    selectedDueDate = DateTimeUtils.getTodayStartOfDay()
                                }
                                QuickDateOption.TOMORROW -> {
                                    selectedDueDate = DateTimeUtils.getTomorrowStartOfDay()
                                }
                                QuickDateOption.CUSTOM -> {
                                    showDatePickerDialog = true
                                }
                            }
                        },
                        label = {
                            Text(
                                text = if (isArabic) option.titleAr else option.titleEn,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_date_${option.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Display current selected date preview
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateTimeUtils.formatTaskDate(selectedDueDate, isArabic),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isArabic) "تغيير" else "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time Selection (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "الوقت المحدد (اختياري)" else "Due Time (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dueTime.isNotBlank()) {
                    TextButton(onClick = { dueTime = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "إزالة الوقت" else "Clear time", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = dueTime,
                onValueChange = { dueTime = it },
                label = { Text(if (isArabic) "وقت التنبيه" else "Time") },
                placeholder = { Text(if (isArabic) "مثال: 09:30 ص أو 04:00 م" else "e.g., 09:30 AM") },
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Quick time chips
            val quickTimes = if (isArabic) {
                listOf("09:00 ص", "01:30 م", "05:00 م", "08:30 م")
            } else {
                listOf("09:00 AM", "01:30 PM", "05:00 PM", "08:30 PM")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickTimes.forEach { qTime ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (dueTime == qTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { dueTime = qTime }
                    ) {
                        Text(
                            text = qTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (dueTime == qTime) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (dueTime == qTime) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selector
            Text(
                text = if (isArabic) "مستوى الأولوية" else "Priority Level",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.values().forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(if (isArabic) p.titleAr else p.titleEn) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                TaskPriority.HIGH -> androidx.compose.ui.graphics.Color(0xFFEF4444).copy(alpha = 0.2f)
                                TaskPriority.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.2f)
                                TaskPriority.LOW -> androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.2f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selector
            Text(
                text = if (isArabic) "التصنيف" else "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            val categories = if (isArabic) {
                listOf("عمل", "شخصي", "صحة", "تطوير", "مالية", "عام")
            } else {
                listOf("Work", "Personal", "Health", "Growth", "Finance", "General")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSubmit(
                            title,
                            description,
                            selectedDueDate,
                            dueTime.ifBlank { null },
                            priority,
                            category
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_task_form_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isEditing) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) {
                        if (isArabic) "حفظ التعديلات" else "Save Changes"
                    } else {
                        if (isArabic) "إضافة المهمة" else "Create Task"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDueDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            selectedDueDate = DateTimeUtils.getStartOfDay(millis)
                            selectedDateOption = QuickDateOption.CUSTOM
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(if (isArabic) "تأكيد" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
