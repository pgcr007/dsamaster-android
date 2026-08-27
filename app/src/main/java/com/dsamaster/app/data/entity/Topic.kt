package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val explanation: String,
    val diagramResId: String? = null,
    val timeComplexity: String,
    val spaceComplexity: String,
    val difficultyLevel: String,
    val companyTags: String // comma-separated, e.g. "Google,Amazon,Microsoft"
)