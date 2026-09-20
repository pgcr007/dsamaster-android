package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_modules",
    foreignKeys = [
        ForeignKey(
            entity = LearningPath::class,
            parentColumns = ["id"],
            childColumns = ["pathId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pathId")]
)
data class LearningModule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pathId: Long,
    val title: String,
    val orderIndex: Int
)