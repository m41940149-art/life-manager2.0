package com.example.data

import android.content.Context
import com.example.data.local.LifeManagerDatabase

object AppContainer {
    @Volatile
    private var _database: LifeManagerDatabase? = null

    @Volatile
    private var _taskRepository: TaskRepository? = null

    @Volatile
    private var _habitRepository: HabitRepository? = null

    val taskRepository: TaskRepository
        get() = _taskRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val habitRepository: HabitRepository
        get() = _habitRepository ?: throw IllegalStateException("AppContainer is not initialized yet. Call AppContainer.initialize(context)")

    val dataRepository: DataRepository by lazy { InMemoryDataRepository() }
    val surveyRepository: SurveyRepository by lazy { InMemorySurveyRepository() }

    fun initialize(context: Context, databaseOverride: LifeManagerDatabase? = null) {
        if (_database == null) {
            synchronized(this) {
                if (_database == null) {
                    val db = databaseOverride ?: LifeManagerDatabase.getDatabase(context.applicationContext)
                    _database = db
                    _taskRepository = RoomTaskRepository(db.taskDao())
                    _habitRepository = RoomHabitRepository(db.habitDao(), context.applicationContext)
                }
            }
        }
    }
}
