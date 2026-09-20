package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.LessonDao
import com.dsamaster.app.data.dao.LessonOverviewRow
import com.dsamaster.app.data.entity.Lesson
import kotlinx.coroutines.flow.Flow

class LessonRepository(private val dao: LessonDao) {
    fun getAllLessons(): Flow<List<Lesson>> = dao.getAllLessons()

    fun getLessonsByModule(moduleId: Long): Flow<List<Lesson>> = dao.getLessonsByModule(moduleId)

    suspend fun getLessonsByModuleOnce(moduleId: Long): List<Lesson> =
        dao.getLessonsByModuleOnce(moduleId)

    fun getLessonById(lessonId: Long): Flow<Lesson?> = dao.getLessonById(lessonId)

    fun getLearningPathOverview(pathId: Long): Flow<List<LessonOverviewRow>> =
        dao.getLearningPathOverview(pathId)

    suspend fun insertLesson(lesson: Lesson): Long = dao.insertLesson(lesson)

    suspend fun updateLesson(lesson: Lesson) = dao.updateLesson(lesson)

    suspend fun deleteLesson(lesson: Lesson) = dao.deleteLesson(lesson)
}