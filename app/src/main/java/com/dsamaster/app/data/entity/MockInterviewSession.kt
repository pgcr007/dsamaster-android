package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "mock_interview_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Problem::class,
            parentColumns = ["id"],
            childColumns = ["problemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("problemId")]
)
data class MockInterviewSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problemId: Long,
    val problemTitle: String, // denormalized so the sessions list doesn't need a join
    val difficulty: String,
    val language: String,
    val approach: String,
    val clarifyingQuestion: String,
    val clarifyingAnswer: String,
    val code: String,
    val followUpQuestion: String,
    val followUpAnswer: String,
    val wentWellJson: String, // JSON-encoded List<String>
    val workOnJson: String, // JSON-encoded List<String>
    val overallNotes: String,
    val durationSeconds: Int,
    val completedAt: Long // epoch millis
)