package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "concept_checks",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class ConceptCheck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: Long,
    val question: String,
    val optionsJson: String, // JSON array of option strings, e.g. ["A","B","C","D"]
    val correctOptionIndex: Int,
    val explanation: String,
    val orderIndex: Int = 0
)