package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.LearningProgressDao
import com.dsamaster.app.data.entity.LearningProgress
import kotlinx.coroutines.flow.Flow

class LearningProgressRepository(private val dao: LearningProgressDao) {
    fun getAllProgress(): Flow<List<LearningProgress>> = dao.getAllProgress()

    suspend fun getAllProgressOnce(): List<LearningProgress> = dao.getAllProgressOnce()

    fun getProgressForLesson(lessonId: Long): Flow<LearningProgress?> =
        dao.getProgressForLesson(lessonId)

    suspend fun getProgressForLessonOnce(lessonId: Long): LearningProgress? =
        dao.getProgressForLessonOnce(lessonId)

    fun getTotalLessonCountForPath(pathId: Long): Flow<Int> = dao.getTotalLessonCountForPath(pathId)

    fun getIncompleteLessonCountForPath(pathId: Long): Flow<Int> =
        dao.getIncompleteLessonCountForPath(pathId)

    suspend fun insertProgress(progress: LearningProgress): Long = dao.insertProgress(progress)

    suspend fun insertAllProgress(progress: List<LearningProgress>) =
        dao.insertAllProgress(progress)

    suspend fun updateProgress(progress: LearningProgress) = dao.updateProgress(progress)
}