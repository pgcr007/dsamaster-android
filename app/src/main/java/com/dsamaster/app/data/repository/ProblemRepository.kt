package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.ProblemDao
import com.dsamaster.app.data.entity.Problem
import kotlinx.coroutines.flow.Flow

class ProblemRepository(private val problemDao: ProblemDao) {
    fun getAllProblems(): Flow<List<Problem>> = problemDao.getAllProblems()

    fun getProblemById(problemId: Long): Flow<Problem?> = problemDao.getProblemById(problemId)

    fun getProblemsByTopic(topicId: Long): Flow<List<Problem>> =
        problemDao.getProblemsByTopic(topicId)

    fun getProblemsByDifficulty(difficulty: String): Flow<List<Problem>> =
        problemDao.getProblemsByDifficulty(difficulty)

    fun getProblemsByIds(ids: List<Long>): Flow<List<Problem>> =
        problemDao.getProblemsByIds(ids)

    suspend fun insertProblem(problem: Problem): Long = problemDao.insertProblem(problem)

    suspend fun insertProblems(problems: List<Problem>) = problemDao.insertProblems(problems)

    suspend fun updateProblem(problem: Problem) = problemDao.updateProblem(problem)

    suspend fun deleteProblem(problem: Problem) = problemDao.deleteProblem(problem)
}