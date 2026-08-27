package com.dsamaster.app.data.repository

import com.dsamaster.app.data.remote.CodeExecutionApiClient
import com.dsamaster.app.data.remote.ExecuteResult
import com.dsamaster.app.data.remote.dto.ExecuteRequestDto
import com.dsamaster.app.data.remote.dto.TestCaseDto

class CodeExecutionRepository(
    private val apiClient: CodeExecutionApiClient = CodeExecutionApiClient()
) {
    suspend fun runCode(
        sourceCode: String,
        language: String,
        testCases: List<Pair<String, String>> // (input, expectedOutput)
    ): ExecuteResult {
        val request = ExecuteRequestDto(
            sourceCode = sourceCode,
            language = language,
            testCases = testCases.map { (input, expected) -> TestCaseDto(input, expected) }
        )
        return apiClient.execute(request)
    }
}