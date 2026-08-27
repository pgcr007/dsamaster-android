package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestCaseDto(
    val input: String,
    val expectedOutput: String
)

@Serializable
data class ExecuteRequestDto(
    val sourceCode: String,
    val language: String, // "python" | "java" | "cpp"
    val testCases: List<TestCaseDto>
)

@Serializable
data class TestCaseResultDto(
    val input: String,
    val expectedOutput: String,
    val actualOutput: String,
    val passed: Boolean,
    val status: String,
    val stderr: String? = null,
    val compileOutput: String? = null,
    val timeSeconds: Double? = null,
    val memoryKb: Double? = null
)

@Serializable
data class ExecuteResponseDto(
    val allPassed: Boolean,
    val results: List<TestCaseResultDto>
)

@Serializable
data class ErrorResponseDto(
    val error: String,
    val detail: String? = null
)