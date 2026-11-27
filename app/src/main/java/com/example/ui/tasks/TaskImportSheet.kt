package com.example.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.model.Task
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.DateTimeUtils

/** Preview shown before anything is saved. Nothing is added until the user taps the add button. */
@Composable
private fun ImportPreview(
    result: ImportParseResult,
    isArabic: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val onDismiss = onBack
    val toAdd = result.toAdd
    run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isArabic) "معاينة الاستيراد" else "Import preview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (result.fatal != null) {
                Text(text = result.fatal, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    text = if (isArabic) "ستُضاف ${toAdd.size} مهمة" else "Will add ${toAdd.size} tasks",
                    style = MaterialTheme.typography.titleMedium
                )
                if (result.duplicateCount > 0) {
                    Text(
                        text = if (isArabic) "${result.duplicateCount} مكررة (نفس العنوان والتاريخ) ستُتخطى" else "${result.duplicateCount} duplicates (same title and date) will be skipped",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.pastCount > 0) {
                    Text(
                        text = if (isArabic) "تنبيه: ${result.pastCount} منها بتاريخ قديم وستظهر متأخرة" else "Heads up: ${result.pastCount} have past dates and will show as overdue",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (result.errors.isNotEmpty()) {
                Text(
                    text = if (isArabic) "صفوف فيها مشكلة (${result.errors.size}) ولن تُضاف:" else "Rows with problems (${result.errors.size}), not added:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    itemsIndexed(result.errors) { _, e ->
                        Text(text = e, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (toAdd.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    itemsIndexed(toAdd) { _, row ->
                        val t = row.task
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = t.title, fontWeight = FontWeight.Medium)
                            val time = if (t.dueTime.isNullOrBlank()) "" else "  ${t.dueTime}"
                            Text(
                                text = DateTimeUtils.formatShortDate(t.dueDate, isArabic) + time + "  ·  " + t.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "رجوع" else "Back")
                }
                if (toAdd.isNotEmpty()) {
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "إضافة ${toAdd.size}" else "Add ${toAdd.size}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** Import hub: two pages side by side, Copy (the prompt) and Paste (the AI's answer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskImportSheet(
    isArabic: Boolean,
    existing: List<Task>,
    onConfirm: (List<Task>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var pasted by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ImportParseResult?>(null) }
    val prompt = remember(isArabic) { TaskImportParser.buildPrompt(isArabic) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val current = result
        if (current != null) {
            ImportPreview(
                result = current,
                isArabic = isArabic,
                onConfirm = { onConfirm(current.toAdd.map { it.task }) },
                onBack = { result = null }
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(if (isArabic) "نسخ" else "Copy") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(if (isArabic) "لصق" else "Paste") })
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (tab == 0) {
                        Text(
                            text = if (isArabic) "1) انسخ البرومبت وأعطه لأي ذكاء اصطناعي مع خطتك. 2) انسخ رده وارجع لصفحة لصق." else "1) Copy the prompt and give it to any AI with your plan. 2) Copy its answer and come back to the Paste page.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())
                        )
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(prompt))
                                copied = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (copied) (if (isArabic) "تم النسخ" else "Copied")
                                else (if (isArabic) "نسخ البرومبت" else "Copy prompt")
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = pasted,
                            onValueChange = { pasted = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp),
                            label = { Text(if (isArabic) "الصق رد الذكاء الاصطناعي هنا" else "Paste the AI's answer here") }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { pasted = clipboard.getText()?.text ?: "" },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (isArabic) "لصق من الحافظة" else "Paste from clipboard") }
                            Button(
                                onClick = {
                                    result = if (pasted.isBlank()) {
                                        ImportParseResult(fatal = if (isArabic) "لا يوجد نص للصق." else "Nothing pasted.")
                                    } else TaskImportParser.parse(pasted, isArabic, existing)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (isArabic) "معاينة" else "Preview") }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
