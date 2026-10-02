package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    // --- Surveys ---
    @Query("SELECT * FROM surveys ORDER BY createdAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id LIMIT 1")
    suspend fun getSurveyById(id: String): SurveyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Query("DELETE FROM surveys WHERE id = :id")
    suspend fun deleteSurvey(id: String)

    // --- Questions ---
    @Query("SELECT * FROM survey_questions WHERE surveyId = :surveyId ORDER BY questionOrder ASC")
    fun getQuestionsForSurvey(surveyId: String): Flow<List<SurveyQuestionEntity>>

    @Query("SELECT * FROM survey_questions WHERE surveyId = :surveyId ORDER BY questionOrder ASC")
    suspend fun getQuestionsForSurveySync(surveyId: String): List<SurveyQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<SurveyQuestionEntity>)

    @Query("DELETE FROM survey_questions WHERE surveyId = :surveyId")
    suspend fun deleteQuestionsForSurvey(surveyId: String)

    // --- Responses ---
    @Query("SELECT * FROM survey_responses WHERE surveyId = :surveyId ORDER BY occurrenceNumber ASC")
    fun getResponsesForSurvey(surveyId: String): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_responses WHERE surveyId = :surveyId ORDER BY occurrenceNumber ASC")
    suspend fun getResponsesForSurveySync(surveyId: String): List<SurveyResponseEntity>

    @Query("SELECT * FROM survey_responses WHERE id = :id LIMIT 1")
    suspend fun getResponseById(id: String): SurveyResponseEntity?

    @Query("SELECT MAX(occurrenceNumber) FROM survey_responses WHERE surveyId = :surveyId")
    suspend fun getMaxOccurrenceNumber(surveyId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: SurveyResponseEntity)

    @Update
    suspend fun updateResponse(response: SurveyResponseEntity)

    @Query("DELETE FROM survey_responses WHERE id = :id")
    suspend fun deleteResponse(id: String)

    @Query("DELETE FROM survey_responses WHERE surveyId = :surveyId")
    suspend fun deleteResponsesForSurvey(surveyId: String)

    // --- Answers ---
    @Query("SELECT * FROM survey_answers WHERE responseId = :responseId")
    fun getAnswersForResponse(responseId: String): Flow<List<SurveyAnswerEntity>>

    @Query("SELECT * FROM survey_answers WHERE responseId = :responseId")
    suspend fun getAnswersForResponseSync(responseId: String): List<SurveyAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<SurveyAnswerEntity>)

    @Query("DELETE FROM survey_answers WHERE responseId = :responseId")
    suspend fun deleteAnswersForResponse(responseId: String)

    @Query("SELECT * FROM surveys")
    suspend fun getAllSurveysSync(): List<SurveyEntity>

    @Query("SELECT * FROM survey_questions")
    suspend fun getAllQuestionsSync(): List<SurveyQuestionEntity>

    @Query("SELECT * FROM survey_responses")
    suspend fun getAllResponsesSync(): List<SurveyResponseEntity>

    @Query("SELECT * FROM survey_answers")
    suspend fun getAllAnswersSync(): List<SurveyAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<SurveyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<SurveyResponseEntity>)

    // --- Reactive queries (so the UI refreshes when questions/answers change) ---
    @Query("SELECT * FROM survey_questions ORDER BY questionOrder ASC")
    fun getAllQuestionsFlow(): Flow<List<SurveyQuestionEntity>>

    @Query(
        "SELECT a.* FROM survey_answers a " +
        "INNER JOIN survey_responses r ON a.responseId = r.id " +
        "WHERE r.surveyId = :surveyId"
    )
    fun getAnswersForSurveyFlow(surveyId: String): Flow<List<SurveyAnswerEntity>>

    @Query("DELETE FROM survey_answers WHERE id IN (:ids)")
    suspend fun deleteAnswersByIds(ids: List<String>)

    @Query("DELETE FROM survey_answers WHERE responseId IN (SELECT id FROM survey_responses WHERE surveyId = :surveyId)")
    suspend fun deleteAnswersForSurvey(surveyId: String)
}
