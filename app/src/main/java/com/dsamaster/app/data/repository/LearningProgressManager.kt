package com.dsamaster.app.data.repository

import com.dsamaster.app.data.entity.LearningProgress
import kotlinx.coroutines.flow.first

/**
 * Orchestrates completing a lesson and unlocking the next one — the one
 * piece of cross-repository Learning Module logic, kept out of the plain
 * repositories the same way SyncManager sits alongside UserProgressRepository
 * rather than inside it. Constructed once in DsaMasterApplication.
 */
class LearningProgressManager(
    private val lessonRepository: LessonRepository,
    private val learningModuleRepository: LearningModuleRepository,
    private val learningProgressRepository: LearningProgressRepository
) {
    suspend fun completeLesson(lessonId: Long) {
        val existing = learningProgressRepository.getProgressForLessonOnce(lessonId)
            ?: LearningProgress(lessonId = lessonId, status = "locked")

        learningProgressRepository.updateProgress(
            existing.copy(
                status = "completed",
                conceptCheckPassed = true,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        unlockNextLesson(lessonId)
    }

    private suspend fun unlockNextLesson(completedLessonId: Long) {
        val currentLesson = lessonRepository.getLessonById(completedLessonId).first() ?: return

        val nextInSameModule = lessonRepository
            .getLessonsByModuleOnce(currentLesson.moduleId)
            .filter { it.orderIndex > currentLesson.orderIndex }
            .minByOrNull { it.orderIndex }

        val nextLesson = nextInSameModule ?: run {
            val currentModule = learningModuleRepository.getModuleById(currentLesson.moduleId).first() ?: return
            val nextModule = learningModuleRepository
                .getModulesByPathOnce(currentModule.pathId)
                .filter { it.orderIndex > currentModule.orderIndex }
                .minByOrNull { it.orderIndex } ?: return
            lessonRepository.getLessonsByModuleOnce(nextModule.id).minByOrNull { it.orderIndex } ?: return
        }

        val nextProgress = learningProgressRepository.getProgressForLessonOnce(nextLesson.id)
        if (nextProgress != null && nextProgress.status == "locked") {
            learningProgressRepository.updateProgress(
                nextProgress.copy(status = "unlocked", updatedAt = System.currentTimeMillis())
            )
        }
    }
}