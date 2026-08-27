package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.UserProgressDao
import com.dsamaster.app.data.entity.UserProgress
import kotlinx.coroutines.flow.Flow

class UserProgressRepository(private val userProgressDao: UserProgressDao) {
    fun getAllProgress(): Flow<List<UserProgress>> = userProgressDao.getAllProgress()

    fun getProgressForProblem(problemId: Long): Flow<UserProgress?> =
        userProgressDao.getProgressForProblem(problemId)

    fun getProgressByStatus(status: String): Flow<List<UserProgress>> =
        userProgressDao.getProgressByStatus(status)

    fun getDueForReview(now: Long): Flow<List<UserProgress>> =
        userProgressDao.getDueForReview(now)

    suspend fun insertProgress(progress: UserProgress): Long =
        userProgressDao.insertProgress(progress)

    suspend fun updateProgress(progress: UserProgress) = userProgressDao.updateProgress(progress)

    suspend fun deleteProgress(progress: UserProgress) = userProgressDao.deleteProgress(progress)
}