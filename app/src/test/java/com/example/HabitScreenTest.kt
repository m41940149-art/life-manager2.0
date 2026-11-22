package com.example

import android.app.Application
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import org.robolectric.shadows.ShadowDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.HabitRepository
import com.example.model.Habit
import java.util.Calendar
import com.example.model.HabitCompletion
import com.example.ui.habits.HabitsScreen
import com.example.ui.habits.HabitsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HabitScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var contentView: View
    private var nowMillis: Long = 0

    private class FakeRepo : HabitRepository {
        val records = MutableStateFlow(listOf(
            Habit(id = "morning", name = "Read", scheduledTime = "15:59"),
            Habit(id = "evening", name = "Walk", scheduledTime = "16:00"),
            Habit(id = "legacy", name = "Stretch"),
            Habit(id = "offday", name = "Weekend habit", scheduledTime = "08:00", isScheduledToday = false)
        ))
        override fun getHabits() = records
        override fun getHabitCompletions(habitId: String) = flowOf(emptyList<HabitCompletion>())
        override suspend fun addHabit(habit: Habit) { records.value += habit }
        override suspend fun updateHabit(habit: Habit) { records.value = records.value.map { if (it.id == habit.id) habit else it } }
        override suspend fun deleteHabit(habitId: String) { records.value = records.value.filterNot { it.id == habitId } }
        override suspend fun reorderHabits(orderedHabitIds: List<String>) {}
        override suspend fun toggleHabitCheckIn(habitId: String, dateTimestamp: Long) {}
        override suspend fun completeMissedOccurrence(habitId: String, missedDateTimestamp: Long) {}
        override suspend fun breakMissedOccurrence(habitId: String, missedDateTimestamp: Long) {}
    }

    private fun show(arabic: Boolean = false, allPeriods: Boolean = true): FakeRepo {
        val time = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 15); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 0) }
        nowMillis = time.timeInMillis
        val repo = FakeRepo()
        compose.setContent {
            contentView = LocalView.current
            CompositionLocalProvider(LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme { HabitsScreen(HabitsViewModel(repo), arabic, nowMillis = { nowMillis }) }
            }
        }
        compose.waitUntil { compose.onAllNodesWithTag("habit_card_morning").fetchSemanticsNodes().isNotEmpty() }
        if (allPeriods) compose.onNodeWithTag("all_periods_filter").performClick()
        return repo
    }

    private fun screenshot(name: String) {
        val target = File("/tmp/habit-visuals/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }
            val view = dialog?.window?.decorView ?: contentView.rootView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `automatic filtering changes at 4 PM and can show all`() {
        show(allPeriods = false)
        compose.onNodeWithTag("habit_card_morning").assertExists()
        compose.onNodeWithTag("habit_card_evening").assertDoesNotExist()
        screenshot("automatic-morning")
        nowMillis += 60_000
        compose.mainClock.advanceTimeBy(60_001)
        compose.waitForIdle()
        compose.onNodeWithTag("habit_card_morning").assertDoesNotExist()
        compose.onNodeWithTag("habit_card_evening").assertExists()
        compose.onNodeWithTag("habit_card_legacy").assertExists()
        screenshot("automatic-evening")
        compose.onNodeWithTag("all_periods_filter").performClick()
        compose.onNodeWithTag("habit_card_morning").assertExists()
    }

    @Test fun `groups respect the boundary and legacy habits remain visible`() {
        show()
        compose.onNodeWithTag("habit_group_MORNING").assertExists()
        compose.onNodeWithTag("habit_group_AFTERNOON_EVENING").assertExists()
        screenshot("groups-english")
        compose.onNodeWithTag("habit_group_UNSCHEDULED").performScrollTo().assertExists()
        compose.onNodeWithTag("all_habits_filter").performClick()
        compose.onNodeWithText("Weekend habit").assertExists()
    }

    @Test fun `edit preloads and saves the same habit`() {
        val repo = show()
        compose.onNodeWithTag("habit_menu_morning").performClick()
        compose.onNodeWithTag("edit_habit_morning").performClick()
        compose.onNodeWithTag("habit_name_input").assertTextContains("Read")
        compose.onNodeWithTag("habit_time_picker").assertTextContains("15:59")
        screenshot("edit-english")
        compose.onNodeWithTag("habit_time_picker").performClick()
        compose.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as TimePickerDialog
            picker.updateTime(16, 0)
            picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        compose.onNodeWithTag("habit_time_picker").assertTextContains("16:00")
        compose.onNodeWithTag("habit_name_input").performTextReplacement("Read a chapter")
        compose.onNodeWithTag("submit_add_habit_btn").performScrollTo().performClick()
        compose.waitUntil { repo.records.value.first().name == "Read a chapter" }
        assertEquals("morning", repo.records.value.first().id)
        assertEquals("16:00", repo.records.value.first().scheduledTime)
        assertEquals(4, repo.records.value.size)
    }

    @Test fun `Arabic right to left grouping and editor`() {
        show(true)
        screenshot("groups-arabic")
        compose.onNodeWithTag("habit_menu_morning").performClick()
        compose.onNodeWithTag("edit_habit_morning").performClick()
        compose.onNodeWithText("تعديل العادة").assertExists()
        screenshot("edit-arabic")
    }
}
