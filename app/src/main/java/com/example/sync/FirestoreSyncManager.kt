package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    SIGNED_OUT,
    NOT_CONFIGURED,
    ERROR
}

data class SyncInfo(
    val state: SyncState = SyncState.IDLE,
    val lastSyncTime: Long = 0L,
    val isConfigured: Boolean = false,
    val currentUid: String? = null,
    val userEmail: String? = null,
    val installationId: String = "",
    val errorMessage: String? = null
)

class SyncPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("life_manager_sync_prefs", Context.MODE_PRIVATE)

    private val _syncInfo = MutableStateFlow(loadSyncInfo())
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    init {
        // Local device identity metadata only - NOT for cloud ownership
        if (prefs.getString(KEY_INSTALLATION_ID, null) == null) {
            val newId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_INSTALLATION_ID, newId).apply()
        }
        _syncInfo.value = loadSyncInfo()
    }

    val currentUid: String?
        get() = prefs.getString(KEY_CURRENT_UID, null)

    val userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)

    val installationId: String
        get() = prefs.getString(KEY_INSTALLATION_ID, null) ?: "default_device"

    val lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

    fun setCurrentUser(uid: String?, email: String?) {
        prefs.edit()
            .putString(KEY_CURRENT_UID, uid)
            .putString(KEY_USER_EMAIL, email)
            .apply()

        _syncInfo.value = _syncInfo.value.copy(
            currentUid = uid,
            userEmail = email
        )
    }

    fun updateSyncState(
        state: SyncState,
        isConfigured: Boolean,
        uid: String? = currentUid,
        userEmail: String? = this.userEmail,
        errorMessage: String? = null
    ) {
        val now = if (state == SyncState.SYNCED) System.currentTimeMillis() else lastSyncTime
        prefs.edit()
            .putString(KEY_LAST_STATE, state.name)
            .putLong(KEY_LAST_SYNC_TIME, now)
            .apply()

        _syncInfo.value = SyncInfo(
            state = state,
            lastSyncTime = now,
            isConfigured = isConfigured,
            currentUid = uid,
            userEmail = userEmail,
            installationId = installationId,
            errorMessage = errorMessage
        )
    }

    private fun loadSyncInfo(): SyncInfo {
        val stateName = prefs.getString(KEY_LAST_STATE, SyncState.IDLE.name)
        val loaded = try {
            SyncState.valueOf(stateName ?: SyncState.IDLE.name)
        } catch (_: Exception) {
            SyncState.IDLE
        }
        // A sync can't still be running after a process restart
        val state = if (loaded == SyncState.SYNCING) SyncState.IDLE else loaded
        return SyncInfo(
            state = state,
            lastSyncTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L),
            isConfigured = false,
            currentUid = prefs.getString(KEY_CURRENT_UID, null),
            userEmail = prefs.getString(KEY_USER_EMAIL, null),
            installationId = prefs.getString(KEY_INSTALLATION_ID, "") ?: ""
        )
    }

    companion object {
        private const val KEY_INSTALLATION_ID = "sync_installation_id"
        private const val KEY_CURRENT_UID = "sync_current_uid"
        private const val KEY_USER_EMAIL = "sync_user_email"
        private const val KEY_LAST_SYNC_TIME = "sync_last_time"
        private const val KEY_LAST_STATE = "sync_last_state"
    }
}
