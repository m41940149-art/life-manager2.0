package com.example.ui.timer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TimerSessionEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The kinds of sessions the user can pick when starting the timer. */
enum class SessionType(
    val titleAr: String,
    val titleEn: String,
    val icon: ImageVector,
    val tag: String
) {
    STUDY("دراسة", "Study", Icons.Filled.MenuBook, "session_study"),
    LEARNING("تعلم", "Learning", Icons.Filled.School, "session_learning"),
    WORK("عمل", "Work", Icons.Filled.Work, "session_work")
}

@Composable
fun TimerScreen(
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showTypeForm by rememberSaveable { mutableStateOf(false) }
    // Name of the active session type, or null while idle.
    var activeTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var goalMinutes by rememberSaveable { mutableLongStateOf(0L) }
    var sessionStartedAt by rememberSaveable { mutableLongStateOf(0L) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    // Time accumulated before the current run segment, plus the segment's start (elapsedRealtime).
    var accumulatedMs by rememberSaveable { mutableLongStateOf(0L) }
    var segmentStartMs by rememberSaveable { mutableLongStateOf(0L) }
    var elapsedMs by rememberSaveable { mutableLongStateOf(0L) }

    val activeType = activeTypeName?.let { name -> SessionType.values().firstOrNull { it.name == name } }

    BackHandler(enabled = showHistory) { showHistory = false }

    LaunchedEffect(isRunning, segmentStartMs) {
        while (isRunning) {
            elapsedMs = accumulatedMs + (SystemClock.elapsedRealtime() - segmentStartMs)
            delay(250)
        }
    }

    fun startSession(type: SessionType, goalMin: Int) {
        activeTypeName = type.name
        goalMinutes = goalMin.toLong()
        sessionStartedAt = System.currentTimeMillis()
        accumulatedMs = 0L
        elapsedMs = 0L
        segmentStartMs = SystemClock.elapsedRealtime()
        isRunning = true
    }

    fun pause() {
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStartMs
        elapsedMs = accumulatedMs
        isRunning = false
    }

    fun resume() {
        segmentStartMs = SystemClock.elapsedRealtime()
        isRunning = true
    }

    fun finish() {
        val total = if (isRunning) {
            accumulatedMs + (SystemClock.elapsedRealtime() - segmentStartMs)
        } else accumulatedMs
        val type = activeType
        // Ignore accidental sessions shorter than one second.
        if (type != null && total >= 1000L) {
            viewModel.saveSession(type, goalMinutes.toInt(), total, sessionStartedAt)
        }
        isRunning = false
        activeTypeName = null
        accumulatedMs = 0L
        elapsedMs = 0L
    }

    if (showHistory) {
        SessionHistoryScreen(
            sessions = sessions,
            isArabic = isArabic,
            onBack = { showHistory = false },
            onDelete = { viewModel.deleteSession(it) },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "المؤقت" else "Timer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showHistory = true },
                    modifier = Modifier.testTag("timer_history_button")
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isArabic) "جلساتك السابقة" else "Past sessions")
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (activeType == null) {
                    IdleContent(isArabic = isArabic, onStart = { showTypeForm = true })
                } else {
                    RunningContent(
                        type = activeType,
                        elapsedMs = elapsedMs,
                        goalMinutes = goalMinutes.toInt(),
                        isRunning = isRunning,
                        isArabic = isArabic,
                        onPauseResume = { if (isRunning) pause() else resume() },
                        onFinish = { finish() }
                    )
                }
            }
        }
    }

    if (showTypeForm) {
        SessionTypeDialog(
            isArabic = isArabic,
            onDismiss = { showTypeForm = false },
            onConfirm = { type, goalMin ->
                showTypeForm = false
                startSession(type, goalMin)
            }
        )
    }
}

