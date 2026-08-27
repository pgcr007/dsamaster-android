package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.StreakDao
import com.dsamaster.app.data.entity.StreakEntry
import kotlinx.coroutines.flow.Flow

class StreakRepository(private val streakDao: StreakDao) {
    fun getAllStreakEntries(): Flow<List<StreakEntry>> = streakDao.getAllStreakEntries()

    fun getStreakEntryByDate(date: String): Flow<StreakEntry?> =
        streakDao.getStreakEntryByDate(date)

    fun getRecentStreakEntries(days: Int): Flow<List<StreakEntry>> =
        streakDao.getRecentStreakEntries(days)

    suspend fun insertStreakEntry(entry: StreakEntry): Long = streakDao.insertStreakEntry(entry)

    suspend fun updateStreakEntry(entry: StreakEntry) = streakDao.updateStreakEntry(entry)

    suspend fun deleteStreakEntry(entry: StreakEntry) = streakDao.deleteStreakEntry(entry)
}