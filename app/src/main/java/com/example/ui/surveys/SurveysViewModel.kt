package com.example.ui.surveys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.SurveyRepository
import com.example.model.Survey
import com.example.model.SurveyFrequency
import com.example.model.SurveyQuestion
import com.example.model.SurveyResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SurveysUiState(
    val surveys: List<Survey> = emptyList(),
    val currentSurvey: Survey? = null,
    val currentResponses: List<SurveyResponse> = emptyList(),
    val isCreatingSurvey: Boolean = false,
    val selectedResponseForEdit: SurveyResponse? = null
)

class SurveysViewModel(
    private val repository: SurveyRepository = AppContainer.surveyRepository
) : ViewModel() {

    private val _currentSurveyId = MutableStateFlow<String?>(null)
    private val _isCreatingSurvey = MutableStateFlow(false)
    private val _selectedResponseForEdit = MutableStateFlow<SurveyResponse?>(null)

    val uiState: StateFlow<SurveysUiState> = combine(
        repository.getSurveys(),
        _currentSurveyId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getResponsesForSurvey(id)
        },
        _currentSurveyId,
        _isCreatingSurvey,
        _selectedResponseForEdit
    ) { surveys, responses, currentSurveyId, isCreating, responseForEdit ->
        val currentSurvey = surveys.find { it.id == currentSurveyId }
        SurveysUiState(
            surveys = surveys,
            currentSurvey = currentSurvey,
            currentResponses = responses,
            isCreatingSurvey = isCreating,
            selectedResponseForEdit = responseForEdit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SurveysUiState()
    )

    fun startCreateSurvey() {
        _isCreatingSurvey.value = true
    }

    fun cancelCreateSurvey() {
        _isCreatingSurvey.value = false
    }

    fun openSurvey(surveyId: String) {
        _currentSurveyId.value = surveyId
        _selectedResponseForEdit.value = null
    }

    fun navigateBackToSurveysList(): Boolean {
        if (_selectedResponseForEdit.value != null) {
            _selectedResponseForEdit.value = null
            return true
        }
        if (_currentSurveyId.value != null) {
            _currentSurveyId.value = null
            return true
        }
        if (_isCreatingSurvey.value) {
            _isCreatingSurvey.value = false
            return true
        }
        return false
    }

    fun openEditResponse(response: SurveyResponse) {
        _selectedResponseForEdit.value = response
    }

    fun cancelEditResponse() {
        _selectedResponseForEdit.value = null
    }

    fun createSurvey(
        name: String,
        frequency: SurveyFrequency,
        questionTexts: List<String>
    ) {
        viewModelScope.launch {
            val surveyId = UUID.randomUUID().toString()
            val questions = questionTexts.mapIndexed { index, text ->
                SurveyQuestion(
                    id = UUID.randomUUID().toString(),
                    surveyId = surveyId,
                    questionOrder = index,
                    questionText = text.trim()
                )
            }
            val survey = Survey(
                id = surveyId,
                name = name.trim(),
                frequency = frequency,
                questions = questions,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.addSurvey(survey, questions)
            _isCreatingSurvey.value = false
        }
    }

    fun deleteSurvey(surveyId: String) {
        viewModelScope.launch {
            if (_currentSurveyId.value == surveyId) {
                _currentSurveyId.value = null
                _selectedResponseForEdit.value = null
            }
            repository.deleteSurvey(surveyId)
        }
    }

    fun saveOccurrence(surveyId: String, answers: Map<String, String>, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveNewResponse(surveyId, answers)
            onSaved?.invoke()
        }
    }

    fun updateOccurrence(responseId: String, answers: Map<String, String>, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateResponse(responseId, answers)
            _selectedResponseForEdit.value = null
            onSaved?.invoke()
        }
    }

    fun copyOccurrence(responseId: String) {
        viewModelScope.launch {
            repository.copyResponse(responseId)
        }
    }

    fun deleteOccurrence(responseId: String) {
        viewModelScope.launch {
            if (_selectedResponseForEdit.value?.id == responseId) {
                _selectedResponseForEdit.value = null
            }
            repository.deleteResponse(responseId)
        }
    }
}
