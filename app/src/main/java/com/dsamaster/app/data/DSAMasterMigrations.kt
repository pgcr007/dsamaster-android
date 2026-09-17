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