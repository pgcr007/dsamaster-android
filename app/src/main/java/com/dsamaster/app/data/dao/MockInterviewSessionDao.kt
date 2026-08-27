package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.MockInterviewSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MockInterviewSessionDao {
    @Query("SELECT * FROM mock_interview_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<MockInterviewSession>>

    @Query("SELECT * FROM mock_interview_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): MockInterviewSession?

    @Query("SELECT COUNT(*) FROM mock_interview_sessions")
    fun getSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MockInterviewSession): Long

    @Delete
    suspend fun deleteSession(session: MockInterviewSession)
}