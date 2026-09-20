package com.dsamaster.app.data.seed

import android.content.Context
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class TopicSeeder(
    private val context: Context,
    private val topicRepository: TopicRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Seeds topics from assets/topics.json into Room. Incremental, matched
     * by `name` — same pattern as ProblemSeeder/LearningContentSeeder — so
     * adding new entries to topics.json later just inserts what's new on
     * next launch instead of requiring a reinstall to see them.
     */
    suspend fun seedIfNeeded() {
        val jsonString = context.assets.open("topics.json").bufferedReader().use { it.readText() }
        val seeds = json.decodeFromString<List<TopicSeed>>(jsonString)

        val existingNames = topicRepository.getAllTopics().first().map { it.name }.toSet()

        val newTopics = seeds
            .filter { it.name !in existingNames }
            .map { seed ->
                Topic(
                    name = seed.name,
                    category = seed.category,
                    explanation = seed.explanation,
                    diagramResId = seed.diagramType,
                    timeComplexity = seed.timeComplexity,
                    spaceComplexity = seed.spaceComplexity,
                    difficultyLevel = seed.difficultyLevel,
                    companyTags = seed.companyTags
                )
            }

        if (newTopics.isNotEmpty()) {
            topicRepository.insertTopics(newTopics)
        }
    }
}