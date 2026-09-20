package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.LearningPath
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningPathDao {
    @Query("SELECT * FROM learning_paths ORDER BY orderIndex")
    fun getAllPaths(): Flow<List<LearningPath>>

    @Query("SELECT * FROM learning_paths WHERE id = :pathId")
    fun getPathById(pathId: Long): Flow<LearningPath?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPath(path: LearningPath): Long

    @Update
    suspend fun updatePath(path: LearningPath)

    @Delete
    suspend fun deletePath(path: LearningPath)
}