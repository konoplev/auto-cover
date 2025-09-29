package me.konoplev.autocover.services

import dev.langchain4j.internal.RetryUtils
import dev.langchain4j.service.SystemMessage
import me.konoplev.autocover.config.AgentProperties
import me.konoplev.autocover.config.ModelProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class CoverageImprovementService(
    private val testCoverageProvider: TestCoverageProvider,
    private val coverageImprovementAssistant: CoverageImprovementAssistant,
    private val agentProperties: AgentProperties,
    private val modelProperties: ModelProperties,
) {

    private val logger = LoggerFactory.getLogger(CoverageImprovementService::class.java)

    /**
     * Iteratively improves test coverage by using AI to generate additional tests.
     * Continues until target coverage is reached or no further improvement is achieved.
     */
    fun improveCoverage(currentDirectory: String = File("").absolutePath, targetCoverage: Double? = null) {
        logger.debug("Starting coverage improvement process for directory: {}", currentDirectory)
        if (targetCoverage != null) {
            logger.debug("Target coverage: {}%", targetCoverage)
        }

        var currentCoverage = testCoverageProvider.getCoverage(currentDirectory)
        if (currentCoverage == null) {
            logger.warn("Could not determine initial coverage")
            return
        }

        logger.debug("Initial coverage: {}%", currentCoverage)
        
        // Check if target coverage is already reached
        if (targetCoverage != null && currentCoverage >= targetCoverage) {
            logger.debug("Target coverage {}% already reached (current: {}%)", targetCoverage, currentCoverage)
            return
        }

        var iteration = 1
        val maxIterations = 5 // Prevent infinite loops
        var improved = true

        while (improved && iteration <= maxIterations) {
            logger.debug("Starting iteration {} of coverage improvement", iteration)

            // Use AI to generate additional tests with LangChain4j retry mechanism
            val prompt = buildPrompt(currentDirectory + "/" + agentProperties.coverage.testResultLocation!!)
            val aiResponse = callAiWithRetry(prompt)

            logger.debug("AI response for iteration {}: {}", iteration, aiResponse)

            // Get new coverage after AI-generated tests
            val newCoverage = testCoverageProvider.getCoverage(currentDirectory)
            if (newCoverage == null) {
                logger.warn("Could not determine coverage after iteration {}", iteration)
                break
            }

                val improvement = newCoverage - currentCoverage!!
                
                // Check if target coverage is reached
                if (targetCoverage != null && newCoverage >= targetCoverage) {
                    logger.debug(
                        "Target coverage {}% reached! Coverage improved from {}% to {}% (improvement: {}%)",
                        targetCoverage,
                        currentCoverage,
                        newCoverage,
                        improvement,
                    )
                    improved = false // Stop iterations
                } else if (improvement > 0.1) { // Only continue if improvement is significant (>0.1%)
                    logger.debug(
                        "Coverage improved from {}% to {}% (improvement: {}%)",
                        currentCoverage,
                        newCoverage,
                        improvement,
                    )
                    currentCoverage = newCoverage
                    iteration++
                } else {
                    logger.debug("No significant improvement achieved ({}%), stopping", improvement)
                    improved = false
                }
        }

        if (iteration > maxIterations) {
            logger.debug("Reached maximum iterations ({}), stopping improvement process", maxIterations)
        }

        logger.debug("Coverage improvement process completed. Final coverage: {}%", currentCoverage)
    }

    private fun buildPrompt(coverageReport: String): String =
        """You are a test file generator. You have access to these tools:
                - readFile(filePath) - reads a file
                - writeFile(filePath, content) - writes a file  
                - findFilesByExtension(directoryPath, extension) - finds files
                - compileAndTest(projectDirectory) - compiles and runs tests, shows errors if any
                - checkCompilation(projectDirectory) - checks compilation without running tests

                IMPORTANT: When calling tools, you MUST provide ALL required parameters:
                - readFile("src/main/java/com/example/MyClass.java")
                - writeFile("src/test/java/com/example/MyClassTest.java", "package com.example;...")
                - findFilesByExtension("src/main/java", "java")
                - compileAndTest("src/test/resources/test-projects/spring-sample-app-with-jacoco")
                - checkCompilation("src/test/resources/test-projects/spring-sample-app-with-jacoco")

                CRITICAL: When writing Java code, write it as proper Java code with actual newlines, not escape sequences.
                Do NOT use \\n or other escape sequences in the content. Write the code as it should appear in the file.
                IMPORTANT: Always write COMPLETE Java files with proper closing braces and full method implementations.
                Do not truncate or cut off the content. Ensure all classes, methods, and blocks are properly closed.

                Coverage report:
                ${File(coverageReport).readText()}

                Your task: Create test files for classes with low coverage.

                You MUST use the writeFile() tool to create actual test files. Do not just describe what you would do.
                Always provide complete file paths and content when calling tools.
                Write Java code with proper formatting and actual newlines, not escape sequences.
                
                IMPORTANT: After creating test files, use compileAndTest() to verify they compile and run correctly.
                If there are compilation errors, fix them and try again.

                Start by reading existing test files to understand the project structure, then create new test files.
               """

    /**
     * Calls the AI assistant using LangChain4j's RetryPolicy for rate limiting.
     * Uses the framework's built-in retry mechanism with custom policy configuration.
     */
    private fun callAiWithRetry(prompt: String): String =
        RetryUtils.retryPolicyBuilder()
            .maxAttempts(modelProperties.rateLimit.maxRetries)
            .delayMillis((modelProperties.rateLimit.initialDelaySeconds * 1000).toInt())
            .backoffExp(modelProperties.rateLimit.backoffMultiplier)
            .jitterScale(if (modelProperties.rateLimit.jitter) 0.1 else 0.0)
            .build().withRetry {
                coverageImprovementAssistant.chat(prompt)
            }
}

interface CoverageImprovementAssistant {
    @SystemMessage(
        "You have access to file system tools and compilation tools. When using tools, you MUST provide ALL required parameters. For example: readFile(\"path/to/file\"), writeFile(\"path/to/file\", \"content\"), findFilesByExtension(\"directory\", \"extension\"), compileAndTest(\"project/directory\"), checkCompilation(\"project/directory\"). " +
                "When writing code, write it with proper formatting and actual newlines, not escape sequences like \\n. " +
                "Always write COMPLETE files with proper closing braces and full implementations. " +
                "Use compileAndTest() to verify your code compiles and runs tests.",
    )
    fun chat(message: String): String
}
