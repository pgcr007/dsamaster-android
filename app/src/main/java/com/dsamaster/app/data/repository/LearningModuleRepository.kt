package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.LearningModuleDao
import com.dsamaster.app.data.entity.LearningModule
import kotlinx.coroutines.flow.Flow

class LearningModuleRepository(private val dao: LearningModuleDao) {
    fun getModulesByPath(pathId: Long): Flow<List<LearningModule>> = dao.getModulesByPath(pathId)

    suspend fun getModulesByPathOnce(pathId: Long): List<LearningModule> =
        dao.getModulesByPathOnce(pathId)

    fun getModuleById(moduleId: Long): Flow<LearningModule?> = dao.getModuleById(moduleId)

    suspend fun insertModule(module: LearningModule): Long = dao.insertModule(module)

    suspend fun updateModule(module: LearningModule) = dao.updateModule(module)

    suspend fun deleteModule(module: LearningModule) = dao.deleteModule(module)
}