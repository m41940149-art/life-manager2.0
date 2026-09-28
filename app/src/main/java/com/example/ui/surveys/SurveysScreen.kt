package com.example.ui.surveys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.model.Survey
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveysScreen(
    viewModel: SurveysViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var surveyToDelete by remember { mutableStateOf<Survey?>(null) }

    when {
        uiState.isCreatingSurvey -> {
            CreateSurveyScreen(
                isArabic = isArabic,
                onBack = { viewModel.cancelCreateSurvey() },
                onSaveSurvey = { name, frequency, questions ->
                    viewModel.createSurvey(name, frequency, questions)
                }
            )
        }

        uiState.currentSurvey != null -> {
            val currentSurvey = uiState.currentSurvey!!
            SurveyOccurrenceDetailScreen(
                survey = currentSurvey,
                responses = uiState.currentResponses,
                isArabic = isArabic,
                onBack = { viewModel.navigateBackToSurveysList() },
                onSaveOccurrence = { answers, onSaved ->
                    viewModel.saveOccurrence(currentSurvey.id, answers, onSaved)
                },
                onOpenEditResponse = { resp ->
                    viewModel.openEditResponse(resp)
                },
                onCopyResponse = { respId ->
                    viewModel.copyOccurrence(respId)
                },
                onDeleteResponse = { respId ->
                    viewModel.deleteOccurrence(respId)
                }
            )

            // Edit occurrence modal
            uiState.selectedResponseForEdit?.let { respToEdit ->
                EditOccurrenceDialog(
                    survey = currentSurvey,
                    response = respToEdit,
                    isArabic = isArabic,
                    onDismiss = { viewModel.cancelEditResponse() },
                    onSaveEdit = { updatedAnswers ->
                        viewModel.updateOccurrence(respToEdit.id, updatedAnswers)
                    }
                )
            }
        }

        else -> {
            // Main Surveys List
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.startCreateSurvey() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("add_survey_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isArabic) "إنشاء استبيان" else "Create Survey",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Header Overview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "الاستبيانات والتقييم الدوري" else "Surveys & Periodic Reviews",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isArabic) {
                                        "${uiState.surveys.size} استبيانات تم إنشاؤها"
                                    } else {
                                        "${uiState.surveys.size} custom surveys created"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isArabic) "استبياناتي" else "My Surveys",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Surveys List or Empty State
                    if (uiState.surveys.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Quiz,
                            title = if (isArabic) "لا توجد استبيانات بعد" else "No surveys created yet",
                            subtitle = if (isArabic) {
                                "صمم استبيانك الأول وحدد عدد الأسئلة والتكرار لتقييم ومتابعة أهدافك."
                            } else {
                                "Create your first survey with custom question count and frequency to track your goals."
                            },
                            actionButtonText = if (isArabic) "+ إنشاء استبيان جديد" else "+ Create New Survey",
                            onActionClick = { viewModel.startCreateSurvey() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.surveys, key = { it.id }) { survey ->
                                SurveyCard(
                                    survey = survey,
                                    isArabic = isArabic,
                                    onClick = { viewModel.openSurvey(survey.id) },
                                    onDelete = { surveyToDelete = survey }
                                )
                            }
                        }
                    }
                }

                // Delete Survey Confirmation Dialog
                surveyToDelete?.let { survey ->
                    AlertDialog(
                        onDismissRequest = { surveyToDelete = null },
                        title = {
                            Text(
                                text = if (isArabic) "حذف الاستبيان" else "Delete Survey",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = if (isArabic) {
                                    "هل أنت متأكد من حذف استبيان \"${survey.name}\" وجميع الإجابات المسجلة التابعة له نهائياً؟"
                                } else {
                                    "Are you sure you want to delete survey \"${survey.name}\" and all its recorded responses permanently?"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteSurvey(survey.id)
                                    surveyToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.testTag("confirm_delete_survey_btn")
                            ) {
                                Text(if (isArabic) "حذف" else "Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { surveyToDelete = null }) {
                                Text(if (isArabic) "إلغاء" else "Cancel")
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}
