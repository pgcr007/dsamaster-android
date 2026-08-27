package com.dsamaster.app.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class ProblemSeed(
    val topicName: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val companyTags: String,
    val constraints: String,
    val examples: List<ExampleSeed>,
    val hints: List<String>,
    val testCases: List<TestCaseSeed>
)

@Serializable
data class ExampleSeed(
    val input: String,
    val output: String,
    val explanation: String = ""
)

@Serializable
data class TestCaseSeed(
    val input: String,
    val output: String
)