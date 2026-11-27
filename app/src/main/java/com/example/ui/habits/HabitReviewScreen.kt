package com.example.ui.habits

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppContainer
import com.example.model.Habit
import com.example.model.HabitTimeGroup
import com.example.model.habitTimeGroup
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/** One pending review: which window, which calendar day, and the habits still open in it. */
data class HabitReviewRequest(
    val group: HabitTimeGroup,
    val dateTimestamp: Long,
    val habits: List<Habit>
) {
    val key: String get() = "review_${group.name}_$dateTimestamp"
}

private const val PREFS = "habit_review_prefs"

/**
 * Morning window ends at 16:00, so from 16:00 on the screen asks about today's morning habits.
 * The evening window ends at midnight, so from 00:00 until 16:00 it asks about yesterday's
 * evening habits. Each review is asked until it is confirmed once.
 */
suspend fun computeHabitReview(context: Context, nowMillis: Long): HabitReviewRequest? {
    val repo = AppContainer.habitRepository
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    val todayStart = DateTimeUtils.getStartOfDay(nowMillis)
    val (group, dayStart) = if (hour >= 16) {
        HabitTimeGroup.MORNING to todayStart
    } else {
        HabitTimeGroup.AFTERNOON_EVENING to Calendar.getInstance().apply {
            timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean("review_${group.name}_$dayStart", false)) return null

    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK)
    val dayEnd = Calendar.getInstance().apply {
        timeInMillis = dayStart; add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val open = repo.getHabits().first().filter { habit ->
        habitTimeGroup(habit.scheduledTime) == group &&
            habit.createdAt < dayEnd &&
            (habit.isEveryDay || dayOfWeek in habit.daysOfWeek) &&
            repo.getHabitCompletions(habit.id).first().none {
                DateTimeUtils.getStartOfDay(it.dateTimestamp) == dayStart
            }
    }
    if (open.isEmpty()) {
        prefs.edit().putBoolean("review_${group.name}_$dayStart", true).apply()
        return null
    }
    return HabitReviewRequest(group, dayStart, open)
}

@Composable
fun HabitReviewHost(isArabic: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var request by remember { mutableStateOf<HabitReviewRequest?>(null) }
    var snoozedKey by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(tick) {
        if (request == null) {
            val next = computeHabitReview(context, System.currentTimeMillis())
            if (next != null && next.key != snoozedKey) request = next
        }
    }

    val current = request ?: return
    val isMorning = current.group == HabitTimeGroup.MORNING
    val answers = remember(current.key) { mutableStateMapOf<String, Boolean>() }

    Dialog(
        onDismissRequest = { snoozedKey = current.key; request = null },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("habit_review_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)
            ) {
                Text(
                    text = if (isMorning) {
                        if (isArabic) "هذه مهام الصباح" else "These are your morning habits"
                    } else {
                        if (isArabic) "هذه مهام المساء" else "These are your evening habits"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isArabic) {
                        if (isMorning) "حدّد ما أنجزته مما فاتك وما لم تنجزه." else "حدّد ما أنجزته من عادات أمس المسائية وما لم تنجزه."
                    } else {
                        if (isMorning) "Mark which ones you did and which you did not."
                        else "Mark which of yesterday's evening habits you did and which you did not."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(current.habits, key = { it.id }) { habit ->
                        val done = answers[habit.id]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(habit.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    habit.scheduledTime?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Button(
                                    onClick = { answers[habit.id] = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(34.dp).testTag("review_done_${habit.id}"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    colors = if (done == true) androidx.compose.material3.ButtonDefaults.buttonColors()
                                    else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                                ) {
                                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text(if (isArabic) "أنجزتها" else "Done")
                                }
                                Spacer(Modifier.size(6.dp))
                                OutlinedButton(
                                    onClick = { answers[habit.id] = false },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(34.dp).testTag("review_missed_${habit.id}"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    border = BorderStroke(
                                        if (done == false) 2.dp else 1.dp,
                                        if (done == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text(if (isArabic) "لم أنجز" else "Not done")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { snoozedKey = current.key; request = null },
                        modifier = Modifier.weight(1f).testTag("review_later")
                    ) { Text(if (isArabic) "لاحقًا" else "Later") }
                    Button(
                        onClick = {
                            val repo = AppContainer.habitRepository
                            val review = current
                            scope.launch {
                                review.habits.forEach { habit ->
                                    when (answers[habit.id]) {
                                        true -> repo.completeMissedOccurrence(habit.id, review.dateTimestamp)
                                        false -> repo.breakMissedOccurrence(habit.id, review.dateTimestamp)
                                        null -> Unit // left unanswered: stays open for the normal prompt
                                    }
                                }
                                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                    .edit().putBoolean(review.key, true).apply()
                            }
                            request = null
                        },
                        enabled = answers.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f).testTag("review_confirm")
                    ) { Text(if (isArabic) "تأكيد" else "Confirm") }
                }
            }
        }
    }
}
