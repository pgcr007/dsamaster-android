package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.LearningModule
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningModuleDao {
    @Query("SELECT * FROM learning_modules WHERE pathId = :pathId ORDER BY orderIndex")
    fun getModulesByPath(pathId: Long): Flow<List<LearningModule>>

    @Query("SELECT * FROM learning_modules WHERE pathId = :pathId ORDER BY orderIndex")
    suspend fun getModulesByPathOnce(pathId: Long): List<LearningModule>

    @Query("SELECT * FROM learning_modules WHERE id = :moduleId")
    fun getModuleById(moduleId: Long): Flow<LearningModule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: LearningModule): Long

    @Update
    suspend fun updateModule(module: LearningModule)

    @Delete
    suspend fun deleteModule(module: LearningModule)
}