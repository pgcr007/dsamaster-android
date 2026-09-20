package com.dsamaster.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6: adds `updatedAt` to user_progress and streak_entries so they can
 * be reconciled with the cloud copy bound to the signed-in account. This is
 * a plain ADD COLUMN, so existing local progress/streaks survive the update
 * instead of being wiped (which fallbackToDestructiveMigration() would do).
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_progress ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE streak_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v6 -> v7: adds the Learning Module tables (Phase 14) — learning_paths,
 * learning_modules, lessons, concept_checks, learning_progress. These are
 * brand-new tables, so this migration only creates them; every existing
 * table and row (topics, problems, progress, streaks, notes, etc.) is
 * untouched.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `learning_paths` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `orderIndex` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `learning_modules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `pathId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `orderIndex` INTEGER NOT NULL,
                FOREIGN KEY(`pathId`) REFERENCES `learning_paths`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_learning_modules_pathId` ON `learning_modules` (`pathId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lessons` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `moduleId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `diagramType` TEXT,
                `orderIndex` INTEGER NOT NULL,
                FOREIGN KEY(`moduleId`) REFERENCES `learning_modules`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_lessons_moduleId` ON `lessons` (`moduleId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `concept_checks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `lessonId` INTEGER NOT NULL,
                `question` TEXT NOT NULL,
                `optionsJson` TEXT NOT NULL,
                `correctOptionIndex` INTEGER NOT NULL,
                `explanation` TEXT NOT NULL,
                `orderIndex` INTEGER NOT NULL,
                FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_concept_checks_lessonId` ON `concept_checks` (`lessonId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `learning_progress` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `lessonId` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `conceptCheckPassed` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_learning_progress_lessonId` ON `learning_progress` (`lessonId`)"
        )
    }
}

/**
 * v7 -> v8: adds `codeExample` to lessons — a plain ADD COLUMN, so existing
 * lesson rows and progress are untouched. Backs the DSA-cheat-sheet content
 * expansion (theory + Python examples, no concept-check quiz).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lessons ADD COLUMN codeExample TEXT")
    }
}