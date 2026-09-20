package com.dsamaster.app.data.seed

import android.content.Context
import com.dsamaster.app.data.entity.ConceptCheck
import com.dsamaster.app.data.entity.Lesson
import com.dsamaster.app.data.entity.LearningModule
import com.dsamaster.app.data.entity.LearningPath
import com.dsamaster.app.data.entity.LearningProgress
import com.dsamaster.app.data.repository.ConceptCheckRepository
import com.dsamaster.app.data.repository.LearningModuleRepository
import com.dsamaster.app.data.repository.LearningPathRepository
import com.dsamaster.app.data.repository.LearningProgressRepository
import com.dsamaster.app.data.repository.LessonRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Seeds the Foundations learning path from assets/learning_content.json.
 * Incremental like ProblemSeeder: modules are matched by title, lessons by
 * (moduleId, title) — re-running after learning_content.json grows with a
 * new module or lesson only inserts what's new, so LearningProgress rows
 * already earned are never touched. Must run AFTER topics/problems seed,
 * though it has no hard data dependency on them.
 */
class LearningContentSeeder(
    private val context: Context,
    private val learningPathRepository: LearningPathRepository,
    private val learningModuleRepository: LearningModuleRepository,
    private val lessonRepository: LessonRepository,
    private val conceptCheckRepository: ConceptCheckRepository,
    private val learningProgressRepository: LearningProgressRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val FOUNDATIONS_PATH_NAME = "DSA Foundations"
    }

    suspend fun seedIfNeeded() {
        val pathId = ensureFoundationsPath()

        val jsonString =
            context.assets.open("learning_content.json").bufferedReader().use { it.readText() }
        val moduleSeeds = json.decodeFromString<List<LearningModuleSeed>>(jsonString)

        val existingModules = learningModuleRepository.getModulesByPathOnce(pathId)
        val moduleIdByTitle = existingModules.associateBy({ it.title }, { it.id }).toMutableMap()

        moduleSeeds.forEachIndexed { moduleIndex, moduleSeed ->
            val moduleId = moduleIdByTitle[moduleSeed.moduleTitle] ?: run {
                val newId = learningModuleRepository.insertModule(
                    LearningModule(
                        pathId = pathId,
                        title = moduleSeed.moduleTitle,
                        orderIndex = moduleIndex
                    )
                )
                moduleIdByTitle[moduleSeed.moduleTitle] = newId
                newId
            }

            val existingLessonTitles =
                lessonRepository.getLessonsByModuleOnce(moduleId).map { it.title }.toSet()

            moduleSeed.lessons.forEachIndexed { lessonIndex, lessonSeed ->
                if (lessonSeed.title in existingLessonTitles) return@forEachIndexed

                val lessonId = lessonRepository.insertLesson(
                    Lesson(
                        moduleId = moduleId,
                        title = lessonSeed.title,
                        content = lessonSeed.content,
                        diagramType = lessonSeed.diagramType,
                        codeExample = lessonSeed.codeExample,
                        orderIndex = lessonIndex
                    )
                )

                learningProgressRepository.insertProgress(
                    LearningProgress(lessonId = lessonId, status = "locked")
                )

                if (lessonSeed.conceptChecks.isNotEmpty()) {
                    val checks = lessonSeed.conceptChecks.mapIndexed { checkIndex, checkSeed ->
                        ConceptCheck(
                            lessonId = lessonId,
                            question = checkSeed.question,
                            optionsJson = json.encodeToString(checkSeed.options),
                            correctOptionIndex = checkSeed.correctOptionIndex,
                            explanation = checkSeed.explanation,
                            orderIndex = checkIndex
                        )
                    }
                    conceptCheckRepository.insertChecks(checks)
                }
            }
        }

        unlockFirstLessonIfNeeded(pathId)
    }

    private suspend fun ensureFoundationsPath(): Long {
        val paths = learningPathRepository.getAllPaths().first()
        val existing = paths.firstOrNull { it.name == FOUNDATIONS_PATH_NAME }
        if (existing != null) return existing.id

        return learningPathRepository.insertPath(
            LearningPath(
                name = FOUNDATIONS_PATH_NAME,
                description = "Core DSA concepts, walked in order, before problems or mock interviews.",
                orderIndex = 0
            )
        )
    }

    /**
     * On first-ever seed, every lesson starts "locked" — nothing is
     * reachable. This unlocks exactly the first lesson of the first module,
     * but only if no lesson anywhere is already unlocked/completed (so it
     * never re-locks or re-unlocks anything on a later app update).
     */
    private suspend fun unlockFirstLessonIfNeeded(pathId: Long) {
        val allProgress = learningProgressRepository.getAllProgressOnce()
        if (allProgress.any { it.status != "locked" }) return

        val firstModule = learningModuleRepository.getModulesByPathOnce(pathId)
            .minByOrNull { it.orderIndex } ?: return
        val firstLesson = lessonRepository.getLessonsByModuleOnce(firstModule.id)
            .minByOrNull { it.orderIndex } ?: return
        val progress = allProgress.firstOrNull { it.lessonId == firstLesson.id } ?: return

        learningProgressRepository.updateProgress(
            progress.copy(status = "unlocked", updatedAt = System.currentTimeMillis())
        )
    }
}