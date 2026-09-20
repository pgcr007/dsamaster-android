package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.ConceptCheck
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptCheckDao {
    @Query("SELECT * FROM concept_checks WHERE lessonId = :lessonId ORDER BY orderIndex")
    fun getChecksForLesson(lessonId: Long): Flow<List<ConceptCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: ConceptCheck): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(checks: List<ConceptCheck>)

    @Update
    suspend fun updateCheck(check: ConceptCheck)

    @Delete
    suspend fun deleteCheck(check: ConceptCheck)
}