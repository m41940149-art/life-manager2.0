package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.SurveyAnswer
import com.example.model.SurveyResponse

@Entity(
    tableName = "survey_responses",
    indices = [Index("surveyId")]
)
data class SurveyResponseEntity(
    @PrimaryKey
    val id: String,
    val surveyId: String,
    val occurrenceNumber: Int,
    val occurrenceDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SurveyResponseEntity.toDomain(answers: List<SurveyAnswer> = emptyList()): SurveyResponse = SurveyResponse(
    id = id,
    surveyId = surveyId,
    occurrenceNumber = occurrenceNumber,
    occurrenceDate = occurrenceDate,
    answers = answers,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SurveyResponse.toEntity(): SurveyResponseEntity = SurveyResponseEntity(
    id = id,
    surveyId = surveyId,
    occurrenceNumber = occurrenceNumber,
    occurrenceDate = occurrenceDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Entity(
    tableName = "survey_answers",
    indices = [
        Index("responseId"),
        Index("questionId")
    ]
)
data class SurveyAnswerEntity(
    @PrimaryKey
    val id: String,
    val responseId: String,
    val questionId: String,
    val answerText: String
)

fun SurveyAnswerEntity.toDomain(): SurveyAnswer = SurveyAnswer(
    id = id,
    responseId = responseId,
    questionId = questionId,
    answerText = answerText
)

fun SurveyAnswer.toEntity(): SurveyAnswerEntity = SurveyAnswerEntity(
    id = id,
    responseId = responseId,
    questionId = questionId,
    answerText = answerText
)
