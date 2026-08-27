package com.dsamaster.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.dsamaster.app.data.entity.PendingReviewRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReviewRequestDao {
    @Query("SELECT * FROM pending_review_requests ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingReviewRequest>>

    @Query("SELECT * FROM pending_review_requests ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<PendingReviewRequest>

    @Insert
    suspend fun insert(request: PendingReviewRequest): Long

    @Delete
    suspend fun delete(request: PendingReviewRequest)

    @Query("UPDATE pending_review_requests SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("DELETE FROM pending_review_requests WHERE id = :id")
    suspend fun deleteById(id: Long)
}