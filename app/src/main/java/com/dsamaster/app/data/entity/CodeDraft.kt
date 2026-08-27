package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "code_drafts",
    indices = [Index(value = ["problemId", "language"], unique = true)]
)
data class CodeDraft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problemId: Long,
    val language: String, // "python" | "java" | "cpp"
    val code: String,
    val lastModified: Long
)