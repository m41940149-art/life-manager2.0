package com.example.data

import com.example.model.Survey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SurveyRepository {
    fun getSurveys(): Flow<List<Survey>>
    suspend fun addSurvey(survey: Survey)
    suspend fun deleteSurvey(surveyId: String)
}

class InMemorySurveyRepository : SurveyRepository {
    private val _surveys = MutableStateFlow<List<Survey>>(emptyList())

    override fun getSurveys(): Flow<List<Survey>> = _surveys.asStateFlow()

    override suspend fun addSurvey(survey: Survey) {
        _surveys.update { listOf(survey) + it }
    }

    override suspend fun deleteSurvey(surveyId: String) {
        _surveys.update { list ->
            list.filterNot { it.id == surveyId }
        }
    }
}
