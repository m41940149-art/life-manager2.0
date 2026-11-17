package com.example.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.local.TimerSessionDao
import com.example.data.local.TimerSessionEntity
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
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { dao.deleteSession(id) }
    }
}
