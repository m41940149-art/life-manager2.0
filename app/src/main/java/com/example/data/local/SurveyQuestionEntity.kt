package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.SurveyQuestion

@Entity(
    tableName = "survey_questions",
    indices = [Index("surveyId")]
)
data class SurveyQuestionEntity(
    @PrimaryKey
    val id: String,
    val surveyId: String,
    val questionOrder: Int,
    val questionText: String
)

fun SurveyQuestionEntity.toDomain(): SurveyQuestion = SurveyQuestion(
    id = id,
    surveyId = surveyId,
    questionOrder = questionOrder,
    questionText = questionText
)

fun SurveyQuestion.toEntity(): SurveyQuestionEntity = SurveyQuestionEntity(
    id = id,
    surveyId = surveyId,
    questionOrder = questionOrder,
    questionText = questionText
)
