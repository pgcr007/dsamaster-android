package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.StreakDao
import com.dsamaster.app.data.entity.StreakEntry
import kotlinx.coroutines.flow.Flow

class StreakRepository(private val streakDao: StreakDao) {

    /**
     * Set by DsaMasterApplication once the SyncManager exists. Fired after
     * every locally-initiated write so the change gets pushed to whichever
     * account (Google or email/password) is currently signed in. Left null
     * for writes that come FROM a sync pull, so we don't immediately echo
     * the same record back to the server.
     */
    var onWrite: ((StreakEntry) -> Unit)? = null

    fun getAllStreakEntries(): Flow<List<StreakEntry>> = streakDao.getAllStreakEntries()

    fun getStreakEntryByDate(date: String): Flow<StreakEntry?> =
        streakDao.getStreakEntryByDate(date)

    fun getRecentStreakEntries(days: Int): Flow<List<StreakEntry>> =
        streakDao.getRecentStreakEntries(days)

    suspend fun insertStreakEntry(entry: StreakEntry): Long {
        val stamped = entry.copy(updatedAt = System.currentTimeMillis())
        val newId = streakDao.insertStreakEntry(stamped)
        onWrite?.invoke(if (stamped.id != 0L) stamped else stamped.copy(id = newId))
        return newId
    }

    suspend fun updateStreakEntry(entry: StreakEntry) {
        val stamped = entry.copy(updatedAt = System.currentTimeMillis())
        streakDao.updateStreakEntry(stamped)
        onWrite?.invoke(stamped)
    }

    suspend fun deleteStreakEntry(entry: StreakEntry) = streakDao.deleteStreakEntry(entry)

    // --- Cloud sync support (do not restamp updatedAt or trigger onWrite —
    // these are used to apply data that already came FROM the cloud). ---

    suspend fun getAllStreakEntriesOnce(): List<StreakEntry> = streakDao.getAllStreakEntriesOnce()

    suspend fun getStreakEntryByDateOnce(date: String): StreakEntry? =
        streakDao.getStreakEntryByDateOnce(date)

    suspend fun upsertFromSync(entry: StreakEntry): Long = streakDao.insertStreakEntry(entry)

    suspend fun clearAll() = streakDao.clearAll()
}