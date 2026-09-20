package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.LearningProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningProgressDao {
    @Query("SELECT * FROM learning_progress")
    fun getAllProgress(): Flow<List<LearningProgress>>

    @Query("SELECT * FROM learning_progress")
    suspend fun getAllProgressOnce(): List<LearningProgress>

    @Query("SELECT * FROM learning_progress WHERE lessonId = :lessonId")
    fun getProgressForLesson(lessonId: Long): Flow<LearningProgress?>

    @Query("SELECT * FROM learning_progress WHERE lessonId = :lessonId")
    suspend fun getProgressForLessonOnce(lessonId: Long): LearningProgress?

    /** Total lessons that exist under this path (used to detect "not seeded yet"). */
    @Query(
        """
        SELECT COUNT(*) FROM lessons l
        INNER JOIN learning_modules m ON l.moduleId = m.id
        WHERE m.pathId = :pathId
        """
    )
    fun getTotalLessonCountForPath(pathId: Long): Flow<Int>

    /** Lessons under this path not yet marked "completed" — 0 means the gate unlocks. */
    @Query(
        """
        SELECT COUNT(*) FROM lessons l
        INNER JOIN learning_modules m ON l.moduleId = m.id
        LEFT JOIN learning_progress p ON p.lessonId = l.id
        WHERE m.pathId = :pathId AND (p.status IS NULL OR p.status != 'completed')
        """
    )
    fun getIncompleteLessonCountForPath(pathId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LearningProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(progress: List<LearningProgress>)

    @Update
    suspend fun updateProgress(progress: LearningProgress)
}