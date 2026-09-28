package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        HabitMissedResolutionEntity::class,
        FolderEntity::class,
        TextCardEntity::class,
        SurveyEntity::class,
        SurveyQuestionEntity::class,
        SurveyResponseEntity::class,
        SurveyAnswerEntity::class,
        SyncTombstoneEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class LifeManagerDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun folderDao(): FolderDao
    abstract fun textCardDao(): TextCardDao
    abstract fun surveyDao(): SurveyDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: LifeManagerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `isEveryDay` INTEGER NOT NULL DEFAULT 1,
                        `daysOfWeek` TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7',
                        `notificationEnabled` INTEGER NOT NULL DEFAULT 0,
                        `notificationTime` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `colorHex` INTEGER NOT NULL DEFAULT 4278217823,
                        `categoryIcon` TEXT NOT NULL DEFAULT 'check_circle',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING_SYNC'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `habitId` TEXT NOT NULL,
                        `dateTimestamp` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `dateTimestamp`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_dateTimestamp` ON `habit_completions` (`dateTimestamp`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_missed_resolutions` (
                        `habitId` TEXT NOT NULL,
                        `dateTimestamp` INTEGER NOT NULL,
                        `resolution` TEXT NOT NULL DEFAULT 'BROKEN',
                        `resolvedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `dateTimestamp`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_missed_resolutions_habitId` ON `habit_missed_resolutions` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_missed_resolutions_dateTimestamp` ON `habit_missed_resolutions` (`dateTimestamp`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `folders` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `colorHex` INTEGER NOT NULL DEFAULT 4278217823,
                        `iconName` TEXT NOT NULL DEFAULT 'folder',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `text_cards` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `folderId` TEXT,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isPasswordProtected` INTEGER NOT NULL DEFAULT 0,
                        `passwordHash` TEXT,
                        `tags` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_text_cards_folderId` ON `text_cards` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_text_cards_isFavorite` ON `text_cards` (`isFavorite`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `surveys` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `frequency` TEXT NOT NULL DEFAULT 'DAILY',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `survey_questions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `surveyId` TEXT NOT NULL,
                        `questionOrder` INTEGER NOT NULL,
                        `questionText` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_survey_questions_surveyId` ON `survey_questions` (`surveyId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `survey_responses` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `surveyId` TEXT NOT NULL,
                        `occurrenceNumber` INTEGER NOT NULL,
                        `occurrenceDate` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_survey_responses_surveyId` ON `survey_responses` (`surveyId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `survey_answers` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `responseId` TEXT NOT NULL,
                        `questionId` TEXT NOT NULL,
                        `answerText` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_survey_answers_responseId` ON `survey_answers` (`responseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_survey_answers_questionId` ON `survey_answers` (`questionId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_tombstones` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `extraId` TEXT,
                        `deletedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_tombstones_entityType` ON `sync_tombstones` (`entityType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_tombstones_entityId` ON `sync_tombstones` (`entityId`)")

                // Ensure folders table has syncStatus column if it was migrated from older versions
                val cursor = db.query("PRAGMA table_info(folders)")
                var hasSyncStatus = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1 && cursor.getString(nameIndex) == "syncStatus") {
                        hasSyncStatus = true
                        break
                    }
                }
                cursor.close()
                if (!hasSyncStatus) {
                    db.execSQL("ALTER TABLE `folders` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
                }
            }
        }

        fun getDatabase(context: Context): LifeManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeManagerDatabase::class.java,
                    "life_manager.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
