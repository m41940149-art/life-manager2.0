package com.example.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.local.TimerSessionDao
import com.example.data.local.TimerSessionEntity
import com.example.ui.celebration.CelebrationCenter
import com.example.ui.celebration.CelebrationKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TimerViewModel(
    private val dao: TimerSessionDao = AppContainer.timerSessionDao
) : ViewModel() {

    val sessions: StateFlow<List<TimerSessionEntity>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSession(
        type: SessionType,
        goalMinutes: Int,
        durationMs: Long,
        startedAt: Long
    ) {
        viewModelScope.launch {
            dao.insertSession(
                TimerSessionEntity(
                    id = UUID.randomUUID().toString(),
                    type = type.name,
                    goalMinutes = goalMinutes,
                    durationMs = durationMs,
                    startedAt = startedAt,
                    endedAt = System.currentTimeMillis()
                )
            )
            celebrateSession(durationMs)
        }
    }

    // Surprise encouragement: crossing an all-time focus total outranks the
    // single-session pat on the back.
    private fun celebrateSession(durationMs: Long) {
        val previousTotalMin = sessions.value.sumOf { it.durationMs } / 60_000L
        val newTotalMin = previousTotalMin + durationMs / 60_000L
        val crossed = totalMinuteMilestones.firstOrNull { it in (previousTotalMin + 1)..newTotalMin }
        if (crossed != null) {
            CelebrationCenter.celebrate(CelebrationKind.TIMER_TOTAL_MILESTONE, crossed.toInt())
        } else {
            val minutes = (durationMs / 60_000L).toInt()
            CelebrationCenter.celebrate(CelebrationKind.TIMER_SESSION, minutes)
        }
    }

    private companion object {
        val totalMinuteMilestones = listOf(60L, 100L, 300L, 600L, 1500L, 3000L, 6000L)
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { dao.deleteSession(id) }
    }
}
