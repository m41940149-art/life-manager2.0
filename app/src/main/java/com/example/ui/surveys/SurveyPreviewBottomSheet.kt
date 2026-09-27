package com.example.ui.surveys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuestionType
import com.example.model.Survey
import com.example.ui.components.BadgeChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyPreviewBottomSheet(
    survey: Survey,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // In-memory preview response state
    val starAnswers = remember { mutableStateMapOf<String, Int>() }
    val yesNoAnswers = remember { mutableStateMapOf<String, Boolean>() }
    val textAnswers = remember { mutableStateMapOf<String, String>() }
    val scaleAnswers = remember { mutableStateMapOf<String, Float>() }

    var isSubmitted by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = survey.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (survey.description.isNotBlank()) {
                        Text(
                            text = survey.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency and Question Count
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val freqText = if (isArabic) survey.frequency.titleAr else survey.frequency.titleEn
                BadgeChip(
                    text = freqText,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )

                val countText = if (isArabic) "${survey.questions.size} أسئلة" else "${survey.questions.size} questions"
                BadgeChip(
                    text = countText,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitted) {
                // Celebration Success view
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isArabic) "تمت الإجابة على الاستبيان بنجاح!" else "Survey Completed Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "شكرًا لك على المشاركة وتدوين التقييم." else "Thank you for completing this survey evaluation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "تم" else "Done")
                        }
                    }
                }
            } else {
                // Interactive Question Items
                survey.questions.forEachIndexed { index, question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${question.text}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            when (question.type) {
                                QuestionType.RATING_5 -> {
                                    val currentRating = starAnswers[question.id] ?: 0
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = if (star <= currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                contentDescription = "Star $star",
                                                tint = if (star <= currentRating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { starAnswers[question.id] = star }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (currentRating > 0) "$currentRating / 5" else "",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                QuestionType.YES_NO -> {
                                    val currentVal = yesNoAnswers[question.id]
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FilterChip(
                                            selected = currentVal == true,
                                            onClick = { yesNoAnswers[question.id] = true },
                                            label = { Text(if (isArabic) "نعم" else "Yes") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                        FilterChip(
                                            selected = currentVal == false,
                                            onClick = { yesNoAnswers[question.id] = false },
                                            label = { Text(if (isArabic) "لا" else "No") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                QuestionType.TEXT -> {
                                    val currentText = textAnswers[question.id] ?: ""
                                    OutlinedTextField(
                                        value = currentText,
                                        onValueChange = { textAnswers[question.id] = it },
                                        placeholder = { Text(if (isArabic) "اكتب إجابتك هنا..." else "Type your response...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                QuestionType.SCALE_10 -> {
                                    val currentScale = scaleAnswers[question.id] ?: 7f
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isArabic) "المقياس: ${currentScale.toInt()} / 10" else "Scale: ${currentScale.toInt()} / 10",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = currentScale,
                                            onValueChange = { scaleAnswers[question.id] = it },
                                            valueRange = 1f..10f,
                                            steps = 8
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = { isSubmitted = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_survey_response_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "إرسال الإجابات" else "Submit Survey",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
