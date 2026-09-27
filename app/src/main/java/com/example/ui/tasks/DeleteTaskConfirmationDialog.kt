package com.example.ui.tasks

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Task

@Composable
fun DeleteTaskConfirmationDialog(
    task: Task,
    isArabic: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (isArabic) "تأكيد حذف المهمة" else "Confirm Task Deletion",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isArabic) {
                    "هل أنت متأكد من رغبتك في حذف المهمة \"${task.title}\" نهائياً؟ لن يمكنك التراجع عن هذا الإجراء."
                } else {
                    "Are you sure you want to permanently delete \"${task.title}\"? This action cannot be undone."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_task_btn")
            ) {
                Text(if (isArabic) "حذف نهائي" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_task_btn")
            ) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
