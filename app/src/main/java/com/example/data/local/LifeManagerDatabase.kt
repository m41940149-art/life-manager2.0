package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        HabitMissedResolutionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LifeManagerDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: LifeManagerDatabase? = null

        fun getDatabase(context: Context): LifeManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeManagerDatabase::class.java,
                    "life_manager.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): LifeManagerDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LifeManagerDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
