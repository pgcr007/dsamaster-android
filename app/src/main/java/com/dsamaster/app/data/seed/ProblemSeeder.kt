package com.dsamaster.app.data.seed

import android.content.Context
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProblemSeeder(
    private val context: Context,
    private val topicRepository: TopicRepository,
    private val problemRepository: ProblemRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Seeds problems from assets/problems.json into Room. Incremental: only
     * inserts problems (matched by topicId + title) that don't already exist,
     * so re-running after problems.json grows with a new batch only adds the
     * new ones — existing progress/streak data is never touched. Must run
     * AFTER topics are seeded, since problems are linked to topics by name.
     */
    suspend fun seedIfNeeded() {
        val topics = topicRepository.getAllTopics().first()
        val topicIdByName = topics.associateBy({ it.name }, { it.id })

        val existingProblems = problemRepository.getAllProblems().first()
        val existingKeys = existingProblems.map { it.topicId to it.title }.toSet()

        val jsonString = context.assets.open("problems.json").bufferedReader().use { it.readText() }
        val seeds = json.decodeFromString<List<ProblemSeed>>(jsonString)

        val newProblems = seeds.mapNotNull { seed ->
            val topicId = topicIdByName[seed.topicName] ?: return@mapNotNull null
            if ((topicId to seed.title) in existingKeys) return@mapNotNull null
            Problem(
                topicId = topicId,
                title = seed.title,
                description = seed.description,
                difficulty = seed.difficulty,
                companyTags = seed.companyTags,
                constraints = seed.constraints,
                examplesJson = json.encodeToString(seed.examples),
                testCasesJson = json.encodeToString(seed.testCases),
                hints = seed.hints.joinToString("|")
            )
        }

        if (newProblems.isNotEmpty()) {
            problemRepository.insertProblems(newProblems)
        }
    }
}