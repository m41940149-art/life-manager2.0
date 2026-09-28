package com.example.model

import java.util.UUID

enum class SurveyFrequency(val titleAr: String, val titleEn: String) {
    DAILY("يومي", "Daily"),
    WEEKLY("أسبوعي", "Weekly"),
    MONTHLY("شهري", "Monthly"),
    YEARLY("سنوي", "Yearly")
}

data class SurveyQuestion(
    val id: String = UUID.randomUUID().toString(),
    val surveyId: String = "",
    val questionOrder: Int = 0,
    val questionText: String = ""
)

data class Survey(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val frequency: SurveyFrequency = SurveyFrequency.DAILY,
    val questions: List<SurveyQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SurveyAnswer(
    val id: String = UUID.randomUUID().toString(),
    val responseId: String = "",
    val questionId: String,
    val answerText: String
)

data class SurveyResponse(
    val id: String = UUID.randomUUID().toString(),
    val surveyId: String,
    val occurrenceNumber: Int,
    val occurrenceDate: Long = System.currentTimeMillis(),
    val answers: List<SurveyAnswer> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun displayTitle(isArabic: Boolean): String {
        return if (isArabic) "استبيان يوم $occurrenceNumber" else "Survey day $occurrenceNumber"
    }
}
