package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Survey
import com.example.model.SurveyFrequency

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val frequency: String = "DAILY",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SurveyEntity.toDomain(questions: List<com.example.model.SurveyQuestion> = emptyList()): Survey {
    val freq = try {
        SurveyFrequency.valueOf(frequency)
    } catch (_: Exception) {
        SurveyFrequency.DAILY
    }
    return Survey(
        id = id,
        name = name,
        description = description,
        frequency = freq,
        questions = questions,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Survey.toEntity(): SurveyEntity = SurveyEntity(
    id = id,
    name = name,
    description = description,
    frequency = frequency.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)
