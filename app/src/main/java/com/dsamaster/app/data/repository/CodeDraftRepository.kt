package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.CodeDraftDao
import com.dsamaster.app.data.entity.CodeDraft
import kotlinx.coroutines.flow.Flow

class CodeDraftRepository(private val codeDraftDao: CodeDraftDao) {
    suspend fun getDraftsForProblem(problemId: Long): List<CodeDraft> =
        codeDraftDao.getDraftsForProblem(problemId)

    fun getAllDrafts(): Flow<List<CodeDraft>> = codeDraftDao.getAllDrafts()

    suspend fun saveDraft(problemId: Long, language: String, code: String) {
        codeDraftDao.upsertDraft(
            CodeDraft(
                problemId = problemId,
                language = language,
                code = code,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun upsertDraft(draft: CodeDraft) = codeDraftDao.upsertDraft(draft)
}