package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.MockInterviewSessionDao
import com.dsamaster.app.data.entity.MockInterviewSession
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class MockInterviewSessionRepository(private val dao: MockInterviewSessionDao) {
    fun getAllSessions(): Flow<List<MockInterviewSession>> = dao.getAllSessions()

    fun getSessionCount(): Flow<Int> = dao.getSessionCount()

    suspend fun getSessionById(id: Long): MockInterviewSession? = dao.getSessionById(id)

    suspend fun insertSession(session: MockInterviewSession): Long = dao.insertSession(session)

    suspend fun deleteSession(session: MockInterviewSession) = dao.deleteSession(session)

    companion object {
        fun encodeStringList(items: List<String>): String = Json.encodeToString(items)

        fun decodeStringList(json: String): List<String> =
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
    }
}