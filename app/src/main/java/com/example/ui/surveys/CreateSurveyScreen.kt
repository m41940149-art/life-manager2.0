package com.example.ui.surveys

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SurveyFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSurveyScreen(
    isArabic: Boolean,
    onBack: () -> Unit,
    onSaveSurvey: (name: String, frequency: SurveyFrequency, questions: List<String>) -> Unit
) {
    BackHandler(onBack = onBack)

    var name by remember { mutableStateOf("") }
    var countInput by remember { mutableStateOf("3") }
    var frequency by remember { mutableStateOf(SurveyFrequency.DAILY) }

    // List of question strings matching count
    val questionTexts = remember {
        mutableStateListOf("", "", "")
    }

    var nameError by remember { mutableStateOf(false) }
    var countError by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun updateQuestionCount(newCountStr: String) {
        countInput = newCountStr
        val count = newCountStr.toIntOrNull()
        if (count == null || count <= 0) {
            countError = if (isArabic) "يرجى إدخال عدد صحيح أكبر من صفر" else "Please enter a valid number greater than 0"
        } else if (count > 50) {
            countError = if (isArabic) "الحد الأقصى 50 سؤالاً" else "Maximum 50 questions"
        } else {
            countError = null
            while (questionTexts.size < count) {
                questionTexts.add("")
            }
            while (questionTexts.size > count) {
                questionTexts.removeAt(questionTexts.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "إنشاء استبيان جديد" else "Create New Survey",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("create_survey_back_btn")) {
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Survey Name Input
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                    if (validationError != null) validationError = null
                },
                label = { Text(if (isArabic) "اسم الاستبيان *" else "Survey Name *") },
                placeholder = { Text(if (isArabic) "مثال: مراجعة العادات اليومية" else "e.g., Daily Habit Review") },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Number of Questions Input
            OutlinedTextField(
                value = countInput,
                onValueChange = { updateQuestionCount(it) },
                label = { Text(if (isArabic) "عدد الأسئلة *" else "Number of Questions *") },
                placeholder = { Text("e.g., 8") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = countError != null,
                supportingText = if (countError != null) {
                    { Text(countError!!) }
                } else {
                    { Text(if (isArabic) "اكتب عدد الأسئلة وسيظهر لك هذا العدد من الحقول" else "Enter the number to display exact question fields") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("survey_question_count_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Frequency Selection
            Text(
                text = if (isArabic) "التكرار الدوري (Frequency) *" else "Frequency *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            Text(
                text = if (isArabic) "نصوص الأسئلة (${questionTexts.size})" else "Questions (${questionTexts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isArabic) "يرجى كتابة نص كل سؤال يدوياً." else "Enter the text for each question manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic question input fields (exactly N fields)
            questionTexts.forEachIndexed { index, currentText ->
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
                            text = if (isArabic) "السؤال ${index + 1} *" else "Question ${index + 1} *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = {
                                questionTexts[index] = it
                                if (validationError != null) validationError = null
                            },
                            placeholder = {
                                Text(if (isArabic) "اكتب نص السؤال ${index + 1}..." else "Enter question ${index + 1}...")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_input_$index"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Survey Button
            Button(
                onClick = {
                    when {
                        name.isBlank() -> {
                            nameError = true
                        }
                        questionTexts.isEmpty() -> {
                            validationError = if (isArabic) "يرجى إضافة سؤال واحد على الأقل" else "Please add at least one question"
                        }
                        questionTexts.any { it.isBlank() } -> {
                            validationError = if (isArabic) "يجب أن تحتوي جميع الأسئلة على نص مكتوب" else "All questions must contain user-entered text"
                        }
                        else -> {
                            onSaveSurvey(name.trim(), frequency, questionTexts.map { it.trim() })
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_save_survey_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "حفظ الاستبيان" else "Save Survey",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
