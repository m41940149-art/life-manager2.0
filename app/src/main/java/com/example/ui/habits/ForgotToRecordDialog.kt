package com.example.ui.habits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Habit
import com.example.util.DateTimeUtils

@Composable
fun ForgotToRecordDialog(
    habit: Habit,
    missedDateTimestamp: Long,
    isArabic: Boolean,
    onContinueStreak: () -> Unit,
    onBreakStreak: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedMissedDate = DateTimeUtils.formatTaskDate(missedDateTimestamp, isArabic)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (isArabic) "أنجزتها لكن نسيت تسجيلها؟" else "Completed but forgot to record?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isArabic) {
                        "يبدو أنك لم تسجل إنجاز عادة \"${habit.name}\" في الموعد المجدول الأخير ($formattedMissedDate)."
                    } else {
                        "It looks like you did not record \"${habit.name}\" on the last scheduled occurrence ($formattedMissedDate)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) {
                            "اختر \"متابعة السلسلة\" إذا كنت قد أتممت العادة بالفعل وتريد تسجيلها، أو \"قطع السلسلة\" للحفاظ على البيانات الفعلية المسجلة."
                        } else {
                            "Choose \"Continue the streak\" if you actually completed it and want to record it, or \"Break the streak\" to keep recorded history exact."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinueStreak,
                modifier = Modifier.testTag("continue_streak_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isArabic) "متابعة السلسلة" else "Continue the streak")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBreakStreak,
                modifier = Modifier.testTag("break_streak_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isArabic) "قطع السلسلة" else "Break the streak")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
