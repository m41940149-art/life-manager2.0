package com.example.model

import java.util.UUID

enum class SurveyFrequency(val titleAr: String, val titleEn: String) {
    DAILY("يومي", "Daily"),
    WEEKLY("أسبوعي", "Weekly"),
    MONTHLY("شهري", "Monthly"),
    YEARLY("سنوي", "Yearly")
}

enum class QuestionType(val titleAr: String, val titleEn: String) {
    RATING_5("تقييم (١ - ٥)", "Rating (1 - 5)"),
    YES_NO("نعم / لا", "Yes / No"),
    TEXT("نص حر", "Short Text"),
    SCALE_10("مقياس (١ - ١٠)", "Scale (1 - 10)")
}

data class SurveyQuestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: QuestionType = QuestionType.RATING_5,
    val options: List<String> = emptyList()
)

data class Survey(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val frequency: SurveyFrequency = SurveyFrequency.DAILY,
    val questions: List<SurveyQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
