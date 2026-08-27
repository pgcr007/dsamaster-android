package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "streak_entries",
    indices = [Index("date", unique = true)]
)
data class StreakEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd" format, unique per day
    val minutesActive: Int = 0,
    val problemsSolved: Int = 0,
    val streakFreezeUsed: Boolean = false
)