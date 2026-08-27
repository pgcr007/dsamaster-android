package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "user_progress",
    foreignKeys = [
        ForeignKey(
            entity = Problem::class,
            parentColumns = ["id"],
            childColumns = ["problemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("problemId", unique = true)]
)
data class UserProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problemId: Long,
    val status: String, // "not_started" | "attempted" | "solved"
    val lastAttemptDate: Long? = null, // epoch millis
    val timesReviewed: Int = 0,
    val nextReviewDate: Long? = null // epoch millis, for spaced repetition
)