@Composable
private fun IdleContent(isArabic: Boolean, onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.testTag("timer_start_button").height(56.dp).fillMaxWidth(0.6f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (isArabic) "ابدأ" else "Start",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RunningContent(
    type: SessionType,
    elapsedMs: Long,
    goalMinutes: Int,
    isRunning: Boolean,
    isArabic: Boolean,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (isArabic) type.titleAr else type.titleEn,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = formatElapsed(elapsedMs),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("timer_display")
        )
        Spacer(Modifier.height(16.dp))
        val goalMs = goalMinutes * 60_000L
        val progress = if (goalMs > 0) (elapsedMs.toFloat() / goalMs).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = (if (isArabic) "الهدف: " else "Goal: ") + formatHours(goalMinutes, isArabic),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (goalMs > 0 && elapsedMs >= goalMs) {
            Text(
                text = if (isArabic) "🎉 تم تحقيق الهدف" else "🎉 Goal reached",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("timer_goal_reached")
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onPauseResume,
                modifier = Modifier.testTag("timer_pause_button")
            ) {
                Icon(
                    if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (isRunning) {
                        if (isArabic) "إيقاف مؤقت" else "Pause"
                    } else {
                        if (isArabic) "استئناف" else "Resume"
                    }
                )
            }
            OutlinedButton(
                onClick = onFinish,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("timer_finish_button")
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (isArabic) "إنهاء" else "Finish",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SessionTypeDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (SessionType, Int) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var goalText by rememberSaveable { mutableStateOf("1") }

    val goalHours = parseHours(goalText)
    val goalValid = goalHours != null && goalHours > 0.0 && goalHours <= 24.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "ما نوع الجلسة؟" else "What type of session?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionType.values().forEach { type ->
                    val isSelected = selected == type.name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(type.tag)
                            .clickable { selected = type.name }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(type.icon, contentDescription = null)
                            Spacer(Modifier.size(12.dp))
                            Text(
                                text = if (isArabic) type.titleAr else type.titleEn,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isArabic) "الهدف كم ساعة؟" else "Goal: how many hours?",
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.take(5) },
                    singleLine = true,
                    isError = !goalValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(if (isArabic) "ساعة" else "h") },
                    supportingText = {
                        if (!goalValid) {
                            Text(if (isArabic) "أدخل رقمًا بين 0.1 و 24" else "Enter a number between 0.1 and 24")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("session_goal_hours")
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && goalValid,
                onClick = {
                    val type = SessionType.values().firstOrNull { it.name == selected }
                    if (type != null && goalHours != null) {
                        onConfirm(type, Math.round(goalHours * 60).toInt().coerceAtLeast(1))
                    }
                },
                modifier = Modifier.testTag("session_confirm")
            ) { Text(if (isArabic) "ابدأ الجلسة" else "Start session") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun SessionHistoryScreen(
    sessions: List<TimerSessionEntity>,
    isArabic: Boolean,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sessionToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val dateFormat = remember(isArabic) {
        SimpleDateFormat("EEE d MMM yyyy، h:mm a", if (isArabic) Locale("ar") else Locale.getDefault())
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("history_back")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isArabic) "رجوع" else "Back"
                )
            }
            Text(
                text = if (isArabic) "جلساتك السابقة" else "Your past sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (sessions.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isArabic) "لا توجد جلسات بعد" else "No sessions yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("history_empty")
                )
            }
        } else {
            Text(
                text = (if (isArabic) "إجمالي الوقت: " else "Total time: ") +
                    formatElapsed(sessions.sumOf { it.durationMs }) +
                    "  •  " + sessions.size + (if (isArabic) " جلسة" else " sessions"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).testTag("history_list")
            ) {
                items(sessions, key = { it.id }) { session ->
                    val type = SessionType.values().firstOrNull { it.name == session.type }
                    val reached = session.durationMs >= session.goalMinutes * 60_000L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                type?.icon ?: Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = type?.let { if (isArabic) it.titleAr else it.titleEn } ?: session.type,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(session.startedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatElapsed(session.durationMs) + "  /  " +
                                        formatHours(session.goalMinutes, isArabic),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (reached) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            text = if (isArabic) "تحقق الهدف" else "Goal reached",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { sessionToDelete = session.id }) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = if (isArabic) "حذف" else "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    sessionToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(if (isArabic) "حذف الجلسة؟" else "Delete session?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    sessionToDelete = null
                }) { Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

/** Parses "1.5", "1,5" and Arabic-Indic digits ("١٫٥") into hours. */
private fun parseHours(text: String): Double? {
    val normalized = buildString {
        text.trim().forEach { c ->
            append(
                when (c) {
                    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                    ',', '\u066B', '\u066C' -> '.'
                    else -> c
                }
            )
        }
    }
    return normalized.toDoubleOrNull()
}

private fun formatHours(minutes: Int, isArabic: Boolean): String {
    val hours = minutes / 60.0
    val text = if (minutes % 60 == 0) (minutes / 60).toString()
    else "%.1f".format(Locale.US, hours).trimEnd('0').trimEnd('.')
    return text + if (isArabic) " ساعة" else " h"
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
