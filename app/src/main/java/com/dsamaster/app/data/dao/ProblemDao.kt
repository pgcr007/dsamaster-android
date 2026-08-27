package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.Problem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems ORDER BY id")
    fun getAllProblems(): Flow<List<Problem>>

    @Query("SELECT * FROM problems WHERE id = :problemId")
    fun getProblemById(problemId: Long): Flow<Problem?>

    @Query("SELECT * FROM problems WHERE topicId = :topicId ORDER BY id")
    fun getProblemsByTopic(topicId: Long): Flow<List<Problem>>

    @Query("SELECT * FROM problems WHERE difficulty = :difficulty ORDER BY id")
    fun getProblemsByDifficulty(difficulty: String): Flow<List<Problem>>

    @Query("SELECT * FROM problems WHERE id IN (:ids)")
    fun getProblemsByIds(ids: List<Long>): Flow<List<Problem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: Problem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<Problem>)

    @Update
    suspend fun updateProblem(problem: Problem)

    @Delete
    suspend fun deleteProblem(problem: Problem)
}