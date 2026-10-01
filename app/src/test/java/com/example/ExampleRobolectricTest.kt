package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoomDataRepository
import com.example.data.RoomHabitRepository
import com.example.data.RoomSurveyRepository
import com.example.data.RoomTaskRepository
import com.example.data.local.LifeManagerDatabase
import com.example.model.Folder
import com.example.model.Habit
import com.example.model.Survey
import com.example.model.SurveyFrequency
import com.example.model.SurveyQuestion
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TextCard
import com.example.sync.FirebaseConfigHelper
import com.example.sync.FirestoreSyncManager
import com.example.sync.SyncPreferences
import com.example.sync.SyncResult
import com.example.sync.SyncState
import com.example.util.DateTimeUtils
import com.example.util.HabitStreakCalculator
import com.example.util.PasswordUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: LifeManagerDatabase
    private lateinit var roomTaskRepo: RoomTaskRepository
    private lateinit var roomHabitRepo: RoomHabitRepository
    private lateinit var roomDataRepo: RoomDataRepository
    private lateinit var roomSurveyRepo: RoomSurveyRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = LifeManagerDatabase.getInMemoryDatabase(context)
        roomTaskRepo = RoomTaskRepository(db.taskDao(), db.syncTombstoneDao())
        roomHabitRepo = RoomHabitRepository(db.habitDao(), context, db.syncTombstoneDao())
        roomDataRepo = RoomDataRepository(db.folderDao(), db.textCardDao(), db.syncTombstoneDao())
        roomSurveyRepo = RoomSurveyRepository(db.surveyDao(), db.syncTombstoneDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Life Manager", appName)
    }

    @Test
    fun `database starts with zero user data across all tables`() {
        runBlocking {
            val tasks = roomTaskRepo.getTasks().first()
            val habits = roomHabitRepo.getHabits().first()
            val folders = roomDataRepo.getFolders().first()
            val cards = roomDataRepo.getTextCards().first()
            val surveys = roomSurveyRepo.getSurveys().first()

            assertEquals(0, tasks.size)
            assertEquals(0, habits.size)
            assertEquals(0, folders.size)
            assertEquals(0, cards.size)
            assertEquals(0, surveys.size)
        }
    }

    @Test
    fun `room database persistence operations for tasks work reactively`() {
        runBlocking {
            val initialList = roomTaskRepo.getTasks().first()
            assertEquals(0, initialList.size)

            val today = DateTimeUtils.getTodayStartOfDay()
            val task1 = Task(
                id = "task-room-1",
                title = "مهمة قاعدة البيانات الأولى",
                description = "اختبار الحفظ الدائم في Room",
                dueDate = today,
                dueTime = "09:00 ص",
                priority = TaskPriority.HIGH,
                category = "عمل"
            )

            // 1. CREATE -> Save to Room
            roomTaskRepo.addTask(task1)

            val afterInsert = roomTaskRepo.getTasks().first()
            assertEquals(1, afterInsert.size)
            assertEquals("مهمة قاعدة البيانات الأولى", afterInsert.first().title)
            assertEquals(today, afterInsert.first().dueDate)
            assertEquals("09:00 ص", afterInsert.first().dueTime)
            assertFalse(afterInsert.first().isCompleted)

            // 2. COMPLETE -> Update Room
            roomTaskRepo.toggleTaskCompletion(task1.id)
            val afterComplete = roomTaskRepo.getTasks().first()
            val completedTask = afterComplete.first { it.id == task1.id }
            assertTrue(completedTask.isCompleted)
            assertNotNull(completedTask.completedAt)

            // 3. UNCOMPLETE -> Update Room
            roomTaskRepo.toggleTaskCompletion(task1.id)
            val afterUncomplete = roomTaskRepo.getTasks().first()
            val uncompletedTask = afterUncomplete.first { it.id == task1.id }
            assertFalse(uncompletedTask.isCompleted)
            assertNull(uncompletedTask.completedAt)

            // 4. EDIT -> Update Room
            val editedTask = uncompletedTask.copy(
                title = "مهمة معدلة ومحفوظة في روم",
                description = "تم تعديل الوصف بنجاح"
            )
            roomTaskRepo.updateTask(editedTask)

            val afterEdit = roomTaskRepo.getTasks().first()
            assertEquals("مهمة معدلة ومحفوظة في روم", afterEdit.first { it.id == task1.id }.title)
            assertEquals("تم تعديل الوصف بنجاح", afterEdit.first { it.id == task1.id }.description)

            // 5. DELETE -> Delete from Room
            roomTaskRepo.deleteTask(task1.id)
            val afterDelete = roomTaskRepo.getTasks().first()
            assertEquals(0, afterDelete.size)
        }
    }

    @Test
    fun `test case 1 - habit created today has no missed habit`() {
        val today = DateTimeUtils.getTodayStartOfDay()
        val result = HabitStreakCalculator.calculate(
            createdAt = today,
            isEveryDay = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            completionTimestamps = emptySet(),
            currentDayTimestamp = today
        )
        assertTrue(result.pendingMissedOccurrences.isEmpty())
        assertNull(result.earliestPendingMissedDate)
        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `test case 2 - habit created yesterday and completed has no missed habit and streak continues`() {
        val yesterday = DateTimeUtils.getYesterdayStartOfDay()
        val today = DateTimeUtils.getTodayStartOfDay()

        val result = HabitStreakCalculator.calculate(
            createdAt = yesterday,
            isEveryDay = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            completionTimestamps = setOf(yesterday),
            currentDayTimestamp = today
        )
        assertTrue(result.pendingMissedOccurrences.isEmpty())
        assertNull(result.earliestPendingMissedDate)
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `test case 3 - habit created yesterday not completed shows missed occurrence without auto-breaking streak`() {
        val yesterday = DateTimeUtils.getYesterdayStartOfDay()
        val today = DateTimeUtils.getTodayStartOfDay()

        val result = HabitStreakCalculator.calculate(
            createdAt = yesterday,
            isEveryDay = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            completionTimestamps = emptySet(),
            brokenTimestamps = emptySet(),
            currentDayTimestamp = today
        )
        assertEquals(listOf(yesterday), result.pendingMissedOccurrences)
        assertEquals(yesterday, result.earliestPendingMissedDate)
    }

    @Test
    fun `test case 4 - user says yes completed it records completion for actual missed date`() {
        runBlocking {
            val yesterday = DateTimeUtils.getYesterdayStartOfDay()

            val habit = Habit(
                id = "habit-c4",
                name = "القراءة",
                createdAt = yesterday,
                isEveryDay = true
            )
            roomHabitRepo.addHabit(habit)

            var habits = roomHabitRepo.getHabits().first()
            assertEquals(yesterday, habits.first().pendingMissedDate)

            roomHabitRepo.completeMissedOccurrence(habit.id, yesterday)

            val completions = roomHabitRepo.getHabitCompletions(habit.id).first()
            assertEquals(1, completions.size)
            assertEquals(yesterday, completions.first().dateTimestamp)

            habits = roomHabitRepo.getHabits().first()
            assertNull(habits.first().pendingMissedDate)
            assertEquals(1, habits.first().currentStreak)
        }
    }

    @Test
    fun `test case 5 - user says break the streak breaks streak and does not add completion`() {
        runBlocking {
            val yesterday = DateTimeUtils.getYesterdayStartOfDay()
            val twoDaysAgo = DateTimeUtils.getRelativeDayStart(-2)

            val habit = Habit(
                id = "habit-c5",
                name = "رياضة",
                createdAt = twoDaysAgo,
                isEveryDay = true
            )
            roomHabitRepo.addHabit(habit)
            roomHabitRepo.toggleHabitCheckIn(habit.id, twoDaysAgo)

            var habits = roomHabitRepo.getHabits().first()
            assertEquals(yesterday, habits.first().pendingMissedDate)

            roomHabitRepo.breakMissedOccurrence(habit.id, yesterday)

            val completions = roomHabitRepo.getHabitCompletions(habit.id).first()
            assertEquals(1, completions.size)
            assertEquals(twoDaysAgo, completions.first().dateTimestamp)

            habits = roomHabitRepo.getHabits().first()
            assertNull(habits.first().pendingMissedDate)
            assertEquals(0, habits.first().currentStreak)
        }
    }

    @Test
    fun `test case 6 - habit created on sep 27 never asks about sep 26`() {
        val sep27 = DateTimeUtils.getYesterdayStartOfDay()
        val sep28 = DateTimeUtils.getTodayStartOfDay()
        val sep26 = DateTimeUtils.getRelativeDayStart(-2)

        val result = HabitStreakCalculator.calculate(
            createdAt = sep27,
            isEveryDay = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            completionTimestamps = emptySet(),
            currentDayTimestamp = sep28
        )

        assertEquals(listOf(sep27), result.pendingMissedOccurrences)
        assertFalse(result.pendingMissedOccurrences.contains(sep26))
    }

    @Test
    fun `test case 7 - weekly habit on mon wed fri shows missed monday but not tuesday`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        val tuesday = DateTimeUtils.getStartOfDay(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val monday = DateTimeUtils.getStartOfDay(cal.timeInMillis)

        val monWedFri = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)

        val result = HabitStreakCalculator.calculate(
            createdAt = monday,
            isEveryDay = false,
            daysOfWeek = monWedFri,
            completionTimestamps = emptySet(),
            currentDayTimestamp = tuesday
        )

        assertEquals(listOf(monday), result.pendingMissedOccurrences)
        assertFalse(result.pendingMissedOccurrences.contains(tuesday))
    }

    @Test
    fun `multiple missed days are handled in chronological order`() {
        val threeDaysAgo = DateTimeUtils.getRelativeDayStart(-3)
        val twoDaysAgo = DateTimeUtils.getRelativeDayStart(-2)
        val yesterday = DateTimeUtils.getYesterdayStartOfDay()
        val today = DateTimeUtils.getTodayStartOfDay()

        val result = HabitStreakCalculator.calculate(
            createdAt = threeDaysAgo,
            isEveryDay = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            completionTimestamps = emptySet(),
            currentDayTimestamp = today
        )

        assertEquals(3, result.pendingMissedOccurrences.size)
        assertEquals(listOf(threeDaysAgo, twoDaysAgo, yesterday), result.pendingMissedOccurrences)
        assertEquals(threeDaysAgo, result.earliestPendingMissedDate)
    }

    @Test
    fun `data repository Room persistence CRUD, favorites, move, and password protection`() {
        runBlocking {
            // 1. Initial state: completely empty
            val initialFolders = roomDataRepo.getFolders().first()
            val initialCards = roomDataRepo.getTextCards().first()
            assertEquals(0, initialFolders.size)
            assertEquals(0, initialCards.size)

            // 2. Create folder
            val folder1 = Folder(
                id = "f-1",
                name = "ملاحظات العمل",
                description = "ملفات وملاحظات هامة"
            )
            roomDataRepo.addFolder(folder1)

            val foldersAfterInsert = roomDataRepo.getFolders().first()
            assertEquals(1, foldersAfterInsert.size)
            assertEquals("ملاحظات العمل", foldersAfterInsert.first().name)

            // 3. Rename folder
            roomDataRepo.renameFolder("f-1", "ملاحظات العمل الرسمية")
            val foldersAfterRename = roomDataRepo.getFolders().first()
            assertEquals("ملاحظات العمل الرسمية", foldersAfterRename.first().name)

            // 4. Create card inside folder
            val card1 = TextCard(
                id = "c-1",
                folderId = "f-1",
                title = "خطة المشروع 2026",
                content = "تفاصيل الخطة الاستراتيجية والأهداف الربع سنوية بالتفصيل.",
                isFavorite = true,
                isPasswordProtected = true,
                passwordHash = PasswordUtils.hashPassword("secret123"),
                tags = listOf("عمل", "استراتيجية")
            )
            roomDataRepo.addTextCard(card1)

            // 5. Create unfiled card
            val card2 = TextCard(
                id = "c-2",
                folderId = null,
                title = "فكرة سريعة",
                content = "محتوى فكرة عادية غير مقيدة بمجلد.",
                isFavorite = false
            )
            roomDataRepo.addTextCard(card2)

            val cardsAfterInsert = roomDataRepo.getTextCards().first()
            assertEquals(2, cardsAfterInsert.size)

            val retrievedCard1 = cardsAfterInsert.first { it.id == "c-1" }
            assertEquals("خطة المشروع 2026", retrievedCard1.title)
            assertEquals("f-1", retrievedCard1.folderId)
            assertTrue(retrievedCard1.isFavorite)
            assertTrue(retrievedCard1.isPasswordProtected)
            assertTrue(PasswordUtils.verifyPassword("secret123", retrievedCard1.passwordHash))
            assertFalse(PasswordUtils.verifyPassword("wrongpass", retrievedCard1.passwordHash))

            val retrievedCard2 = cardsAfterInsert.first { it.id == "c-2" }
            assertNull(retrievedCard2.folderId)
            assertFalse(retrievedCard2.isFavorite)

            // 6. Edit card content and title
            val editedCard2 = retrievedCard2.copy(
                title = "فكرة سريعة معدلة",
                content = "تم تحديث محتوى الفكرة السريعة."
            )
            roomDataRepo.updateTextCard(editedCard2)

            val cardsAfterEdit = roomDataRepo.getTextCards().first()
            assertEquals("فكرة سريعة معدلة", cardsAfterEdit.first { it.id == "c-2" }.title)
            assertEquals("تم تحديث محتوى الفكرة السريعة.", cardsAfterEdit.first { it.id == "c-2" }.content)

            // 7. Toggle favorite
            roomDataRepo.toggleFavorite("c-2")
            val cardsAfterFav = roomDataRepo.getTextCards().first()
            assertTrue(cardsAfterFav.first { it.id == "c-2" }.isFavorite)

            // 8. Move card to another folder
            roomDataRepo.moveCardToFolder("c-2", "f-1")
            val cardsAfterMove = roomDataRepo.getTextCards().first()
            assertEquals("f-1", cardsAfterMove.first { it.id == "c-2" }.folderId)

            // 9. Delete single card
            roomDataRepo.deleteTextCard("c-2")
            val cardsAfterDeleteCard = roomDataRepo.getTextCards().first()
            assertEquals(1, cardsAfterDeleteCard.size)
            assertEquals("c-1", cardsAfterDeleteCard.first().id)

            // 10. Delete folder (should delete cards inside folder as well)
            roomDataRepo.deleteFolder("f-1")
            val foldersAfterDelete = roomDataRepo.getFolders().first()
            val cardsAfterDeleteFolder = roomDataRepo.getTextCards().first()
            assertEquals(0, foldersAfterDelete.size)
            assertEquals(0, cardsAfterDeleteFolder.size)
        }
    }

    @Test
    fun `data persists across database close and reopen simulating app restart`() {
        runBlocking {
            val dbFile = File(context.cacheDir, "test_restart_life_manager.db")
            if (dbFile.exists()) dbFile.delete()

            // Session 1: App is running
            val session1Db = Room.databaseBuilder(context, LifeManagerDatabase::class.java, dbFile.absolutePath)
                .addMigrations(
                    LifeManagerDatabase.MIGRATION_1_2,
                    LifeManagerDatabase.MIGRATION_2_3,
                    LifeManagerDatabase.MIGRATION_3_4,
                    LifeManagerDatabase.MIGRATION_4_5,
                    LifeManagerDatabase.MIGRATION_5_6,
                    LifeManagerDatabase.MIGRATION_6_7
                )
                .build()
            val session1Repo = RoomDataRepository(session1Db.folderDao(), session1Db.textCardDao(), session1Db.syncTombstoneDao())

            val folder = Folder(
                id = "f-restart-1",
                name = "المجلد الدائم",
                description = "اختبار الإغلاق وإعادة الفتح"
            )
            session1Repo.addFolder(folder)

            val cardInFolder = TextCard(
                id = "c-restart-1",
                folderId = "f-restart-1",
                title = "بطاقة داخل المجلد",
                content = "هذا النص يجب أن يبقى محفوظاً بعد إغلاق التطبيق تماماً.",
                isFavorite = true,
                isPasswordProtected = true,
                passwordHash = PasswordUtils.hashPassword("P@ssword123")
            )
            session1Repo.addTextCard(cardInFolder)

            val unfiledCard = TextCard(
                id = "c-restart-2",
                folderId = null,
                title = "بطاقة عامة بدون مجلد",
                content = "ملاحظة عامة للتأكد من بقاء البطاقات الحرة.",
                isFavorite = false
            )
            session1Repo.addTextCard(unfiledCard)

            assertEquals(1, session1Repo.getFolders().first().size)
            assertEquals(2, session1Repo.getTextCards().first().size)

            // CLOSE APP COMPLETELY
            session1Db.close()

            // Session 2: App reopened from same database file
            val session2Db = Room.databaseBuilder(context, LifeManagerDatabase::class.java, dbFile.absolutePath)
                .addMigrations(
                    LifeManagerDatabase.MIGRATION_1_2,
                    LifeManagerDatabase.MIGRATION_2_3,
                    LifeManagerDatabase.MIGRATION_3_4,
                    LifeManagerDatabase.MIGRATION_4_5,
                    LifeManagerDatabase.MIGRATION_5_6,
                    LifeManagerDatabase.MIGRATION_6_7
                )
                .build()
            val session2Repo = RoomDataRepository(session2Db.folderDao(), session2Db.textCardDao(), session2Db.syncTombstoneDao())

            val restoredFolders = session2Repo.getFolders().first()
            val restoredCards = session2Repo.getTextCards().first()

            assertEquals(1, restoredFolders.size)
            assertEquals("المجلد الدائم", restoredFolders.first().name)
            assertEquals("f-restart-1", restoredFolders.first().id)

            assertEquals(2, restoredCards.size)
            val card1 = restoredCards.first { it.id == "c-restart-1" }
            assertEquals("بطاقة داخل المجلد", card1.title)
            assertEquals("هذا النص يجب أن يبقى محفوظاً بعد إغلاق التطبيق تماماً.", card1.content)
            assertEquals("f-restart-1", card1.folderId)
            assertTrue(card1.isFavorite)
            assertTrue(card1.isPasswordProtected)
            assertTrue(PasswordUtils.verifyPassword("P@ssword123", card1.passwordHash))

            val card2 = restoredCards.first { it.id == "c-restart-2" }
            assertEquals("بطاقة عامة بدون مجلد", card2.title)
            assertEquals("ملاحظة عامة للتأكد من بقاء البطاقات الحرة.", card2.content)
            assertNull(card2.folderId)
            assertFalse(card2.isFavorite)

            session2Db.close()
            dbFile.delete()
        }
    }

    @Test
    fun `survey full workflow - create with 8 questions, answer occurrence day 1, edit, copy to day 2, and delete`() {
        runBlocking {
            // 1. Survey repository starts completely empty
            val initialSurveys = roomSurveyRepo.getSurveys().first()
            assertEquals(0, initialSurveys.size)

            // 2. Create survey with 8 questions manually, frequency Daily
            val surveyId = "survey-8q"
            val questions = (1..8).map { i ->
                SurveyQuestion(
                    id = "q-$i",
                    surveyId = surveyId,
                    questionOrder = i - 1,
                    questionText = "ما هو مستوى تقييمك للسؤال رقم $i اليوم؟"
                )
            }
            val survey = Survey(
                id = surveyId,
                name = "التقييم اليومي الشامل",
                frequency = SurveyFrequency.DAILY,
                questions = questions
            )
            roomSurveyRepo.addSurvey(survey, questions)

            val surveysAfterAdd = roomSurveyRepo.getSurveys().first()
            assertEquals(1, surveysAfterAdd.size)
            val savedSurvey = surveysAfterAdd.first()
            assertEquals("التقييم اليومي الشامل", savedSurvey.name)
            assertEquals(SurveyFrequency.DAILY, savedSurvey.frequency)
            assertEquals(8, savedSurvey.questions.size)

            // 3. Survey starts with zero completed occurrences
            val initialResponses = roomSurveyRepo.getResponsesForSurvey(surveyId).first()
            assertEquals(0, initialResponses.size)

            // 4. User answers all 8 questions and saves occurrence
            val manualAnswers = (1..8).associate { i ->
                "q-$i" to "إجابة يدوية واقعية على السؤال $i"
            }
            val firstResponse = roomSurveyRepo.saveNewResponse(surveyId, manualAnswers)

            assertEquals(1, firstResponse.occurrenceNumber)
            assertEquals("Survey day 1", firstResponse.displayTitle(false))
            assertEquals("استبيان يوم 1", firstResponse.displayTitle(true))
            assertEquals(8, firstResponse.answers.size)

            val responsesAfterFirst = roomSurveyRepo.getResponsesForSurvey(surveyId).first()
            assertEquals(1, responsesAfterFirst.size)
            assertEquals(1, responsesAfterFirst.first().occurrenceNumber)

            // 5. Edit Survey day 1 answers -> preserves occurrence number 1
            val updatedAnswers = manualAnswers.toMutableMap().apply {
                put("q-1", "إجابة معدلة للسؤال الأول")
            }
            roomSurveyRepo.updateResponse(firstResponse.id, updatedAnswers)

            val responsesAfterEdit = roomSurveyRepo.getResponsesForSurvey(surveyId).first()
            assertEquals(1, responsesAfterEdit.size)
            val edited = responsesAfterEdit.first()
            assertEquals(1, edited.occurrenceNumber)
            assertEquals("Survey day 1", edited.displayTitle(false))
            val q1Ans = edited.answers.find { it.questionId == "q-1" }?.answerText
            assertEquals("إجابة معدلة للسؤال الأول", q1Ans)

            // 6. Copy Survey day 1 -> creates Survey day 2 with same answers
            val copiedResponse = roomSurveyRepo.copyResponse(firstResponse.id)
            assertEquals(2, copiedResponse.occurrenceNumber)
            assertEquals("Survey day 2", copiedResponse.displayTitle(false))

            val responsesAfterCopy = roomSurveyRepo.getResponsesForSurvey(surveyId).first()
            assertEquals(2, responsesAfterCopy.size)
            assertEquals(1, responsesAfterCopy[0].occurrenceNumber)
            assertEquals(2, responsesAfterCopy[1].occurrenceNumber)

            // 7. Delete Survey day 1 -> Survey day 2 remains and does NOT renumber to day 1
            roomSurveyRepo.deleteResponse(firstResponse.id)
            val responsesAfterDelete = roomSurveyRepo.getResponsesForSurvey(surveyId).first()
            assertEquals(1, responsesAfterDelete.size)
            assertEquals(2, responsesAfterDelete.first().occurrenceNumber)
            assertEquals("Survey day 2", responsesAfterDelete.first().displayTitle(false))
        }
    }

    @Test
    fun `survey and responses persist across complete application restart`() {
        runBlocking {
            val dbFile = File(context.cacheDir, "test_survey_restart.db")
            if (dbFile.exists()) dbFile.delete()

            // Session 1: Create survey with 8 questions and complete Survey day 1
            val session1Db = Room.databaseBuilder(context, LifeManagerDatabase::class.java, dbFile.absolutePath)
                .addMigrations(
                    LifeManagerDatabase.MIGRATION_1_2,
                    LifeManagerDatabase.MIGRATION_2_3,
                    LifeManagerDatabase.MIGRATION_3_4,
                    LifeManagerDatabase.MIGRATION_4_5,
                    LifeManagerDatabase.MIGRATION_5_6,
                    LifeManagerDatabase.MIGRATION_6_7
                )
                .build()
            val session1Repo = RoomSurveyRepository(session1Db.surveyDao(), session1Db.syncTombstoneDao())

            val surveyId = "s-persistent-1"
            val questions = (1..8).map { i ->
                SurveyQuestion(
                    id = "q-persist-$i",
                    surveyId = surveyId,
                    questionOrder = i - 1,
                    questionText = "سؤال الاختبار رقم $i"
                )
            }
            val survey = Survey(
                id = surveyId,
                name = "استبيان المتابعة المستمرة",
                frequency = SurveyFrequency.DAILY,
                questions = questions
            )
            session1Repo.addSurvey(survey, questions)

            val answers = (1..8).associate { i -> "q-persist-$i" to "نص الإجابة رقم $i" }
            val resp1 = session1Repo.saveNewResponse(surveyId, answers)
            assertEquals(1, resp1.occurrenceNumber)

            // Verify in session 1
            assertEquals(1, session1Repo.getSurveys().first().size)
            assertEquals(1, session1Repo.getResponsesForSurvey(surveyId).first().size)

            // CLOSE APP COMPLETELY
            session1Db.close()

            // Session 2: Reopen app from same database file
            val session2Db = Room.databaseBuilder(context, LifeManagerDatabase::class.java, dbFile.absolutePath)
                .addMigrations(
                    LifeManagerDatabase.MIGRATION_1_2,
                    LifeManagerDatabase.MIGRATION_2_3,
                    LifeManagerDatabase.MIGRATION_3_4,
                    LifeManagerDatabase.MIGRATION_4_5,
                    LifeManagerDatabase.MIGRATION_5_6,
                    LifeManagerDatabase.MIGRATION_6_7
                )
                .build()
            val session2Repo = RoomSurveyRepository(session2Db.surveyDao(), session2Db.syncTombstoneDao())

            val restoredSurveys = session2Repo.getSurveys().first()
            assertEquals(1, restoredSurveys.size)
            val restoredSurvey = restoredSurveys.first()
            assertEquals("استبيان المتابعة المستمرة", restoredSurvey.name)
            assertEquals(8, restoredSurvey.questions.size)

            val restoredResponses = session2Repo.getResponsesForSurvey(surveyId).first()
            assertEquals(1, restoredResponses.size)
            val restoredResp1 = restoredResponses.first()
            assertEquals(1, restoredResp1.occurrenceNumber)
            assertEquals("Survey day 1", restoredResp1.displayTitle(false))
            assertEquals("استبيان يوم 1", restoredResp1.displayTitle(true))
            assertEquals(8, restoredResp1.answers.size)
            assertEquals("نص الإجابة رقم 1", restoredResp1.answers.find { it.questionId == "q-persist-1" }?.answerText)

            session2Db.close()
            dbFile.delete()
        }
    }

    @Test
    fun `firebase configuration helper returns true when google-services json is configured`() {
        assertTrue(FirebaseConfigHelper.isFirebaseConfigured(context))
        assertNotNull(FirebaseConfigHelper.getFirestore(context))
    }

    @Test
    fun `sync preferences generates and persists stable single-user installation identity`() {
        val prefs1 = SyncPreferences(context)
        val id1 = prefs1.installationId
        assertTrue(id1.isNotBlank())
        assertTrue(id1.startsWith("usr_"))

        // Reopening preferences retains the identical installation ID
        val prefs2 = SyncPreferences(context)
        assertEquals(id1, prefs2.installationId)
    }

    @Test
    fun `firestore sync manager handles offline network gracefully without throwing exception`() {
        runBlocking {
            val prefs = SyncPreferences(context)
            val syncManager = FirestoreSyncManager(context, db, prefs)

            val result = syncManager.sync()
            assertEquals(SyncResult.Offline, result)
            assertEquals(SyncState.OFFLINE, prefs.syncInfo.value.state)
            assertTrue(prefs.syncInfo.value.isConfigured)
        }
    }

    @Test
    fun `deleting tasks, folders, cards, and surveys records tombstones in room database`() {
        runBlocking {
            val initialTombstones = db.syncTombstoneDao().getAllTombstones()
            assertEquals(0, initialTombstones.size)

            // 1. Task tombstone
            val task = Task(
                id = "task-sync-1",
                title = "مهمة المزامنة",
                dueDate = DateTimeUtils.getTodayStartOfDay()
            )
            roomTaskRepo.addTask(task)
            roomTaskRepo.deleteTask("task-sync-1")

            // 2. Folder and card tombstone
            val folder = Folder(id = "folder-sync-1", name = "مجلد مزامنة")
            roomDataRepo.addFolder(folder)
            val card = TextCard(id = "card-sync-1", title = "بطاقة", content = "نص")
            roomDataRepo.addTextCard(card)

            roomDataRepo.deleteTextCard("card-sync-1")
            roomDataRepo.deleteFolder("folder-sync-1")

            // 3. Survey tombstone
            val survey = Survey(id = "survey-sync-1", name = "استبيان مزامنة", questions = emptyList())
            roomSurveyRepo.addSurvey(survey, emptyList())
            roomSurveyRepo.deleteSurvey("survey-sync-1")

            val tombstones = db.syncTombstoneDao().getAllTombstones()
            assertTrue(tombstones.any { it.entityType == "TASK" && it.entityId == "task-sync-1" })
            assertTrue(tombstones.any { it.entityType == "TEXT_CARD" && it.entityId == "card-sync-1" })
            assertTrue(tombstones.any { it.entityType == "FOLDER" && it.entityId == "folder-sync-1" })
            assertTrue(tombstones.any { it.entityType == "SURVEY" && it.entityId == "survey-sync-1" })
        }
    }
}
