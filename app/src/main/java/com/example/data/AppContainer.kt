package com.example.data

import android.content.Context
import com.example.auth.AuthManager
import com.example.data.local.LifeManagerDatabase
import com.example.sync.FirestoreSyncManager
import com.example.sync.SyncPreferences
import com.example.sync.SyncWorker

object AppContainer {
    @Volatile
    private var _database: LifeManagerDatabase? = null

    @Volatile
    private var _taskRepository: TaskRepository? = null

    @Volatile
    private var _habitRepository: HabitRepository? = null

    @Volatile
    private var _dataRepository: DataRepository? = null

    @Volatile
    private var _surveyRepository: SurveyRepository? = null

    @Volatile
    private var _syncPreferences: SyncPreferences? = null

    @Volatile
    private var _syncManager: FirestoreSyncManager? = null

    @Volatile
    private var _authManager: AuthManager? = null

    val taskRepository: TaskRepository
        get() = _taskRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val habitRepository: HabitRepository
        get() = _habitRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val dataRepository: DataRepository
        get() = _dataRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val surveyRepository: SurveyRepository
        get() = _surveyRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val syncPreferences: SyncPreferences
        get() = _syncPreferences ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val syncManager: FirestoreSyncManager
        get() = _syncManager ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val authManager: AuthManager
        get() = _authManager ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    fun initialize(context: Context, databaseOverride: LifeManagerDatabase? = null) {
        if (_database == null) {
            synchronized(this) {
                if (_database == null) {
                    val db = databaseOverride ?: LifeManagerDatabase.getDatabase(context.applicationContext)
                    _database = db
                    val tombstoneDao = db.syncTombstoneDao()
                    _taskRepository = RoomTaskRepository(db.taskDao(), tombstoneDao)
                    _habitRepository = RoomHabitRepository(db.habitDao(), context.applicationContext, tombstoneDao)
                    _dataRepository = RoomDataRepository(db.folderDao(), db.textCardDao(), tombstoneDao)
                    _surveyRepository = RoomSurveyRepository(db.surveyDao(), tombstoneDao)

                    val prefs = SyncPreferences(context.applicationContext)
                    _syncPreferences = prefs
                    _syncManager = FirestoreSyncManager(context.applicationContext, db, prefs)
                    _authManager = AuthManager(context.applicationContext)

                    // If user is already signed in at startup, schedule periodic background sync
                    if (_authManager?.isUserSignedIn == true) {
                        SyncWorker.schedulePeriodicSync(context.applicationContext)
                        // Trigger startup background sync without waiting
                        try {
                            SyncWorker.triggerImmediateSync(context.applicationContext)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
