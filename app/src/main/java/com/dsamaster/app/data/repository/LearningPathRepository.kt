package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.LearningPathDao
import com.dsamaster.app.data.entity.LearningPath
import kotlinx.coroutines.flow.Flow

class LearningPathRepository(private val dao: LearningPathDao) {
    fun getAllPaths(): Flow<List<LearningPath>> = dao.getAllPaths()

    fun getPathById(pathId: Long): Flow<LearningPath?> = dao.getPathById(pathId)

    suspend fun insertPath(path: LearningPath): Long = dao.insertPath(path)

    suspend fun updatePath(path: LearningPath) = dao.updatePath(path)

    suspend fun deletePath(path: LearningPath) = dao.deletePath(path)
}