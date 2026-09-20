package com.dsamaster.app.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class ConceptCheckSeed(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@Serializable
data class LessonSeed(
    val title: String,
    val content: String,
    val diagramType: String? = null,
    val codeExample: String? = null,
    val conceptChecks: List<ConceptCheckSeed> = emptyList()
)

@Serializable
data class LearningModuleSeed(
    val moduleTitle: String,
    val lessons: List<LessonSeed>
)