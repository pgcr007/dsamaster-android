package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE problemId = :problemId")
    fun getProgressForProblem(problemId: Long): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE status = :status")
    fun getProgressByStatus(status: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE nextReviewDate <= :now AND status = 'solved'")
    fun getDueForReview(now: Long): Flow<List<UserProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress): Long

    @Update
    suspend fun updateProgress(progress: UserProgress)

    @Delete
    suspend fun deleteProgress(progress: UserProgress)
}