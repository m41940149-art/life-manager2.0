package com.example.ui.surveys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.SurveyRepository
import com.example.model.Survey
import com.example.model.SurveyFrequency
import com.example.model.SurveyQuestion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SurveysUiState(
    val surveys: List<Survey> = emptyList(),
    val totalSurveys: Int = 0,
    val dailyCount: Int = 0,
    val weeklyCount: Int = 0
)

class SurveysViewModel(
    private val repository: SurveyRepository = AppContainer.surveyRepository
) : ViewModel() {

    val uiState: StateFlow<SurveysUiState> = repository.getSurveys()
        .map { list ->
            SurveysUiState(
                surveys = list,
                totalSurveys = list.size,
                dailyCount = list.count { it.frequency == SurveyFrequency.DAILY },
                weeklyCount = list.count { it.frequency == SurveyFrequency.WEEKLY }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SurveysUiState()
        )

    fun addSurvey(
        name: String,
        description: String,
        frequency: SurveyFrequency,
        questions: List<SurveyQuestion>
    ) {
        viewModelScope.launch {
            val survey = Survey(
                name = name.trim(),
                description = description.trim(),
                frequency = frequency,
                questions = questions
            )
            repository.addSurvey(survey)
        }
    }

    fun deleteSurvey(surveyId: String) {
        viewModelScope.launch {
            repository.deleteSurvey(surveyId)
        }
    }
}
