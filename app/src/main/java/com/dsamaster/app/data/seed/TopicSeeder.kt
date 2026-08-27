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
     * Seeds topics from assets/topics.json into Room, but only if the table is
     * currently empty — safe to call on every app launch without duplicating data.
     */
    suspend fun seedIfNeeded() {
        val existingTopics = topicRepository.getAllTopics().first()
        if (existingTopics.isNotEmpty()) return

        val jsonString = context.assets.open("topics.json").bufferedReader().use { it.readText() }
        val seeds = json.decodeFromString<List<TopicSeed>>(jsonString)

        val topics = seeds.map { seed ->
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

        topicRepository.insertTopics(topics)
    }
}