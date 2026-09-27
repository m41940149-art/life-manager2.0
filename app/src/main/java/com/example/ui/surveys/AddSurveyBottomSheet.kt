package com.example.ui.surveys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuestionType
import com.example.model.SurveyFrequency
import com.example.model.SurveyQuestion

data class EditableQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "",
    var type: QuestionType = QuestionType.RATING_5
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSurveyBottomSheet(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAddSurvey: (
        name: String,
        description: String,
        frequency: SurveyFrequency,
        questions: List<SurveyQuestion>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(SurveyFrequency.DAILY) }

    val questionsList = remember {
        mutableStateListOf(
            EditableQuestion(
                text = if (isArabic) "ما هو تقييمك لمستوى الإنجاز اليوم؟" else "How do you rate today's accomplishment?",
                type = QuestionType.RATING_5
            )
        )
    }

    var nameError by remember { mutableStateOf(false) }
    var questionError by remember { mutableStateOf(false) }

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
                    text = if (isArabic) "إنشاء استبيان مخصص" else "Create Custom Survey",
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

            // Survey Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text(if (isArabic) "اسم الاستبيان *" else "Survey Name *") },
                placeholder = { Text(if (isArabic) "مثال: مراجعة العادات الأسبوعية" else "e.g., Weekly Habit Review") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(if (isArabic) "اسم الاستبيان مطلوب" else "Survey name is required") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("survey_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Survey Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "الوصف والهدف (اختياري)" else "Goal & Description (optional)") },
                placeholder = { Text(if (isArabic) "ما الغرض من هذا الاستبيان؟" else "What is the purpose of this survey?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Selector
            Text(
                text = if (isArabic) "دورية الاستبيان (التكرار)" else "Survey Frequency",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SurveyFrequency.values().forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(if (isArabic) freq.titleAr else freq.titleEn) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Questions Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "الأسئلة (${questionsList.size})" else "Questions (${questionsList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = {
                        questionsList.add(
                            EditableQuestion(
                                text = "",
                                type = QuestionType.RATING_5
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_question_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إضافة سؤال" else "Add Question", fontSize = 12.sp)
                }
            }

            if (questionError) {
                Text(
                    text = if (isArabic) "يرجى كتابة نص سؤال واحد على الأقل" else "Please enter text for at least one question",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Questions List
            questionsList.forEachIndexed { index, q ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "سؤال ${index + 1}" else "Question ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (questionsList.size > 1) {
                                IconButton(
                                    onClick = { questionsList.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete question",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = q.text,
                            onValueChange = {
                                q.text = it
                                if (questionError && it.isNotBlank()) questionError = false
                            },
                            label = { Text(if (isArabic) "نص السؤال *" else "Question text *") },
                            placeholder = { Text(if (isArabic) "اكتب السؤال هنا..." else "Enter your question...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isArabic) "نوع الإجابة:" else "Answer Type:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuestionType.values().forEach { qType ->
                                FilterChip(
                                    selected = q.type == qType,
                                    onClick = { q.type = qType },
                                    label = {
                                        Text(
                                            if (isArabic) qType.titleAr else qType.titleEn,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    val validQuestions = questionsList.filter { it.text.isNotBlank() }
                    when {
                        name.isBlank() -> nameError = true
                        validQuestions.isEmpty() -> questionError = true
                        else -> {
                            val modelQuestions = validQuestions.map {
                                SurveyQuestion(
                                    id = it.id,
                                    text = it.text.trim(),
                                    type = it.type
                                )
                            }
                            onAddSurvey(name, description, frequency, modelQuestions)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_add_survey_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "حفظ الاستبيان" else "Save Survey",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
