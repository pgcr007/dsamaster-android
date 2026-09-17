package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.UserProgressDao
import com.dsamaster.app.data.entity.UserProgress
import kotlinx.coroutines.flow.Flow

class UserProgressRepository(private val userProgressDao: UserProgressDao) {

    /**
     * Set by DsaMasterApplication once the SyncManager exists. Fired after
     * every locally-initiated write so the change gets pushed to whichever
     * account (Google or email/password) is currently signed in. Left null
     * for writes that come FROM a sync pull, so we don't immediately echo
     * the same record back to the server.
     */
    var onWrite: ((UserProgress) -> Unit)? = null

    fun getAllProgress(): Flow<List<UserProgress>> = userProgressDao.getAllProgress()

    fun getProgressForProblem(problemId: Long): Flow<UserProgress?> =
        userProgressDao.getProgressForProblem(problemId)

    fun getProgressByStatus(status: String): Flow<List<UserProgress>> =
        userProgressDao.getProgressByStatus(status)

    fun getDueForReview(now: Long): Flow<List<UserProgress>> =
        userProgressDao.getDueForReview(now)

    suspend fun insertProgress(progress: UserProgress): Long {
        val stamped = progress.copy(updatedAt = System.currentTimeMillis())
        val newId = userProgressDao.insertProgress(stamped)
        onWrite?.invoke(if (stamped.id != 0L) stamped else stamped.copy(id = newId))
        return newId
    }

    suspend fun updateProgress(progress: UserProgress) {
        val stamped = progress.copy(updatedAt = System.currentTimeMillis())
        userProgressDao.updateProgress(stamped)
        onWrite?.invoke(stamped)
    }

    suspend fun deleteProgress(progress: UserProgress) = userProgressDao.deleteProgress(progress)

    // --- Cloud sync support (do not restamp updatedAt or trigger onWrite —
    // these are used to apply data that already came FROM the cloud). ---

    suspend fun getAllProgressOnce(): List<UserProgress> = userProgressDao.getAllProgressOnce()

    suspend fun getProgressForProblemOnce(problemId: Long): UserProgress? =
        userProgressDao.getProgressForProblemOnce(problemId)

    suspend fun upsertFromSync(progress: UserProgress): Long =
        userProgressDao.insertProgress(progress)

    suspend fun clearAll() = userProgressDao.clearAll()
}