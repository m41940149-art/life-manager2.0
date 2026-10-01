package com.example.sync

import android.content.Context
import com.example.data.local.FolderEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitMissedResolutionEntity
import com.example.data.local.LifeManagerDatabase
import com.example.data.local.SurveyAnswerEntity
import com.example.data.local.SurveyEntity
import com.example.data.local.SurveyQuestionEntity
import com.example.data.local.SurveyResponseEntity
import com.example.data.local.SyncStatus
import com.example.data.local.TaskEntity
import com.example.data.local.TextCardEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class SyncResult {
    object Success : SyncResult()
    object NotConfigured : SyncResult()
    object SignedOut : SyncResult()
    object Offline : SyncResult()
    data class Error(val message: String) : SyncResult()
}

suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result) {}
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(Result.failure(exception))
        }
    }

class FirestoreSyncManager(
    private val context: Context,
    private val database: LifeManagerDatabase,
    private val syncPreferences: SyncPreferences
) {

    private val syncMutex = Mutex()

    /** True while a sync is running. */
    val isSyncing: Boolean
        get() = syncMutex.isLocked

    /** Time the last sync finished (used to ignore the DB writes the sync itself makes). */
    @Volatile
    var lastFinishedAt: Long = 0L
        private set

    /** Only one sync runs at a time; a second caller waits for the first to finish. */
    suspend fun sync(): SyncResult = syncMutex.withLock {
        try {
            performSync()
        } finally {
            lastFinishedAt = System.currentTimeMillis()
        }
    }

    private suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        if (!FirebaseConfigHelper.isFirebaseConfigured(context)) {
            syncPreferences.updateSyncState(
                SyncState.NOT_CONFIGURED,
                isConfigured = false,
                errorMessage = "Firebase is not configured. google-services.json is missing."
            )
            return@withContext SyncResult.NotConfigured
        }

        // Obtain authenticated Firebase user - Firebase UID is the canonical identity
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        }

        if (currentUser == null) {
            syncPreferences.updateSyncState(
                SyncState.SIGNED_OUT,
                isConfigured = true,
                uid = null,
                userEmail = null,
                errorMessage = "User is signed out. Local Room data is active."
            )
            return@withContext SyncResult.SignedOut
        }

        val uid = currentUser.uid
        val email = currentUser.email
        syncPreferences.setCurrentUser(uid, email)

        if (!NetworkUtils.isOnline(context)) {
            syncPreferences.updateSyncState(
                SyncState.OFFLINE,
                isConfigured = true,
                uid = uid,
                userEmail = email,
                errorMessage = "Device is currently offline."
            )
            return@withContext SyncResult.Offline
        }

        val firestore = FirebaseConfigHelper.getFirestore(context)
        if (firestore == null) {
            syncPreferences.updateSyncState(
                SyncState.NOT_CONFIGURED,
                isConfigured = false,
                uid = uid,
                userEmail = email,
                errorMessage = "Could not initialize Firestore."
            )
            return@withContext SyncResult.NotConfigured
        }

        syncPreferences.updateSyncState(
            SyncState.SYNCING,
            isConfigured = true,
            uid = uid,
            userEmail = email
        )

        try {
            // Strictly canonical cloud path: /users/{uid}/...
            val userDoc = firestore.collection("users").document(uid)

            // 1. Process local tombstones (deleted items)
            processTombstones(userDoc)

            // 2. Sync Tasks
            syncTasks(userDoc)

            // 3. Sync Habits & Completions & Missed Resolutions
            syncHabits(userDoc)
            syncHabitCompletions(userDoc)
            syncHabitResolutions(userDoc)

            // 4. Sync Data (Folders & TextCards)
            syncFolders(userDoc)
            syncTextCards(userDoc)

            // 5. Sync Surveys (Surveys, Questions, Responses, Answers)
            syncSurveys(userDoc)
            syncSurveyQuestions(userDoc)
            syncSurveyResponses(userDoc)
            syncSurveyAnswers(userDoc)

            syncPreferences.updateSyncState(
                SyncState.SYNCED,
                isConfigured = true,
                uid = uid,
                userEmail = email
            )
            SyncResult.Success
        } catch (e: Exception) {
            syncPreferences.updateSyncState(
                SyncState.ERROR,
                isConfigured = true,
                uid = uid,
                userEmail = email,
                errorMessage = e.message ?: "Sync error occurred"
            )
            SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }

    private suspend fun processTombstones(userDoc: com.google.firebase.firestore.DocumentReference) {
        val tombstones = database.syncTombstoneDao().getAllTombstones()
        for (t in tombstones) {
            try {
                when (t.entityType) {
                    "TASK" -> userDoc.collection("tasks").document(t.entityId).delete().await()
                    "HABIT" -> userDoc.collection("habits").document(t.entityId).delete().await()
                    "HABIT_COMPLETION" -> userDoc.collection("habit_completions").document("${t.entityId}__${t.extraId}").delete().await()
                    "FOLDER" -> userDoc.collection("folders").document(t.entityId).delete().await()
                    "TEXT_CARD" -> userDoc.collection("text_cards").document(t.entityId).delete().await()
                    "SURVEY" -> userDoc.collection("surveys").document(t.entityId).delete().await()
                    "SURVEY_RESPONSE" -> userDoc.collection("survey_responses").document(t.entityId).delete().await()
                }
                database.syncTombstoneDao().deleteTombstone(t.id)
            } catch (_: Exception) {}
        }
    }

    private suspend fun syncTasks(userDoc: com.google.firebase.firestore.DocumentReference) {
        val tasksCol = userDoc.collection("tasks")
        val localTasks = database.taskDao().getAllTasksSync()

        // Push local tasks
        for (task in localTasks) {
            val taskMap = hashMapOf<String, Any?>(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "dueDate" to task.dueDate,
                "dueTime" to task.dueTime,
                "isCompleted" to task.isCompleted,
                "createdAt" to task.createdAt,
                "completedAt" to task.completedAt,
                "updatedAt" to task.updatedAt,
                "priority" to task.priority,
                "category" to task.category
            )
            tasksCol.document(task.id).set(taskMap, SetOptions.merge()).await()
        }

        // Pull remote tasks
        val remoteTasks = tasksCol.get().await()
        for (doc in remoteTasks.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localTask = database.taskDao().getTaskById(id)
            if (localTask == null) {
                val newTask = TaskEntity(
                    id = id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    dueDate = doc.getLong("dueDate") ?: 0L,
                    dueTime = doc.getString("dueTime"),
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    completedAt = doc.getLong("completedAt"),
                    updatedAt = remoteUpdatedAt,
                    priority = doc.getString("priority") ?: "MEDIUM",
                    category = doc.getString("category") ?: "عام",
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.taskDao().insertTask(newTask)
            } else if (remoteUpdatedAt > localTask.updatedAt) {
                val updatedTask = localTask.copy(
                    title = doc.getString("title") ?: localTask.title,
                    description = doc.getString("description") ?: localTask.description,
                    dueDate = doc.getLong("dueDate") ?: localTask.dueDate,
                    dueTime = doc.getString("dueTime"),
                    isCompleted = doc.getBoolean("isCompleted") ?: localTask.isCompleted,
                    completedAt = doc.getLong("completedAt"),
                    updatedAt = remoteUpdatedAt,
                    priority = doc.getString("priority") ?: localTask.priority,
                    category = doc.getString("category") ?: localTask.category,
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.taskDao().updateTask(updatedTask)
            }
        }
    }

    private suspend fun syncHabits(userDoc: com.google.firebase.firestore.DocumentReference) {
        val habitsCol = userDoc.collection("habits")
        val localHabits = database.habitDao().getAllHabitsSync()

        // Push local habits
        for (h in localHabits) {
            val map = hashMapOf<String, Any?>(
                "id" to h.id,
                "name" to h.name,
                "description" to h.description,
                "isEveryDay" to h.isEveryDay,
                "daysOfWeek" to h.daysOfWeek,
                "notificationEnabled" to h.notificationEnabled,
                "notificationTime" to h.notificationTime,
                "createdAt" to h.createdAt,
                "updatedAt" to h.updatedAt,
                "colorHex" to h.colorHex,
                "categoryIcon" to h.categoryIcon
            )
            habitsCol.document(h.id).set(map, SetOptions.merge()).await()
        }

        // Pull remote habits
        val remoteHabits = habitsCol.get().await()
        for (doc in remoteHabits.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localHabit = database.habitDao().getHabitById(id)
            if (localHabit == null) {
                val newHabit = HabitEntity(
                    id = id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    isEveryDay = doc.getBoolean("isEveryDay") ?: true,
                    daysOfWeek = doc.getString("daysOfWeek") ?: "1,2,3,4,5,6,7",
                    notificationEnabled = doc.getBoolean("notificationEnabled") ?: false,
                    notificationTime = doc.getString("notificationTime"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = remoteUpdatedAt,
                    colorHex = doc.getLong("colorHex") ?: 0xFF006C5F,
                    categoryIcon = doc.getString("categoryIcon") ?: "check_circle",
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.habitDao().insertHabit(newHabit)
            } else if (remoteUpdatedAt > localHabit.updatedAt) {
                val updatedHabit = localHabit.copy(
                    name = doc.getString("name") ?: localHabit.name,
                    description = doc.getString("description") ?: localHabit.description,
                    isEveryDay = doc.getBoolean("isEveryDay") ?: localHabit.isEveryDay,
                    daysOfWeek = doc.getString("daysOfWeek") ?: localHabit.daysOfWeek,
                    notificationEnabled = doc.getBoolean("notificationEnabled") ?: localHabit.notificationEnabled,
                    notificationTime = doc.getString("notificationTime"),
                    updatedAt = remoteUpdatedAt,
                    colorHex = doc.getLong("colorHex") ?: localHabit.colorHex,
                    categoryIcon = doc.getString("categoryIcon") ?: localHabit.categoryIcon,
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.habitDao().updateHabit(updatedHabit)
            }
        }
    }

    private suspend fun syncHabitCompletions(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("habit_completions")
        val localCompletions = database.habitDao().getAllCompletionsSync()

        for (c in localCompletions) {
            val docId = "${c.habitId}__${c.dateTimestamp}"
            val map = hashMapOf(
                "habitId" to c.habitId,
                "dateTimestamp" to c.dateTimestamp,
                "completedAt" to c.completedAt
            )
            col.document(docId).set(map, SetOptions.merge()).await()
        }

        val remoteCompletions = col.get().await()
        for (doc in remoteCompletions.documents) {
            val habitId = doc.getString("habitId") ?: continue
            val dateTimestamp = doc.getLong("dateTimestamp") ?: continue
            val completedAt = doc.getLong("completedAt") ?: System.currentTimeMillis()
            if (!database.habitDao().isCompletedOnDate(habitId, dateTimestamp)) {
                database.habitDao().insertCompletion(
                    HabitCompletionEntity(
                        habitId = habitId,
                        dateTimestamp = dateTimestamp,
                        completedAt = completedAt
                    )
                )
            }
        }
    }

    private suspend fun syncHabitResolutions(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("habit_resolutions")
        val localResolutions = database.habitDao().getAllMissedResolutionsSync()

        for (r in localResolutions) {
            val docId = "${r.habitId}__${r.dateTimestamp}"
            val map = hashMapOf(
                "habitId" to r.habitId,
                "dateTimestamp" to r.dateTimestamp,
                "resolution" to r.resolution,
                "resolvedAt" to r.resolvedAt
            )
            col.document(docId).set(map, SetOptions.merge()).await()
        }

        val remoteResolutions = col.get().await()
        for (doc in remoteResolutions.documents) {
            val habitId = doc.getString("habitId") ?: continue
            val dateTimestamp = doc.getLong("dateTimestamp") ?: continue
            val resolution = doc.getString("resolution") ?: "BROKEN"
            val resolvedAt = doc.getLong("resolvedAt") ?: System.currentTimeMillis()
            database.habitDao().insertMissedResolution(
                HabitMissedResolutionEntity(
                    habitId = habitId,
                    dateTimestamp = dateTimestamp,
                    resolution = resolution,
                    resolvedAt = resolvedAt
                )
            )
        }
    }

    private suspend fun syncFolders(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("folders")
        val localFolders = database.folderDao().getAllFoldersSync()

        for (f in localFolders) {
            val map = hashMapOf<String, Any?>(
                "id" to f.id,
                "name" to f.name,
                "description" to f.description,
                "colorHex" to f.colorHex,
                "iconName" to f.iconName,
                "createdAt" to f.createdAt,
                "updatedAt" to f.updatedAt
            )
            col.document(f.id).set(map, SetOptions.merge()).await()
        }

        val remoteFolders = col.get().await()
        for (doc in remoteFolders.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localFolder = database.folderDao().getFolderById(id)
            if (localFolder == null) {
                val newFolder = FolderEntity(
                    id = id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    colorHex = doc.getLong("colorHex") ?: 0xFF006C5F,
                    iconName = doc.getString("iconName") ?: "folder",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = remoteUpdatedAt,
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.folderDao().insertFolder(newFolder)
            } else if (remoteUpdatedAt > localFolder.updatedAt) {
                val updatedFolder = localFolder.copy(
                    name = doc.getString("name") ?: localFolder.name,
                    description = doc.getString("description") ?: localFolder.description,
                    colorHex = doc.getLong("colorHex") ?: localFolder.colorHex,
                    iconName = doc.getString("iconName") ?: localFolder.iconName,
                    updatedAt = remoteUpdatedAt,
                    syncStatus = SyncStatus.SYNCED.name
                )
                database.folderDao().updateFolder(updatedFolder)
            }
        }
    }

    private suspend fun syncTextCards(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("text_cards")
        val localCards = database.textCardDao().getAllCardsSync()

        for (c in localCards) {
            val map = hashMapOf<String, Any?>(
                "id" to c.id,
                "title" to c.title,
                "content" to c.content,
                "folderId" to c.folderId,
                "isFavorite" to c.isFavorite,
                "isPasswordProtected" to c.isPasswordProtected,
                "passwordHash" to c.passwordHash,
                "tags" to c.tags,
                "createdAt" to c.createdAt,
                "updatedAt" to c.updatedAt
            )
            col.document(c.id).set(map, SetOptions.merge()).await()
        }

        val remoteCards = col.get().await()
        for (doc in remoteCards.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localCard = database.textCardDao().getCardById(id)
            if (localCard == null) {
                val newCard = TextCardEntity(
                    id = id,
                    title = doc.getString("title") ?: "",
                    content = doc.getString("content") ?: "",
                    folderId = doc.getString("folderId"),
                    isFavorite = doc.getBoolean("isFavorite") ?: false,
                    isPasswordProtected = doc.getBoolean("isPasswordProtected") ?: false,
                    passwordHash = doc.getString("passwordHash"),
                    tags = doc.getString("tags") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = remoteUpdatedAt
                )
                database.textCardDao().insertCard(newCard)
            } else if (remoteUpdatedAt > localCard.updatedAt) {
                val updatedCard = localCard.copy(
                    title = doc.getString("title") ?: localCard.title,
                    content = doc.getString("content") ?: localCard.content,
                    folderId = doc.getString("folderId"),
                    isFavorite = doc.getBoolean("isFavorite") ?: localCard.isFavorite,
                    isPasswordProtected = doc.getBoolean("isPasswordProtected") ?: localCard.isPasswordProtected,
                    passwordHash = doc.getString("passwordHash"),
                    tags = doc.getString("tags") ?: localCard.tags,
                    updatedAt = remoteUpdatedAt
                )
                database.textCardDao().updateCard(updatedCard)
            }
        }
    }

    private suspend fun syncSurveys(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("surveys")
        val localSurveys = database.surveyDao().getAllSurveysSync()

        for (s in localSurveys) {
            val map = hashMapOf<String, Any?>(
                "id" to s.id,
                "name" to s.name,
                "description" to s.description,
                "frequency" to s.frequency,
                "createdAt" to s.createdAt,
                "updatedAt" to s.updatedAt
            )
            col.document(s.id).set(map, SetOptions.merge()).await()
        }

        val remoteSurveys = col.get().await()
        for (doc in remoteSurveys.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localSurvey = database.surveyDao().getSurveyById(id)
            if (localSurvey == null) {
                val newSurvey = SurveyEntity(
                    id = id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    frequency = doc.getString("frequency") ?: "DAILY",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = remoteUpdatedAt
                )
                database.surveyDao().insertSurvey(newSurvey)
            }
        }
    }

    private suspend fun syncSurveyQuestions(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("survey_questions")
        val localQuestions = database.surveyDao().getAllQuestionsSync()

        for (q in localQuestions) {
            val map = hashMapOf(
                "id" to q.id,
                "surveyId" to q.surveyId,
                "questionOrder" to q.questionOrder,
                "questionText" to q.questionText
            )
            col.document(q.id).set(map, SetOptions.merge()).await()
        }

        val remoteQuestions = col.get().await()
        val questionsToInsert = mutableListOf<SurveyQuestionEntity>()
        for (doc in remoteQuestions.documents) {
            val id = doc.getString("id") ?: doc.id
            val surveyId = doc.getString("surveyId") ?: continue
            val questionOrder = doc.getLong("questionOrder")?.toInt() ?: 0
            val questionText = doc.getString("questionText") ?: ""
            questionsToInsert.add(
                SurveyQuestionEntity(
                    id = id,
                    surveyId = surveyId,
                    questionOrder = questionOrder,
                    questionText = questionText
                )
            )
        }
        if (questionsToInsert.isNotEmpty()) {
            database.surveyDao().insertQuestions(questionsToInsert)
        }
    }

    private suspend fun syncSurveyResponses(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("survey_responses")
        val localResponses = database.surveyDao().getAllResponsesSync()

        for (r in localResponses) {
            val map = hashMapOf(
                "id" to r.id,
                "surveyId" to r.surveyId,
                "occurrenceNumber" to r.occurrenceNumber,
                "occurrenceDate" to r.occurrenceDate,
                "createdAt" to r.createdAt,
                "updatedAt" to r.updatedAt
            )
            col.document(r.id).set(map, SetOptions.merge()).await()
        }

        val remoteResponses = col.get().await()
        for (doc in remoteResponses.documents) {
            val id = doc.getString("id") ?: doc.id
            val surveyId = doc.getString("surveyId") ?: continue
            val occurrenceNumber = doc.getLong("occurrenceNumber")?.toInt() ?: 1
            val occurrenceDate = doc.getLong("occurrenceDate") ?: System.currentTimeMillis()
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

            val localResponse = database.surveyDao().getResponseById(id)
            if (localResponse == null) {
                database.surveyDao().insertResponse(
                    SurveyResponseEntity(
                        id = id,
                        surveyId = surveyId,
                        occurrenceNumber = occurrenceNumber,
                        occurrenceDate = occurrenceDate,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            } else if (updatedAt > localResponse.updatedAt) {
                database.surveyDao().updateResponse(
                    localResponse.copy(
                        occurrenceDate = occurrenceDate,
                        updatedAt = updatedAt
                    )
                )
            }
        }
    }

    private suspend fun syncSurveyAnswers(userDoc: com.google.firebase.firestore.DocumentReference) {
        val col = userDoc.collection("survey_answers")
        val localAnswers = database.surveyDao().getAllAnswersSync()

        for (a in localAnswers) {
            val map = hashMapOf(
                "id" to a.id,
                "responseId" to a.responseId,
                "questionId" to a.questionId,
                "answerText" to a.answerText
            )
            col.document(a.id).set(map, SetOptions.merge()).await()
        }

        val remoteAnswers = col.get().await()
        val answersToInsert = mutableListOf<SurveyAnswerEntity>()
        for (doc in remoteAnswers.documents) {
            val id = doc.getString("id") ?: doc.id
            val responseId = doc.getString("responseId") ?: continue
            val questionId = doc.getString("questionId") ?: continue
            val answerText = doc.getString("answerText") ?: ""
            answersToInsert.add(
                SurveyAnswerEntity(
                    id = id,
                    responseId = responseId,
                    questionId = questionId,
                    answerText = answerText
                )
            )
        }
        if (answersToInsert.isNotEmpty()) {
            database.surveyDao().insertAnswers(answersToInsert)
        }
    }
}
