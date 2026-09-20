package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.ConceptCheckDao
import com.dsamaster.app.data.entity.ConceptCheck
import kotlinx.coroutines.flow.Flow

class ConceptCheckRepository(private val dao: ConceptCheckDao) {
    fun getChecksForLesson(lessonId: Long): Flow<List<ConceptCheck>> =
        dao.getChecksForLesson(lessonId)

    suspend fun insertCheck(check: ConceptCheck): Long = dao.insertCheck(check)

    suspend fun insertChecks(checks: List<ConceptCheck>) = dao.insertChecks(checks)

    suspend fun updateCheck(check: ConceptCheck) = dao.updateCheck(check)

    suspend fun deleteCheck(check: ConceptCheck) = dao.deleteCheck(check)
}