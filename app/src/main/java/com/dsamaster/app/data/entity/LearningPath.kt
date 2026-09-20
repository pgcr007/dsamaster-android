package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_paths")
data class LearningPath(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val orderIndex: Int
)