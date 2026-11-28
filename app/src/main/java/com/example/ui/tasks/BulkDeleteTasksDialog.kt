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

/**
 * Confirmation before deleting every ticked task. The delete is permanent:
 * there is no undo, so the count and the warning are stated plainly.
 */
@Composable
fun BulkDeleteTasksDialog(
    count: Int,
    includesRecurring: Boolean,
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
                text = if (isArabic) "حذف المهام المحددة" else "Delete Selected Tasks",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isArabic) {
                    "سيتم حذف $count من المهام نهائياً. لن يمكنك التراجع عن هذا الإجراء." +
                        if (includesRecurring) "\nسيتوقف تكرار المهام المتكررة بينها." else ""
                } else {
                    "This permanently deletes $count selected tasks. This action cannot be undone." +
                        if (includesRecurring) "\nRepetition stops for the recurring tasks among them." else ""
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
                modifier = Modifier.testTag("confirm_bulk_delete_btn")
            ) {
                Text(if (isArabic) "حذف نهائي" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_bulk_delete_btn")
            ) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("bulk_delete_dialog")
    )
}
