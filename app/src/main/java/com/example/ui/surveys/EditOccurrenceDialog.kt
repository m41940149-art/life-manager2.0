package com.example.ui.surveys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Survey
import com.example.model.SurveyResponse

@Composable
fun EditOccurrenceDialog(
    survey: Survey,
    response: SurveyResponse,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSaveEdit: (updatedAnswers: Map<String, String>) -> Unit
) {
    val editedAnswers = remember {
        mutableStateMapOf<String, String>().apply {
            survey.questions.forEach { q ->
                val existing = response.answers.find { it.questionId == q.id }?.answerText ?: ""
                put(q.id, existing)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تعديل ${response.displayTitle(true)}" else "Edit ${response.displayTitle(false)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isArabic) "يمكنك تعديل إجابات الأسئلة أدناه وحفظها في نفس اليوم." else "Edit the responses below and save changes to this occurrence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                survey.questions.forEachIndexed { index, question ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${question.questionText}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val ans = editedAnswers[question.id] ?: ""
                            OutlinedTextField(
                                value = ans,
                                onValueChange = { editedAnswers[question.id] = it },
                                placeholder = {
                                    Text(if (isArabic) "اكتب الإجابة المعدلة..." else "Type updated response...")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_answer_input_${question.id}"),
                                minLines = 2,
                                maxLines = 6,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveEdit(editedAnswers.toMap())
                },
                modifier = Modifier.testTag("submit_edit_occurrence_btn")
            ) {
                Text(if (isArabic) "حفظ التعديلات" else "Save Changes")
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
