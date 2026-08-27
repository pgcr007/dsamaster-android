package com.dsamaster.app.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class TopicSeed(
    val name: String,
    val category: String,
    val explanation: String,
    val diagramType: String,
    val timeComplexity: String,
    val spaceComplexity: String,
    val difficultyLevel: String,
    val companyTags: String
)