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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomSurveyRepository(
    private val surveyDao: SurveyDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : SurveyRepository {

    override fun getSurveys(): Flow<List<Survey>> {
        // Observe BOTH tables so the list refreshes when questions arrive after the survey row.
        return combine(
            surveyDao.getAllSurveys(),
            surveyDao.getAllQuestionsFlow()
        ) { surveys, allQuestions ->
            val bySurvey = allQuestions.groupBy { it.surveyId }
            surveys.map { entity ->
                val questions = (bySurvey[entity.id] ?: emptyList())
                    .sortedBy { it.questionOrder }
                    .map { it.toDomain() }
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
        val questions = surveyDao.getQuestionsForSurveySync(surveyId)
        // Tombstones so the cloud copies are deleted too (otherwise they come back on next pull)
        for (r in responses) {
            for (a in surveyDao.getAnswersForResponseSync(r.id)) {
                syncTombstoneDao?.insertTombstone(SyncTombstoneEntity(entityType = "SURVEY_ANSWER", entityId = a.id))
            }
            syncTombstoneDao?.insertTombstone(SyncTombstoneEntity(entityType = "SURVEY_RESPONSE", entityId = r.id))
        }
        for (q in questions) {
            syncTombstoneDao?.insertTombstone(SyncTombstoneEntity(entityType = "SURVEY_QUESTION", entityId = q.id))
        }
        surveyDao.deleteAnswersForSurvey(surveyId)
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
        // Observe responses AND answers: answers are written after the response row,
        // so watching only the responses table showed incomplete answers.
        return combine(
            surveyDao.getResponsesForSurvey(surveyId),
            surveyDao.getAnswersForSurveyFlow(surveyId)
        ) { responses, allAnswers ->
            val byResponse = allAnswers.groupBy { it.responseId }
            responses.map { r ->
                r.toDomain((byResponse[r.id] ?: emptyList()).map { it.toDomain() })
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
                id = answerId(responseId, questionId),
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
        surveyDao.updateResponse(existing.copy(updatedAt = now))

        val answerEntities = answers.map { (questionId, text) ->
            SurveyAnswerEntity(
                id = answerId(responseId, questionId),
                responseId = responseId,
                questionId = questionId,
                answerText = text
            )
        }
        // Remove legacy answers (random ids) and make sure the cloud copies are deleted as well
        val keepIds = answerEntities.map { it.id }.toSet()
        val obsolete = surveyDao.getAnswersForResponseSync(responseId).filter { it.id !in keepIds }
        if (obsolete.isNotEmpty()) {
            surveyDao.deleteAnswersByIds(obsolete.map { it.id })
            for (a in obsolete) {
                syncTombstoneDao?.insertTombstone(SyncTombstoneEntity(entityType = "SURVEY_ANSWER", entityId = a.id))
            }
        }
        // Same ids -> REPLACE overwrites in place (no delete/re-insert gap)
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
                id = answerId(newResponseId, it.questionId),
                responseId = newResponseId,
                questionId = it.questionId,
                answerText = it.answerText
            )
        }
        surveyDao.insertAnswers(copiedAnswers)

        return newResponseEntity.toDomain(copiedAnswers.map { it.toDomain() })
    }

    override suspend fun deleteResponse(responseId: String) {
        for (a in surveyDao.getAnswersForResponseSync(responseId)) {
            syncTombstoneDao?.insertTombstone(SyncTombstoneEntity(entityType = "SURVEY_ANSWER", entityId = a.id))
        }
        surveyDao.deleteAnswersForResponse(responseId)
        surveyDao.deleteResponse(responseId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                entityType = "SURVEY_RESPONSE",
                entityId = responseId
            )
        )
    }

    private fun answerId(responseId: String, questionId: String) = "${responseId}_$questionId"
}
