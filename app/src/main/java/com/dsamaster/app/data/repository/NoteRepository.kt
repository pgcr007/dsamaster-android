package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.NoteDao
import com.dsamaster.app.data.entity.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesForTopic(topicId: Long): Flow<List<Note>> = noteDao.getNotesForTopic(topicId)

    fun getNotesForProblem(problemId: Long): Flow<List<Note>> =
        noteDao.getNotesForProblem(problemId)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}