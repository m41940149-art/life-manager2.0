package com.example.data

import com.example.model.Survey
import com.example.model.SurveyQuestion
import com.example.model.SurveyResponse
import kotlinx.coroutines.flow.Flow

interface SurveyRepository {
    fun getSurveys(): Flow<List<Survey>>
    suspend fun getSurveyById(surveyId: String): Survey?
    suspend fun addSurvey(survey: Survey, questions: List<SurveyQuestion>)
    suspend fun deleteSurvey(surveyId: String)

    fun getQuestionsForSurvey(surveyId: String): Flow<List<SurveyQuestion>>
    fun getResponsesForSurvey(surveyId: String): Flow<List<SurveyResponse>>
    suspend fun getResponseById(responseId: String): SurveyResponse?
    suspend fun saveNewResponse(surveyId: String, answers: Map<String, String>): SurveyResponse
    suspend fun updateResponse(responseId: String, answers: Map<String, String>)
    suspend fun copyResponse(responseId: String): SurveyResponse
    suspend fun deleteResponse(responseId: String)
}

class InMemorySurveyRepository : SurveyRepository {
    private val surveys = mutableListOf<Survey>()
    private val questions = mutableListOf<SurveyQuestion>()
    private val responses = mutableListOf<SurveyResponse>()

    override fun getSurveys(): Flow<List<Survey>> = kotlinx.coroutines.flow.flowOf(surveys.toList())

    override suspend fun getSurveyById(surveyId: String): Survey? {
        val s = surveys.find { it.id == surveyId } ?: return null
        val qs = questions.filter { it.surveyId == surveyId }.sortedBy { it.questionOrder }
        return s.copy(questions = qs)
    }

    override suspend fun addSurvey(survey: Survey, questions: List<SurveyQuestion>) {
        this.surveys.add(survey.copy(questions = questions))
        this.questions.addAll(questions)
    }

    override suspend fun deleteSurvey(surveyId: String) {
        surveys.removeAll { it.id == surveyId }
        questions.removeAll { it.surveyId == surveyId }
        responses.removeAll { it.surveyId == surveyId }
    }

    override fun getQuestionsForSurvey(surveyId: String): Flow<List<SurveyQuestion>> =
        kotlinx.coroutines.flow.flowOf(questions.filter { it.surveyId == surveyId }.sortedBy { it.questionOrder })

    override fun getResponsesForSurvey(surveyId: String): Flow<List<SurveyResponse>> =
        kotlinx.coroutines.flow.flowOf(responses.filter { it.surveyId == surveyId }.sortedBy { it.occurrenceNumber })

    override suspend fun getResponseById(responseId: String): SurveyResponse? =
        responses.find { it.id == responseId }

    override suspend fun saveNewResponse(surveyId: String, answers: Map<String, String>): SurveyResponse {
        val nextOccurrence = (responses.filter { it.surveyId == surveyId }.maxOfOrNull { it.occurrenceNumber } ?: 0) + 1
        val responseId = java.util.UUID.randomUUID().toString()
        val answerList = answers.map { (qId, text) ->
            com.example.model.SurveyAnswer(
                id = java.util.UUID.randomUUID().toString(),
                responseId = responseId,
                questionId = qId,
                answerText = text
            )
        }
        val response = SurveyResponse(
            id = responseId,
            surveyId = surveyId,
            occurrenceNumber = nextOccurrence,
            occurrenceDate = System.currentTimeMillis(),
            answers = answerList
        )
        responses.add(response)
        return response
    }

    override suspend fun updateResponse(responseId: String, answers: Map<String, String>) {
        val idx = responses.indexOfFirst { it.id == responseId }
        if (idx >= 0) {
            val old = responses[idx]
            val answerList = answers.map { (qId, text) ->
                com.example.model.SurveyAnswer(
                    id = java.util.UUID.randomUUID().toString(),
                    responseId = responseId,
                    questionId = qId,
                    answerText = text
                )
            }
            responses[idx] = old.copy(answers = answerList, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun copyResponse(responseId: String): SurveyResponse {
        val source = responses.find { it.id == responseId } ?: throw IllegalArgumentException("Response not found")
        val nextOccurrence = (responses.filter { it.surveyId == source.surveyId }.maxOfOrNull { it.occurrenceNumber } ?: 0) + 1
        val newResponseId = java.util.UUID.randomUUID().toString()
        val copiedAnswers = source.answers.map {
            it.copy(
                id = java.util.UUID.randomUUID().toString(),
                responseId = newResponseId
            )
        }
        val newResponse = SurveyResponse(
            id = newResponseId,
            surveyId = source.surveyId,
            occurrenceNumber = nextOccurrence,
            occurrenceDate = System.currentTimeMillis(),
            answers = copiedAnswers,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        responses.add(newResponse)
        return newResponse
    }

    override suspend fun deleteResponse(responseId: String) {
        responses.removeAll { it.id == responseId }
    }
}
