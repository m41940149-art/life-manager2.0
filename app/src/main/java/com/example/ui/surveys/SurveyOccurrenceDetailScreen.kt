package com.example.ui.surveys

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.model.Survey
import com.example.model.SurveyResponse
import com.example.ui.components.BadgeChip
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurveyOccurrenceDetailScreen(
    survey: Survey,
    responses: List<SurveyResponse>,
    isArabic: Boolean,
    onBack: () -> Unit,
    onSaveOccurrence: (answers: Map<String, String>, onSaved: () -> Unit) -> Unit,
    onOpenEditResponse: (SurveyResponse) -> Unit,
    onDeleteResponse: (responseId: String) -> Unit
) {
    BackHandler(onBack = onBack)

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Copies the questions and answers of a saved occurrence as plain text
    fun copyResponseText(response: SurveyResponse) {
        val text = buildString {
            append(survey.name).append(" - ").append(response.displayTitle(isArabic))
            append("\n\n")
            survey.questions.forEachIndexed { qIndex, question ->
                val ans = response.answers.find { it.questionId == question.id }?.answerText?.trim().orEmpty()
                append("${qIndex + 1}. ${question.questionText}\n")
                append(if (ans.isNotBlank()) ans else "-")
                append("\n\n")
            }
        }.trimEnd()
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(
            context,
            if (isArabic) "تم نسخ الأسئلة والإجابات" else "Questions and answers copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var responseToDelete by remember { mutableStateOf<SurveyResponse?>(null) }

    // Answers state for new occurrence
    val answersMap = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = survey.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("survey_detail_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val qCountText = if (isArabic) "${survey.questions.size} أسئلة" else "${survey.questions.size} questions"
                BadgeChip(
                    icon = Icons.Default.HelpOutline,
                    text = qCountText,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                )

                val freqText = if (isArabic) survey.frequency.titleAr else survey.frequency.titleEn
                BadgeChip(
                    icon = Icons.Default.Schedule,
                    text = freqText,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            }

            // Tabs: [Answer Survey] and [History]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "الإجابة على الاستبيان" else "Answer Survey")
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isArabic) "السجل (${responses.size})" else "History (${responses.size})"
                            )
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Answer Survey Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isArabic) "إجابة الاستبيان الحالي" else "Answer Questions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "أدخل إجاباتك النصية لكل سؤال بالترتيب ثم اضغط حفظ." else "Type your text responses in order, then click Save.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // All questions in their original order
                        survey.questions.forEachIndexed { index, question ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                    ) {
                                    Text(
                                        text = "${index + 1}. ${question.questionText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val currentAnswer = answersMap[question.id] ?: ""
                                    OutlinedTextField(
                                        value = currentAnswer,
                                        onValueChange = { answersMap[question.id] = it },
                                        placeholder = {
                                            Text(if (isArabic) "اكتب الإجابة هنا..." else "Enter your answer here...")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("answer_input_${question.id}"),
                                        minLines = 2,
                                        maxLines = 6,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                onSaveOccurrence(answersMap.toMap()) {
                                    answersMap.clear()
                                    selectedTab = 1 // Switch to history to view saved occurrence
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_occurrence_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "حفظ إجابات الاستبيان" else "Save Survey Occurrence",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                1 -> {
                    // History Tab
                    if (responses.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.History,
                            title = if (isArabic) "لا توجد إجابات سابقة محفوظة" else "No completed survey occurrences yet",
                            subtitle = if (isArabic) {
                                "عند الإجابة على الاستبيان وحفظه، ستظهر هنا استبياناتك السابقة بالترتيب (استبيان يوم 1، يوم 2، ...)."
                            } else {
                                "When you complete and save a survey occurrence, it will appear here (Survey day 1, Survey day 2, etc.)."
                            },
                            actionButtonText = if (isArabic) "الإجابة الآن" else "Answer Now",
                            onActionClick = { selectedTab = 0 },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(responses, key = { it.id }) { response ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("occurrence_card_${response.occurrenceNumber}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = response.displayTitle(isArabic),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = if (isArabic) "${response.answers.size} إجابات مسجلة" else "${response.answers.size} recorded answers",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Action Buttons: Edit, Copy, Delete
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { onOpenEditResponse(response) },
                                                    modifier = Modifier.testTag("edit_occurrence_${response.occurrenceNumber}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit response",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { copyResponseText(response) },
                                                    modifier = Modifier.testTag("copy_occurrence_${response.occurrenceNumber}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy response",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { responseToDelete = response },
                                                    modifier = Modifier.testTag("delete_occurrence_${response.occurrenceNumber}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete response",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Answers Preview
                                        survey.questions.forEachIndexed { qIndex, question ->
                                            val ans = response.answers.find { it.questionId == question.id }?.answerText ?: ""
                                            if (ans.isNotBlank()) {
                                                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                    Text(
                                                        text = "${qIndex + 1}. ${question.questionText}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = ans,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Occurrence Confirmation Dialog
        responseToDelete?.let { resp ->
            AlertDialog(
                onDismissRequest = { responseToDelete = null },
                title = {
                    Text(
                        text = if (isArabic) "حذف ${resp.displayTitle(isArabic)}" else "Delete ${resp.displayTitle(isArabic)}",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isArabic) {
                            "هل أنت متأكد من حذف هذه الإجابة المسجلة نهائياً؟ لن يتم إعادة ترقيم الأيام الأخرى."
                        } else {
                            "Are you sure you want to delete this recorded survey occurrence? Other occurrences will not be renumbered."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteResponse(resp.id)
                            responseToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_delete_occurrence_btn")
                    ) {
                        Text(if (isArabic) "حذف" else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { responseToDelete = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
