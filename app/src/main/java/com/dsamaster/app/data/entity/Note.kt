package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long? = null,
    val problemId: Long? = null,
    val userNote: String,
    val timestamp: Long // epoch millis
)