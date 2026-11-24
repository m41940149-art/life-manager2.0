package com.example.ui.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChecklistRatingMode

private const val MAX_CUSTOM_BOXES = 10

/**
 * Shown before a new list is created: picks the list's [ChecklistRatingMode].
 * For [ChecklistRatingMode.CUSTOM] the user also picks how many boxes there are
 * (up to 10) and names each one, e.g. مقبول / جيد / ممتاز.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecklistTypeDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onCreate: (mode: ChecklistRatingMode, labels: List<String>) -> Unit
) {
    var mode by remember { mutableStateOf<ChecklistRatingMode?>(null) }
    var boxCount by remember { mutableIntStateOf(3) }
    val labels = remember { mutableStateListOf("", "", "") }

    fun setBoxCount(n: Int) {
        boxCount = n
        while (labels.size < n) labels.add("")
        while (labels.size > n) labels.removeAt(labels.lastIndex)
    }

    val canCreate = when (mode) {
        null -> false
        ChecklistRatingMode.CUSTOM -> labels.all { it.isNotBlank() }
        else -> true
    }

    val option: @Composable (ChecklistRatingMode, String, String, String) -> Unit =
        { optionMode, tag, title, subtitle ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == optionMode, onClick = { mode = optionMode })
                    .padding(vertical = 4.dp)
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = mode == optionMode, onClick = { mode = optionMode })
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isArabic) "ما نوع القائمة؟" else "What kind of list?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                option(
                    ChecklistRatingMode.NONE,
                    "checklist_type_option_none",
                    if (isArabic) "إنجاز مرة واحدة" else "One-time completion",
                    if (isArabic) "بلا تقييم" else "No rating"
                )
                option(
                    ChecklistRatingMode.SCORE,
                    "checklist_type_option_score",
                    if (isArabic) "قائمة تقييم" else "Rating list",
                    if (isArabic) "التقييم من 10" else "Rated out of 10"
                )
                option(
                    ChecklistRatingMode.CUSTOM,
                    "checklist_type_option_custom",
                    if (isArabic) "قائمة مخصصة" else "Custom list",
                    if (isArabic) "خانات تقييم تسمّيها بنفسك" else "Your own named boxes"
                )

                if (mode == ChecklistRatingMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "عدد الخانات (بحد أقصى 10)" else "Number of boxes (max 10)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (n in 1..MAX_CUSTOM_BOXES) {
                            FilterChip(
                                selected = boxCount == n,
                                onClick = { setBoxCount(n) },
                                label = { Text("$n") },
                                modifier = Modifier.testTag("checklist_type_count_$n")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    labels.forEachIndexed { index, _ ->
                        OutlinedTextField(
                            value = labels[index],
                            onValueChange = { labels[index] = it },
                            label = {
                                Text(
                                    if (isArabic) "اسم الخانة ${index + 1}" else "Box ${index + 1} name"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("checklist_type_label_$index")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = mode ?: return@Button
                    onCreate(
                        chosen,
                        if (chosen == ChecklistRatingMode.CUSTOM) labels.map { it.trim() } else emptyList()
                    )
                },
                enabled = canCreate,
                modifier = Modifier.testTag("checklist_type_create_btn")
            ) {
                Text(if (isArabic) "إنشاء" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
