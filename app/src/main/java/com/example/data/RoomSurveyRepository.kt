package com.example.data

import com.example.data.local.SurveyAnswerEntity
import com.example.data.local.SurveyDao
import com.example.data.local.SurveyResponseEntity
import com.example.data.local.SyncTombstoneDao
import com.example.data.local.SyncTombstoneEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.Survey
import com.example.model.SurveyAnswer
import com.example.model.SurveyQuestion
import com.example.model.SurveyResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomSurveyRepository(
    private val surveyDao: SurveyDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : SurveyRepository {

    override fun getSurveys(): Flow<List<Survey>> {
        return surveyDao.getAllSurveys().map { entities ->
            entities.map { entity ->
                val questions = surveyDao.getQuestionsForSurveySync(entity.id).map { it.toDomain() }
                entity.toDomain(questions)
            }
        }
    }

    override suspend fun getSurveyById(surveyId: String): Survey? {
        val entity = surveyDao.getSurveyById(surveyId) ?: return null
        val questions = surveyDao.getQuestionsForSurveySync(surveyId).map { it.toDomain() }
        return entity.toDomain(questions)
    }

    override suspend fun addSurvey(survey: Survey, questions: List<SurveyQuestion>) {
        surveyDao.insertSurvey(survey.toEntity())
        val questionEntities = questions.mapIndexed { index, q ->
            q.copy(surveyId = survey.id, questionOrder = index).toEntity()
        }
        surveyDao.insertQuestions(questionEntities)
    }

    override suspend fun deleteSurvey(surveyId: String) {
        val responses = surveyDao.getResponsesForSurveySync(surveyId)
        for (r in responses) {
            surveyDao.deleteAnswersForResponse(r.id)
        }
        surveyDao.deleteResponsesForSurvey(surveyId)
        surveyDao.deleteQuestionsForSurvey(surveyId)
        surveyDao.deleteSurvey(surveyId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "SURVEY",
                entityId = surveyId
            )
        )
    }

    override fun getQuestionsForSurvey(surveyId: String): Flow<List<SurveyQuestion>> {
        return surveyDao.getQuestionsForSurvey(surveyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getResponsesForSurvey(surveyId: String): Flow<List<SurveyResponse>> {
        return surveyDao.getResponsesForSurvey(surveyId).map { responses ->
            responses.map { r ->
                val answers = surveyDao.getAnswersForResponseSync(r.id).map { it.toDomain() }
                r.toDomain(answers)
            }
        }
    }

    override suspend fun getResponseById(responseId: String): SurveyResponse? {
        val entity = surveyDao.getResponseById(responseId) ?: return null
        val answers = surveyDao.getAnswersForResponseSync(responseId).map { it.toDomain() }
        return entity.toDomain(answers)
    }

    override suspend fun saveNewResponse(surveyId: String, answers: Map<String, String>): SurveyResponse {
        val maxOccurrence = surveyDao.getMaxOccurrenceNumber(surveyId) ?: 0
        val nextNumber = maxOccurrence + 1
        val responseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val responseEntity = SurveyResponseEntity(
            id = responseId,
            surveyId = surveyId,
            occurrenceNumber = nextNumber,
            occurrenceDate = now,
            createdAt = now,
            updatedAt = now
        )
        surveyDao.insertResponse(responseEntity)

        val answerEntities = answers.map { (questionId, text) ->
            SurveyAnswerEntity(
                id = UUID.randomUUID().toString(),
                responseId = responseId,
                questionId = questionId,
                answerText = text
            )
        }
        surveyDao.insertAnswers(answerEntities)

        return responseEntity.toDomain(answerEntities.map { it.toDomain() })
    }

    override suspend fun updateResponse(responseId: String, answers: Map<String, String>) {
        val existing = surveyDao.getResponseById(responseId) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(updatedAt = now)
        surveyDao.updateResponse(updated)

        // Replace answers for this response
        surveyDao.deleteAnswersForResponse(responseId)
        val answerEntities = answers.map { (questionId, text) ->
            SurveyAnswerEntity(
                id = UUID.randomUUID().toString(),
                responseId = responseId,
                questionId = questionId,
                answerText = text
            )
        }
        surveyDao.insertAnswers(answerEntities)
    }

    override suspend fun copyResponse(responseId: String): SurveyResponse {
        val sourceResponse = surveyDao.getResponseById(responseId)
            ?: throw IllegalArgumentException("Response $responseId not found")
        val sourceAnswers = surveyDao.getAnswersForResponseSync(responseId)

        val maxOccurrence = surveyDao.getMaxOccurrenceNumber(sourceResponse.surveyId) ?: 0
        val nextNumber = maxOccurrence + 1
        val newResponseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val newResponseEntity = SurveyResponseEntity(
            id = newResponseId,
            surveyId = sourceResponse.surveyId,
            occurrenceNumber = nextNumber,
            occurrenceDate = now,
            createdAt = now,
            updatedAt = now
        )
        surveyDao.insertResponse(newResponseEntity)

        val copiedAnswers = sourceAnswers.map {
            SurveyAnswerEntity(
                id = UUID.randomUUID().toString(),
                responseId = newResponseId,
                questionId = it.questionId,
                answerText = it.answerText
            )
        }
        surveyDao.insertAnswers(copiedAnswers)

        return newResponseEntity.toDomain(copiedAnswers.map { it.toDomain() })
    }

    override suspend fun deleteResponse(responseId: String) {
        surveyDao.deleteAnswersForResponse(responseId)
        surveyDao.deleteResponse(responseId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "SURVEY_RESPONSE",
                entityId = responseId
            )
        )
    }
}
