package com.dsamaster.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dsamaster.app.data.dao.CodeDraftDao
import com.dsamaster.app.data.dao.MockInterviewSessionDao
import com.dsamaster.app.data.dao.NoteDao
import com.dsamaster.app.data.dao.PendingReviewRequestDao
import com.dsamaster.app.data.dao.ProblemDao
import com.dsamaster.app.data.dao.StreakDao
import com.dsamaster.app.data.dao.TopicDao
import com.dsamaster.app.data.dao.UserProgressDao
import com.dsamaster.app.data.entity.CodeDraft
import com.dsamaster.app.data.entity.MockInterviewSession
import com.dsamaster.app.data.entity.Note
import com.dsamaster.app.data.entity.PendingReviewRequest
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.entity.UserProgress

@Database(
    entities = [
        Topic::class,
        Problem::class,
        UserProgress::class,
        StreakEntry::class,
        Note::class,
        CodeDraft::class,
        MockInterviewSession::class,
        PendingReviewRequest::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DSAMasterDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun problemDao(): ProblemDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun streakDao(): StreakDao
    abstract fun noteDao(): NoteDao
    abstract fun codeDraftDao(): CodeDraftDao
    abstract fun mockInterviewSessionDao(): MockInterviewSessionDao
    abstract fun pendingReviewRequestDao(): PendingReviewRequestDao

    companion object {
        @Volatile
        private var INSTANCE: DSAMasterDatabase? = null

        fun getDatabase(context: android.content.Context): DSAMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DSAMasterDatabase::class.java,
                    "dsamaster_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}