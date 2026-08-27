package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE topicId = :topicId ORDER BY timestamp DESC")
    fun getNotesForTopic(topicId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE problemId = :problemId ORDER BY timestamp DESC")
    fun getNotesForProblem(problemId: Long): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}