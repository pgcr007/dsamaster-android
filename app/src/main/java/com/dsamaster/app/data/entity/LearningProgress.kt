package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_progress",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId", unique = true)]
)
data class LearningProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: Long,
    val status: String, // "locked" | "unlocked" | "completed"
    val conceptCheckPassed: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)