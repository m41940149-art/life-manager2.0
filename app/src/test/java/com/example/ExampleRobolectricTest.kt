package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InMemoryDataRepository
import com.example.data.InMemorySurveyRepository
import com.example.data.RoomHabitRepository
import com.example.data.RoomTaskRepository
import com.example.data.local.LifeManagerDatabase
import com.example.model.Habit
import com.example.model.QuestionType
import com.example.model.Survey
import com.example.model.SurveyFrequency
import com.example.model.SurveyQuestion
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.util.DateTimeUtils
import com.example.util.HabitStreakCalculator
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
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: LifeManagerDatabase
    private lateinit var roomTaskRepo: RoomTaskRepository
    private lateinit var roomHabitRepo: RoomHabitRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = LifeManagerDatabase.getInMemoryDatabase(context)
        roomTaskRepo = RoomTaskRepository(db.taskDao())
        roomHabitRepo = RoomHabitRepository(db.habitDao(), context)
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
    fun `database starts with zero user data`() = runBlocking {
        val tasks = roomTaskRepo.getTasks().first()
        val habits = roomHabitRepo.getHabits().first()
        assertEquals(0, tasks.size)
        assertEquals(0, habits.size)
    }

    @Test
    fun `room database persistence operations for tasks work reactively`() = runBlocking {
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
        // Must have NO missed habit
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
        // No missed habit for yesterday because it was completed
        assertTrue(result.pendingMissedOccurrences.isEmpty())
        assertNull(result.earliestPendingMissedDate)
        // Streak continues from yesterday (1)
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
        // Shows missed occurrence for yesterday
        assertEquals(listOf(yesterday), result.pendingMissedOccurrences)
        assertEquals(yesterday, result.earliestPendingMissedDate)
    }

    @Test
    fun `test case 4 - user says yes completed it records completion for actual missed date`() = runBlocking {
        val yesterday = DateTimeUtils.getYesterdayStartOfDay()
        val today = DateTimeUtils.getTodayStartOfDay()

        val habit = Habit(
            id = "habit-c4",
            name = "القراءة",
            createdAt = yesterday,
            isEveryDay = true
        )
        roomHabitRepo.addHabit(habit)

        // Verify missed occurrence is shown for yesterday
        var habits = roomHabitRepo.getHabits().first()
        assertEquals(yesterday, habits.first().pendingMissedDate)

        // User says "Yes, I completed it"
        roomHabitRepo.completeMissedOccurrence(habit.id, yesterday)

        // Verify completion record is for yesterday (NOT today)
        val completions = roomHabitRepo.getHabitCompletions(habit.id).first()
        assertEquals(1, completions.size)
        assertEquals(yesterday, completions.first().dateTimestamp)

        // Missed prompt is now removed and streak is 1
        habits = roomHabitRepo.getHabits().first()
        assertNull(habits.first().pendingMissedDate)
        assertEquals(1, habits.first().currentStreak)
    }

    @Test
    fun `test case 5 - user says break the streak breaks streak and does not add completion`() = runBlocking {
        val yesterday = DateTimeUtils.getYesterdayStartOfDay()
        val twoDaysAgo = DateTimeUtils.getRelativeDayStart(-2)
        val today = DateTimeUtils.getTodayStartOfDay()

        val habit = Habit(
            id = "habit-c5",
            name = "رياضة",
            createdAt = twoDaysAgo,
            isEveryDay = true
        )
        roomHabitRepo.addHabit(habit)
        // User completed 2 days ago, but missed yesterday
        roomHabitRepo.toggleHabitCheckIn(habit.id, twoDaysAgo)

        var habits = roomHabitRepo.getHabits().first()
        assertEquals(yesterday, habits.first().pendingMissedDate)

        // User says "Break the streak"
        roomHabitRepo.breakMissedOccurrence(habit.id, yesterday)

        // Verify NO completion record added for yesterday
        val completions = roomHabitRepo.getHabitCompletions(habit.id).first()
        assertEquals(1, completions.size)
        assertEquals(twoDaysAgo, completions.first().dateTimestamp)

        // Prompt removed and streak is broken (0)
        habits = roomHabitRepo.getHabits().first()
        assertNull(habits.first().pendingMissedDate)
        assertEquals(0, habits.first().currentStreak)
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

        // Must ONLY have sep 27 as missed, NEVER sep 26
        assertEquals(listOf(sep27), result.pendingMissedOccurrences)
        assertFalse(result.pendingMissedOccurrences.contains(sep26))
    }

    @Test
    fun `test case 7 - weekly habit on mon wed fri shows missed monday but not tuesday`() {
        // Find a recent Monday and Tuesday
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

        // Monday is missed
        assertEquals(listOf(monday), result.pendingMissedOccurrences)
        // Tuesday is NOT missed (it is today and not scheduled anyway)
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

        // 3 missed days: 3 days ago, 2 days ago, yesterday
        assertEquals(3, result.pendingMissedOccurrences.size)
        assertEquals(listOf(threeDaysAgo, twoDaysAgo, yesterday), result.pendingMissedOccurrences)
        // Earliest date must be threeDaysAgo (chronological order)
        assertEquals(threeDaysAgo, result.earliestPendingMissedDate)
    }

    @Test
    fun `data repository and survey repository start empty`() = runBlocking {
        val dataRepo = InMemoryDataRepository()
        val surveyRepo = InMemorySurveyRepository()

        assertEquals(0, dataRepo.getFolders().first().size)
        assertEquals(0, dataRepo.getTextCards().first().size)
        assertEquals(0, surveyRepo.getSurveys().first().size)
    }
}
