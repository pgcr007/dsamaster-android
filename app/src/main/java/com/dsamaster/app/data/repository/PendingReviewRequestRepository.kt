package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.PendingReviewRequestDao
import com.dsamaster.app.data.entity.PendingReviewRequest
import kotlinx.coroutines.flow.Flow

class PendingReviewRequestRepository(private val dao: PendingReviewRequestDao) {
    fun getAll(): Flow<List<PendingReviewRequest>> = dao.getAll()

    suspend fun getAllOnce(): List<PendingReviewRequest> = dao.getAllOnce()

    suspend fun enqueue(request: PendingReviewRequest): Long = dao.insert(request)

    suspend fun remove(request: PendingReviewRequest) = dao.delete(request)

    suspend fun incrementRetryCount(id: Long) = dao.incrementRetryCount(id)

    suspend fun removeById(id: Long) = dao.deleteById(id)
}