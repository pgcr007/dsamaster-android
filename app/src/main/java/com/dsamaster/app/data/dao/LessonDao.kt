package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.Lesson
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY moduleId, orderIndex")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE moduleId = :moduleId ORDER BY orderIndex")
    fun getLessonsByModule(moduleId: Long): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE moduleId = :moduleId ORDER BY orderIndex")
    suspend fun getLessonsByModuleOnce(moduleId: Long): List<Lesson>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    fun getLessonById(lessonId: Long): Flow<Lesson?>

    /**
     * One row per lesson in the given path, with its module title/order and
     * current progress status already joined in (status defaults to
     * "locked" if no progress row exists yet). Powers LearningPathScreen
     * without the UI needing to combine three separate Flows itself.
     */
    @Query(
        """
        SELECT l.id as lessonId, l.moduleId as moduleId, l.title as lessonTitle, l.orderIndex as lessonOrder,
               m.title as moduleTitle, m.orderIndex as moduleOrder,
               COALESCE(p.status, 'locked') as status
        FROM lessons l
        INNER JOIN learning_modules m ON l.moduleId = m.id
        LEFT JOIN learning_progress p ON p.lessonId = l.id
        WHERE m.pathId = :pathId
        ORDER BY m.orderIndex, l.orderIndex
        """
    )
    fun getLearningPathOverview(pathId: Long): Flow<List<LessonOverviewRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Delete
    suspend fun deleteLesson(lesson: Lesson)
}