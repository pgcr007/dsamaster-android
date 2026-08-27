package com.dsamaster.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsamaster.app.data.entity.CodeDraft
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeDraftDao {
    @Query("SELECT * FROM code_drafts WHERE problemId = :problemId")
    suspend fun getDraftsForProblem(problemId: Long): List<CodeDraft>

    @Query("SELECT * FROM code_drafts")
    fun getAllDrafts(): Flow<List<CodeDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: CodeDraft): Long
}