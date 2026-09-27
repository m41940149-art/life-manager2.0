package com.example.ui.data

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddFolderDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAddFolder: (name: String, description: String, colorHex: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF006C5F) }
    var nameError by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFF006C5F,
        0xFF0288D1,
        0xFF7B1FA2,
        0xFFE65100,
        0xFFB58400,
        0xFF37474F
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "إنشاء مجلد جديد" else "Create New Folder",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = { Text(if (isArabic) "اسم المجلد *" else "Folder Name *") },
                    placeholder = { Text(if (isArabic) "مثال: ملاحظات العمل" else "e.g., Work Notes") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(if (isArabic) "اسم المجلد مطلوب" else "Folder name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isArabic) "الوصف (اختياري)" else "Description (optional)") },
                    placeholder = { Text(if (isArabic) "وصف محتويات المجلد..." else "Brief description...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "لون المجلد" else "Folder Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onAddFolder(name, description, selectedColor)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("submit_create_folder_btn")
            ) {
                Text(if (isArabic) "إنشاء المجلد" else "Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
