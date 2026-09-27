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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Folder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTextCardBottomSheet(
    folders: List<Folder>,
    initialFolderId: String?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAddCard: (
        title: String,
        content: String,
        folderId: String?,
        isPasswordProtected: Boolean,
        tags: List<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf(initialFolderId) }
    var isPasswordProtected by remember { mutableStateOf(false) }
    var tagsInput by remember { mutableStateOf("") }
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
                    text = if (isArabic) "إضافة بطاقة نصية" else "Add Text Card",
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

            // Card Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text(if (isArabic) "عنوان البطاقة *" else "Card Title *") },
                placeholder = { Text(if (isArabic) "مثال: ملاحظات استراتيجية" else "e.g., Strategic Notes") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(if (isArabic) "العنوان مطلوب" else "Title is required") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_card_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(if (isArabic) "المحتوى والنص" else "Content & Text Notes") },
                placeholder = { Text(if (isArabic) "اكتب الملاحظات والبيانات هنا..." else "Enter notes or data here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_card_content_input"),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Folder Selector
            Text(
                text = if (isArabic) "المجلد التابع له" else "Assign to Folder",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Unfiled / Root"
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    label = { Text(if (isArabic) "بدون مجلد (عام)" else "Unfiled (General)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Protection Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = if (isArabic) "حماية بكلمة مرور" else "Password Protection",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "تأمين محتوى البطاقة برمز سري" else "Keep sensitive text protected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isPasswordProtected,
                    onCheckedChange = { isPasswordProtected = it },
                    modifier = Modifier.testTag("protect_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags input
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text(if (isArabic) "الوسوم (مفصولة بفاصلة)" else "Tags (comma-separated)") },
                placeholder = { Text(if (isArabic) "مثال: مالي, سري, عمل" else "e.g., finance, work") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val tags = tagsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        onAddCard(
                            title,
                            content,
                            selectedFolderId,
                            isPasswordProtected,
                            tags
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_add_text_card_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "حفظ البطاقة" else "Save Card",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
