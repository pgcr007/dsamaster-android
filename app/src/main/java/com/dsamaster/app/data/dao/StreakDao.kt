package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.StreakEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_entries ORDER BY date DESC")
    fun getAllStreakEntries(): Flow<List<StreakEntry>>

    @Query("SELECT * FROM streak_entries WHERE date = :date")
    fun getStreakEntryByDate(date: String): Flow<StreakEntry?>

    @Query("SELECT * FROM streak_entries ORDER BY date DESC LIMIT :days")
    fun getRecentStreakEntries(days: Int): Flow<List<StreakEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakEntry(entry: StreakEntry): Long

    @Update
    suspend fun updateStreakEntry(entry: StreakEntry)

    @Delete
    suspend fun deleteStreakEntry(entry: StreakEntry)
}